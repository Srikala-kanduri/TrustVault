
package com.trustvault;

import java.util.Locale;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final AuditChainHeadRepository headRepository;
    private final AuditIntegrityHasher hasher;

    public AuditService(
            AuditLogRepository auditLogRepository,
            AuditChainHeadRepository headRepository,
            AuditIntegrityHasher hasher) {
        this.auditLogRepository = auditLogRepository;
        this.headRepository = headRepository;
        this.hasher = hasher;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(
            String actorEmail,
            String action,
            Long documentId,
            String details,
            String outcome) {

        saveAuditLog(actorEmail, action, documentId, details, outcome);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordInCurrentTransaction(
            String actorEmail,
            String action,
            Long documentId,
            String details,
            String outcome) {
        saveAuditLog(actorEmail, action, documentId, details, outcome);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(
            String actorEmail,
            String action,
            Long documentId,
            String details) {

        saveAuditLog(actorEmail, action, documentId, details, "SUCCESS");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordDenied(
            String actorEmail,
            String action,
            Long documentId,
            String details) {

        saveAuditLog(actorEmail, action, documentId, details, "DENIED");
    }

    private void saveAuditLog(
            String actorEmail,
            String action,
            Long documentId,
            String details,
            String outcome) {

        AuditChainHead head = headRepository
                .lockForWrite(AuditChainHead.SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException(
                        "Audit integrity head is missing"));

        if (!head.isInitialized()) {
            throw new IllegalStateException(
                    "Audit integrity chain has not been initialized");
        }

        if (!hasher.matches(
                hasher.headHash(
                        head.getLastEntryHash(), head.getEntryCount()),
                head.getHeadHash())) {
            throw new IllegalStateException(
                    "Audit integrity head verification failed");
        }

        String actor = normalizeActor(actorEmail);
        Instant occurredAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
        String previousHash = head.getLastEntryHash();
        String entryHash = hasher.entryHash(
                previousHash,
                actor,
                action,
                documentId,
                details,
                outcome,
                occurredAt);

        auditLogRepository.saveAndFlush(new AuditLog(
                actor,
                action,
                documentId,
                details,
                outcome,
                occurredAt,
                previousHash,
                entryHash));

        long count = head.getEntryCount() + 1;
        head.advance(entryHash, count, hasher.headHash(entryHash, count));
        headRepository.saveAndFlush(head);
    }

    // verifyIntegrity takes a PESSIMISTIC_READ lock on the chain head.
    // Keep this as a read-write transaction because PostgreSQL rejects
    // row-locking SELECTs in a read-only transaction.
    @Transactional
    public IntegrityResult verifyIntegrity() {
        AuditChainHead head = headRepository
                .lockForRead(AuditChainHead.SINGLETON_ID)
                .orElse(null);
        if (head == null || !head.isInitialized()) {
            return new IntegrityResult(false, null,
                    "Audit integrity head is missing");
        }

        String previousHash = AuditIntegrityHasher.GENESIS_HASH;
        long count = 0;
        for (AuditLog log : auditLogRepository.findAllByOrderByIdAsc()) {
            String calculated = hasher.entryHash(
                    previousHash,
                    log.getActorEmail(),
                    log.getAction(),
                    log.getDocumentId(),
                    log.getDetails(),
                    log.getOutcome(),
                    log.getOccurredAt());

            if (!hasher.matches(previousHash, log.getPreviousHash())
                    || !hasher.matches(calculated, log.getEntryHash())) {
                return new IntegrityResult(false, log.getId(),
                        "Audit chain validation failed");
            }
            previousHash = calculated;
            count++;
        }

        boolean valid = head.getEntryCount() == count
                && hasher.matches(previousHash, head.getLastEntryHash())
                && hasher.matches(
                        hasher.headHash(previousHash, count), head.getHeadHash());
        return valid
                ? new IntegrityResult(true, null, "Audit history is intact")
                : new IntegrityResult(false, null,
                        "Audit chain head does not match the recorded history");
    }

    public record IntegrityResult(
            boolean valid, Long invalidRecordId, String message) {
    }

    private String normalizeActor(String actorEmail) {
        if (actorEmail == null || actorEmail.isBlank()) {
            return "unknown";
        }

        return actorEmail.trim().toLowerCase(Locale.ROOT);
    }
}
