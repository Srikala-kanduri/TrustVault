package com.trustvault;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditChainBootstrapService {

    private final AuditLogRepository auditLogRepository;
    private final AuditChainHeadRepository headRepository;
    private final AuditIntegrityHasher hasher;

    public AuditChainBootstrapService(
            AuditLogRepository auditLogRepository,
            AuditChainHeadRepository headRepository,
            AuditIntegrityHasher hasher) {
        this.auditLogRepository = auditLogRepository;
        this.headRepository = headRepository;
        this.hasher = hasher;
    }

    @Transactional
    public void initializeAndVerify() {
        boolean headAlreadyExisted =
                headRepository.existsById(AuditChainHead.SINGLETON_ID);
        if (!headAlreadyExisted) {
            headRepository.saveAndFlush(new AuditChainHead(
                    AuditIntegrityHasher.GENESIS_HASH,
                    hasher.headHash(AuditIntegrityHasher.GENESIS_HASH, 0)));
        }

        AuditChainHead head = headRepository
                .lockForWrite(AuditChainHead.SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException(
                        "Audit integrity head is missing"));

        List<AuditLog> logs = auditLogRepository.findAllByOrderByIdAsc();
        if (!head.isInitialized()) {
            boolean isGenesisHead = head.getEntryCount() == 0
                    && hasher.matches(
                            AuditIntegrityHasher.GENESIS_HASH,
                            head.getLastEntryHash())
                    && hasher.matches(
                            hasher.headHash(AuditIntegrityHasher.GENESIS_HASH, 0),
                            head.getHeadHash());
            if (!isGenesisHead) {
                throw new IllegalStateException(
                        "Uninitialized audit chain contains unexpected integrity state");
            }

            if (!logs.isEmpty()) {
                throw new IllegalStateException(
                        "Unsealed legacy audit records cannot be verified; "
                                + "refusing to seal or modify the existing history");
            }

            String previousHash = AuditIntegrityHasher.GENESIS_HASH;
            long count = 0;
            head.initialize(
                    previousHash, count, hasher.headHash(previousHash, count));
            headRepository.saveAndFlush(head);
            return;
        }

        verify(head, logs);
    }

    private void verify(AuditChainHead head, List<AuditLog> logs) {
        String previousHash = AuditIntegrityHasher.GENESIS_HASH;
        long count = 0;

        for (AuditLog log : logs) {
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
                throw new IllegalStateException(
                        "Audit chain is invalid at record " + log.getId());
            }
            previousHash = calculated;
            count++;
        }

        if (!headMatches(head, previousHash, count)) {
            throw new IllegalStateException(
                    "Audit chain head does not match the recorded history");
        }
    }

    private boolean headMatches(
            AuditChainHead head, String lastHash, long count) {
        return head.getEntryCount() == count
                && hasher.matches(lastHash, head.getLastEntryHash())
                && hasher.matches(
                        hasher.headHash(lastHash, count), head.getHeadHash());
    }
}
