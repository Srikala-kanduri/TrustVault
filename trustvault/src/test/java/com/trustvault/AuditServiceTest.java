
package com.trustvault;

import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

class AuditServiceTest {

    private AuditService service(
            AuditLogRepository repository,
            AuditChainHeadRepository headRepository) {
        AuditIntegrityHasher hasher = new AuditIntegrityHasher(
                "trustvault-test-audit-hmac-key-32-bytes-minimum");
        AuditChainHead head = new AuditChainHead(
                AuditIntegrityHasher.GENESIS_HASH,
                hasher.headHash(AuditIntegrityHasher.GENESIS_HASH, 0));
        head.initialize(
                AuditIntegrityHasher.GENESIS_HASH,
                0,
                hasher.headHash(AuditIntegrityHasher.GENESIS_HASH, 0));
        when(headRepository.lockForWrite(AuditChainHead.SINGLETON_ID))
                .thenReturn(Optional.of(head));
        when(headRepository.saveAndFlush(any(AuditChainHead.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.saveAndFlush(any(AuditLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        return new AuditService(repository, headRepository, hasher);
    }

    @Test
    void recordSuccessSavesSuccessfulAuditEvent() {
        AuditLogRepository repository = mock(AuditLogRepository.class);
        AuditChainHeadRepository headRepository =
                mock(AuditChainHeadRepository.class);
        AuditService service = service(repository, headRepository);

        service.recordSuccess(
                "Owner@Example.com",
                "DOCUMENT_UPLOAD",
                10L,
                "Document uploaded");

        verify(repository).saveAndFlush(org.mockito.ArgumentMatchers.argThat(log ->
                log.getActorEmail().equals("owner@example.com")
                        && log.getAction().equals("DOCUMENT_UPLOAD")
                        && log.getDocumentId().equals(10L)
                        && log.getOutcome().equals("SUCCESS")));
    }

    @Test
    void recordDeniedSavesDeniedAuditEvent() {
        AuditLogRepository repository = mock(AuditLogRepository.class);
        AuditChainHeadRepository headRepository =
                mock(AuditChainHeadRepository.class);
        AuditService service = service(repository, headRepository);

        service.recordDenied(
                "viewer@example.com",
                "DOCUMENT_DOWNLOAD",
                10L,
                "Read permission missing");

        verify(repository).saveAndFlush(org.mockito.ArgumentMatchers.argThat(log ->
                log.getActorEmail().equals("viewer@example.com")
                        && log.getAction().equals("DOCUMENT_DOWNLOAD")
                        && log.getDocumentId().equals(10L)
                        && log.getOutcome().equals("DENIED")));
    }
}
