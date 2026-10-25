package com.trustvault;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.multipart.MultipartFile;

class DocumentFileReplacementServiceTest {

    @Test
    void deniedReplacementIsAuditedBeforeFileValidation() throws Exception {
        DocumentAuthorizationService authorization =
                mock(DocumentAuthorizationService.class);
        DocumentRepository documents = mock(DocumentRepository.class);
        DocumentFileStorageService storage =
                mock(DocumentFileStorageService.class);
        DocumentFileValidator validator = mock(DocumentFileValidator.class);
        AuditService audit = mock(AuditService.class);
        DocumentFileReplacementService service =
                new DocumentFileReplacementService(
                        authorization, documents, storage, validator, audit);
        MultipartFile file = mock(MultipartFile.class);

        when(authorization.requireEditAccess(42L, "viewer@example.com"))
                .thenThrow(new AccessDeniedException("Edit permission required"));

        assertThrows(
                AccessDeniedException.class,
                () -> service.replaceFile(42L, "viewer@example.com", file));

        verify(audit).recordDenied(
                "viewer@example.com",
                "DOCUMENT_FILE_REPLACED",
                42L,
                "File replacement denied");
        verify(validator, never()).validate(file);
    }
}
