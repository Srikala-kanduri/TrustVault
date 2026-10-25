package com.trustvault;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import java.sql.Timestamp;
import java.time.Instant;
import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
class AuditIntegrityIntegrationTest {

    @Autowired
    private AuditService auditService;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AuditChainHeadRepository headRepository;

    @Autowired
    private AuditChainBootstrapService bootstrapService;

    @Autowired
    private AuditIntegrityHasher hasher;

    @Autowired
    private EntityManager entityManager;

    @Test
    void changingAnAuditFieldIsDetectedAndCanBeRestored() {
        String actor = "audit-integrity-" + UUID.randomUUID() + "@example.com";
        String details = "Integrity check event";

        auditService.recordSuccess(
                actor, "DOCUMENT_DOWNLOAD", 91234L, details);
        AuditLog log = auditLogRepository
                .findByActorEmailOrderByOccurredAtDesc(actor)
                .get(0);

        try {
            jdbcTemplate.update(
                    "update audit_logs set details = ? where id = ?",
                    "altered details",
                    log.getId());
            assertFalse(auditService.verifyIntegrity().valid());
        } finally {
            jdbcTemplate.update(
                    "update audit_logs set details = ? where id = ?",
                    details,
                    log.getId());
        }
        assertTrue(auditService.verifyIntegrity().valid());

        AuditChainHead head = headRepository
                .findById(AuditChainHead.SINGLETON_ID)
                .orElseThrow();
        try {
            jdbcTemplate.update(
                    "update audit_chain_head set entry_count = ? where id = ?",
                    head.getEntryCount() - 1,
                    AuditChainHead.SINGLETON_ID);
            assertFalse(auditService.verifyIntegrity().valid());
        } finally {
            jdbcTemplate.update(
                    "update audit_chain_head set entry_count = ? where id = ?",
                    head.getEntryCount(),
                    AuditChainHead.SINGLETON_ID);
        }
        assertTrue(auditService.verifyIntegrity().valid());
    }

    @Test
    @Transactional
    void deletingAnAuditEntryIsDetected() {
        String actor = "audit-delete-" + UUID.randomUUID() + "@example.com";
        auditService.recordSuccess(actor, "DOCUMENT_READ", 91235L, "delete check");
        Long id = auditLogRepository
                .findByActorEmailOrderByOccurredAtDesc(actor)
                .get(0)
                .getId();

        jdbcTemplate.update("delete from audit_logs where id = ?", id);
        entityManager.clear();

        assertFalse(auditService.verifyIntegrity().valid());
    }

    @Test
    @Transactional
    void truncatingAuditHistoryIsDetected() {
        String actor = "audit-truncate-" + UUID.randomUUID() + "@example.com";
        auditService.recordSuccess(
                actor, "DOCUMENT_READ", 91239L, "truncate check");
        jdbcTemplate.update("delete from audit_logs");
        entityManager.clear();

        assertFalse(auditService.verifyIntegrity().valid());
    }

    @Test
    @Transactional
    void reorderingAuditEntriesIsDetected() {
        String actor = "audit-order-" + UUID.randomUUID() + "@example.com";
        auditService.recordSuccess(actor, "DOCUMENT_READ", 91236L, "first");
        auditService.recordSuccess(actor, "DOCUMENT_READ", 91237L, "second");

        var entries = auditLogRepository.findAllByOrderByIdAsc().stream()
                .filter(log -> actor.equals(log.getActorEmail()))
                .toList();
        long firstId = entries.get(0).getId();
        long secondId = entries.get(1).getId();
        long temporaryId = Long.MIN_VALUE;

        jdbcTemplate.update("update audit_logs set id = ? where id = ?",
                temporaryId, firstId);
        jdbcTemplate.update("update audit_logs set id = ? where id = ?",
                firstId, secondId);
        jdbcTemplate.update("update audit_logs set id = ? where id = ?",
                secondId, temporaryId);
        entityManager.clear();

        assertFalse(auditService.verifyIntegrity().valid());
    }

    @Test
    void bootstrapRejectsModifiedSealedHistoryWithoutResealingIt() {
        String actor = "audit-bootstrap-" + UUID.randomUUID() + "@example.com";
        String originalDetails = "sealed bootstrap event";
        auditService.recordSuccess(
                actor, "DOCUMENT_READ", 91238L, originalDetails);
        AuditLog log = auditLogRepository
                .findByActorEmailOrderByOccurredAtDesc(actor)
                .get(0);
        String originalHash = log.getEntryHash();

        jdbcTemplate.update(
                "update audit_logs set details = ? where id = ?",
                "tampered bootstrap event",
                log.getId());
        try {
            org.junit.jupiter.api.Assertions.assertThrows(
                    IllegalStateException.class,
                    () -> bootstrapService.initializeAndVerify());

            assertEquals(
                    "tampered bootstrap event",
                    jdbcTemplate.queryForObject(
                            "select details from audit_logs where id = ?",
                            String.class,
                            log.getId()));
            assertEquals(
                    originalHash,
                    jdbcTemplate.queryForObject(
                            "select entry_hash from audit_logs where id = ?",
                            String.class,
                            log.getId()));
        } finally {
            jdbcTemplate.update(
                    "update audit_logs set details = ? where id = ?",
                    originalDetails,
                    log.getId());
        }
        assertTrue(auditService.verifyIntegrity().valid());
    }

    @Test
    @Transactional
    void bootstrapRefusesUnsealedLegacyRowsInsteadOfAdoptingThem() {
        jdbcTemplate.update("delete from audit_logs");
        String genesis = AuditIntegrityHasher.GENESIS_HASH;
        jdbcTemplate.update(
                "update audit_chain_head set initialized = false, entry_count = 0, "
                        + "last_entry_hash = ?, head_hash = ? where id = ?",
                genesis,
                hasher.headHash(genesis, 0),
                AuditChainHead.SINGLETON_ID);

        jdbcTemplate.update(
                "insert into audit_logs "
                        + "(actor_email, action, document_id, details, outcome, "
                        + "occurred_at, previous_hash, entry_hash) "
                        + "values (?, ?, ?, ?, ?, ?, null, null)",
                "legacy@example.com",
                "DOCUMENT_READ",
                91240L,
                "legacy unsealed row",
                "SUCCESS",
                Timestamp.from(Instant.now()));
        entityManager.clear();

        assertThrows(
                IllegalStateException.class,
                () -> bootstrapService.initializeAndVerify());
        assertNull(jdbcTemplate.queryForObject(
                "select entry_hash from audit_logs where actor_email = ?",
                String.class,
                "legacy@example.com"));
        assertNull(jdbcTemplate.queryForObject(
                "select previous_hash from audit_logs where actor_email = ?",
                String.class,
                "legacy@example.com"));
    }
}
