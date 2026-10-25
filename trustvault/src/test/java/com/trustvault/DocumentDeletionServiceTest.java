
package com.trustvault;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class DocumentDeletionServiceTest {

    @Test
    void userWithoutReadAccessCannotDeleteAndAttemptIsAudited()
            throws Exception {
        DocumentAuthorizationService authorizationService =
                mock(DocumentAuthorizationService.class);
        DocumentRepository documentRepository =
                mock(DocumentRepository.class);
        DocumentPermissionRepository permissionRepository =
                mock(DocumentPermissionRepository.class);
        DocumentFileStorageService storageService =
                mock(DocumentFileStorageService.class);
        AuditService auditService = mock(AuditService.class);

        when(authorizationService.requireReadAccess(
                10L, "intruder@example.com"))
                .thenThrow(new AccessDeniedException("Document access denied"));

        DocumentDeletionService service = new DocumentDeletionService(
                authorizationService,
                documentRepository,
                permissionRepository,
                storageService,
                auditService);

        assertThrows(
                AccessDeniedException.class,
                () -> service.deleteDocument(10L, "intruder@example.com"));

        verify(auditService).recordDenied(
                "intruder@example.com",
                "DOCUMENT_DELETE",
                10L,
                "Document deletion denied: caller has no read permission");
        verify(storageService, never()).quarantine(
                org.mockito.ArgumentMatchers.anyString());
        verify(documentRepository, never()).delete(
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void nonOwnerCannotDeleteDocument() throws Exception {
        DocumentAuthorizationService authorizationService =
                mock(DocumentAuthorizationService.class);
        DocumentRepository documentRepository =
                mock(DocumentRepository.class);
        DocumentPermissionRepository permissionRepository =
                mock(DocumentPermissionRepository.class);
        DocumentFileStorageService storageService =
                mock(DocumentFileStorageService.class);
        AuditService auditService = mock(AuditService.class);

        Document document = mock(Document.class);
        AppUser owner = mock(AppUser.class);

        when(authorizationService.requireReadAccess(
                10L, "viewer@example.com")).thenReturn(document);
        when(document.getOwner()).thenReturn(owner);
        when(owner.getEmail()).thenReturn("owner@example.com");

        DocumentDeletionService service = new DocumentDeletionService(
                authorizationService,
                documentRepository,
                permissionRepository,
                storageService,
                auditService);

        assertThrows(
                AccessDeniedException.class,
                () -> service.deleteDocument(10L, "viewer@example.com"));

        verify(auditService).recordDenied(
                "viewer@example.com",
                "DOCUMENT_DELETE",
                10L,
                "Only the document owner can delete it");

        verify(storageService, never()).quarantine(
                org.mockito.ArgumentMatchers.anyString());
        verify(permissionRepository, never()).deleteByDocument_Id(10L);
        verify(documentRepository, never()).delete(document);
    }
}
