
package com.trustvault;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import org.mockito.Mock;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

class DocumentControllerTest {
    @Mock
    private DocumentUpdateService updateService;

    @org.mockito.Mock
private DocumentFileReplacementService fileReplacementService;

    @Test
    void getDocumentChecksReadAccessBeforeReturningMetadata() {
        DocumentAuthorizationService authorizationService =
                mock(DocumentAuthorizationService.class);
        DocumentUploadService uploadService =
                mock(DocumentUploadService.class);
        DocumentDownloadService downloadService =
                mock(DocumentDownloadService.class);

        Authentication authentication = mock(Authentication.class);
        Document document = mock(Document.class);
        AppUser owner = mock(AppUser.class);
        AuditService auditService = mock(AuditService.class);

        DocumentController controller = new DocumentController(
                authorizationService, uploadService, downloadService,
                mock(DocumentDeletionService.class), updateService,
                fileReplacementService, auditService);

        when(authentication.getName()).thenReturn("owner@example.com");
        when(authorizationService.requireReadAccess(
                10L, "owner@example.com")).thenReturn(document);

        when(document.getId()).thenReturn(10L);
        when(document.getTitle()).thenReturn("Contract");
        when(document.getOriginalFilename()).thenReturn("contract.pdf");
        when(document.getCategory()).thenReturn("Legal");
        when(document.getOwner()).thenReturn(owner);
        when(owner.getEmail()).thenReturn("owner@example.com");

        DocumentResponse response =
                controller.getDocument(10L, authentication);

        assertEquals(10L, response.id());
        assertEquals("Contract", response.title());
        assertEquals("contract.pdf", response.originalFilename());

        verify(authorizationService)
                .requireReadAccess(10L, "owner@example.com");
        verify(auditService).recordSuccess(
                "owner@example.com",
                "DOCUMENT_READ",
                10L,
                "Document details viewed: Contract");
    }

    @Test
    void deniedAccessDoesNotReturnMetadata() {
        DocumentAuthorizationService authorizationService =
                mock(DocumentAuthorizationService.class);
        DocumentUploadService uploadService =
                mock(DocumentUploadService.class);
        DocumentDownloadService downloadService =
                mock(DocumentDownloadService.class);

        Authentication authentication = mock(Authentication.class);

        AuditService auditService = mock(AuditService.class);
        DocumentController controller = new DocumentController(
                authorizationService, uploadService, downloadService,
                mock(DocumentDeletionService.class), updateService,
                fileReplacementService, auditService);

        when(authentication.getName()).thenReturn("viewer@example.com");

        when(authorizationService.requireReadAccess(
                10L, "viewer@example.com"))
                .thenThrow(new AccessDeniedException(
                        "Document access denied"));

        assertThrows(
                AccessDeniedException.class,
                () -> controller.getDocument(10L, authentication));

        verify(auditService).recordDenied(
                "viewer@example.com",
                "DOCUMENT_READ",
                10L,
                "Document detail read denied");
    }

    
@Test
void deleteDocumentCallsDeletionService() throws IOException {
    DocumentAuthorizationService authorizationService =
            mock(DocumentAuthorizationService.class);
    DocumentUploadService uploadService =
            mock(DocumentUploadService.class);
    DocumentDownloadService downloadService =
            mock(DocumentDownloadService.class);
    DocumentDeletionService deletionService =
            mock(DocumentDeletionService.class);
    Authentication authentication = mock(Authentication.class);

    when(authentication.getName()).thenReturn("owner@example.com");

    DocumentController controller = new DocumentController(
            authorizationService,
            uploadService,
            downloadService,
            deletionService, updateService, fileReplacementService,
            mock(AuditService.class));

    controller.deleteDocument(10L, authentication);

    verify(deletionService).deleteDocument(10L, "owner@example.com");
}

@Test
void deleteDocumentPropagatesAccessDenied() throws IOException {
    DocumentAuthorizationService authorizationService =
            mock(DocumentAuthorizationService.class);
    DocumentUploadService uploadService =
            mock(DocumentUploadService.class);
    DocumentDownloadService downloadService =
            mock(DocumentDownloadService.class);
    DocumentDeletionService deletionService =
            mock(DocumentDeletionService.class);
    Authentication authentication = mock(Authentication.class);

    when(authentication.getName()).thenReturn("viewer@example.com");

    doThrow(new AccessDeniedException("Only the document owner can delete it"))
            .when(deletionService)
            .deleteDocument(10L, "viewer@example.com");

    DocumentController controller = new DocumentController(
            authorizationService,
            uploadService,
            downloadService,
            deletionService, updateService, fileReplacementService,
            mock(AuditService.class));

    assertThrows(
            AccessDeniedException.class,
            () -> controller.deleteDocument(10L, authentication));

    verify(deletionService).deleteDocument(10L, "viewer@example.com");
    verify(deletionService, never())
            .deleteDocument(11L, "viewer@example.com");
}

}
