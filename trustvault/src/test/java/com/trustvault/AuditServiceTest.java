
package com.trustvault;

import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuditServiceTest {

    @Test
    void recordSuccessSavesSuccessfulAuditEvent() {
        AuditLogRepository repository = mock(AuditLogRepository.class);
        AuditService service = new AuditService(repository);

        when(repository.save(any(AuditLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.recordSuccess(
                "Owner@Example.com",
                "DOCUMENT_UPLOAD",
                10L,
                "Document uploaded");

        verify(repository).save(org.mockito.ArgumentMatchers.argThat(log ->
                log.getActorEmail().equals("owner@example.com")
                        && log.getAction().equals("DOCUMENT_UPLOAD")
                        && log.getDocumentId().equals(10L)
                        && log.getOutcome().equals("SUCCESS")));
    }

    @Test
    void recordDeniedSavesDeniedAuditEvent() {
        AuditLogRepository repository = mock(AuditLogRepository.class);
        AuditService service = new AuditService(repository);

        when(repository.save(any(AuditLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.recordDenied(
                "viewer@example.com",
                "DOCUMENT_DOWNLOAD",
                10L,
                "Read permission missing");

        verify(repository).save(org.mockito.ArgumentMatchers.argThat(log ->
                log.getActorEmail().equals("viewer@example.com")
                        && log.getAction().equals("DOCUMENT_DOWNLOAD")
                        && log.getDocumentId().equals(10L)
                        && log.getOutcome().equals("DENIED")));
    }
}
