
package com.trustvault;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.mockito.Mockito.doThrow;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocumentDownloadServiceTest {

    @Test
    void authorizedUserCanDownloadDocument() throws IOException {
        DocumentAuthorizationService authorizationService =
                mock(DocumentAuthorizationService.class);
        DocumentFileStorageService storageService =
                mock(DocumentFileStorageService.class);
        AuditService auditService = mock(AuditService.class);
        Document document = mock(Document.class);

        Path file = Files.createTempFile("trustvault-test-", ".pdf");

        try {
            byte[] expected = "test document content".getBytes();
            Files.write(file, expected);

            when(authorizationService.requireReadAccess(
                    1L, "owner@example.com")).thenReturn(document);
            when(document.getTitle()).thenReturn("Contract");
            when(document.getStorageKey()).thenReturn(
                    "12345678-1234-1234-1234-123456789012");
            when(document.getOriginalFilename()).thenReturn("contract.pdf");
            when(storageService.resolve(
                    "12345678-1234-1234-1234-123456789012"))
                    .thenReturn(file);

            DocumentDownloadService service =
                    new DocumentDownloadService(
                            authorizationService, storageService, auditService);

            DocumentDownloadService.DownloadedDocument result =
                    service.download(1L, "owner@example.com");

            assertArrayEquals(expected, result.content());
            assertEquals("contract.pdf", result.filename());

            verify(authorizationService)
                    .requireReadAccess(1L, "owner@example.com");
            verify(storageService).resolve(
                    "12345678-1234-1234-1234-123456789012");
            verify(auditService).recordSuccess(
                    "owner@example.com",
                    "DOCUMENT_DOWNLOAD",
                    1L,
                    "Document downloaded: Contract (contract.pdf)");
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    void unauthorizedUserCannotAccessStoredFile() throws IOException {
        DocumentAuthorizationService authorizationService =
                mock(DocumentAuthorizationService.class);
        DocumentFileStorageService storageService =
                mock(DocumentFileStorageService.class);
        AuditService auditService = mock(AuditService.class);

        when(authorizationService.requireReadAccess(
                1L, "intruder@example.com"))
                .thenThrow(new AccessDeniedException("Access denied"));

        DocumentDownloadService service =
                new DocumentDownloadService(
                        authorizationService, storageService, auditService);

        assertThrows(
                AccessDeniedException.class,
                () -> service.download(1L, "intruder@example.com"));

        verify(auditService).recordDenied(
                "intruder@example.com",
                "DOCUMENT_DOWNLOAD",
                1L,
                "Read permission denied");

        verify(storageService, never()).resolve(anyString());
    }
    
@Test
void revokedUserCannotDownloadDocument() throws IOException {
    DocumentAuthorizationService authorizationService =
            mock(DocumentAuthorizationService.class);
    DocumentFileStorageService storageService =
            mock(DocumentFileStorageService.class);
    AuditService auditService = mock(AuditService.class);

    when(authorizationService.requireReadAccess(
            1L, "viewer@example.com"))
            .thenThrow(new AccessDeniedException("Access denied"));

    DocumentDownloadService service =
            new DocumentDownloadService(
                    authorizationService, storageService, auditService);

    assertThrows(
            AccessDeniedException.class,
            () -> service.download(1L, "viewer@example.com"));

    verify(authorizationService)
            .requireReadAccess(1L, "viewer@example.com");

    verify(auditService).recordDenied(
            "viewer@example.com",
            "DOCUMENT_DOWNLOAD",
            1L,
            "Read permission denied");

    verify(storageService, never()).resolve(anyString());
}

@Test
void failedDownloadIsAudited() throws IOException {
    DocumentAuthorizationService authorizationService =
            mock(DocumentAuthorizationService.class);
    DocumentFileStorageService storageService =
            mock(DocumentFileStorageService.class);
    AuditService auditService = mock(AuditService.class);
    Document document = mock(Document.class);

    when(authorizationService.requireReadAccess(
            1L, "owner@example.com")).thenReturn(document);

    when(document.getStorageKey()).thenReturn(
            "12345678-1234-1234-1234-123456789012");

    Path missingFile = Path.of(
            "trustvault-test-file-that-does-not-exist.pdf");

    when(storageService.resolve(
            "12345678-1234-1234-1234-123456789012"))
            .thenReturn(missingFile.toAbsolutePath());

    DocumentDownloadService service = new DocumentDownloadService(
            authorizationService, storageService, auditService);

    assertThrows(
            IOException.class,
            () -> service.download(1L, "owner@example.com"));

    verify(auditService).record(
            "owner@example.com",
            "DOCUMENT_DOWNLOAD",
            1L,
            "Download failed",
            "FAILED");
}

@Test
void auditFailureDoesNotHideAccessDenial() throws IOException {
    DocumentAuthorizationService authorizationService =
            mock(DocumentAuthorizationService.class);
    DocumentFileStorageService storageService =
            mock(DocumentFileStorageService.class);
    AuditService auditService = mock(AuditService.class);

    when(authorizationService.requireReadAccess(
            1L, "intruder@example.com"))
            .thenThrow(new AccessDeniedException("Access denied"));

    doThrow(new RuntimeException("Audit database unavailable"))
            .when(auditService).recordDenied(
                    "intruder@example.com",
                    "DOCUMENT_DOWNLOAD",
                    1L,
                    "Read permission denied");

    DocumentDownloadService service = new DocumentDownloadService(
            authorizationService, storageService, auditService);

    assertThrows(
            AccessDeniedException.class,
            () -> service.download(1L, "intruder@example.com"));

    verify(storageService, never()).resolve(anyString());
}

}
