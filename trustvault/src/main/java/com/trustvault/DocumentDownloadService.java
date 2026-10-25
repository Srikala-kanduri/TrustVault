
package com.trustvault;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class DocumentDownloadService {

    private final DocumentAuthorizationService authorizationService;
    private final DocumentFileStorageService storageService;
    private final AuditService auditService;

    public DocumentDownloadService(
            DocumentAuthorizationService authorizationService,
            DocumentFileStorageService storageService,
            AuditService auditService) {
        this.authorizationService = authorizationService;
        this.storageService = storageService;
        this.auditService = auditService;
    }

    public DownloadedDocument download(Long documentId, String email)
            throws IOException {

        Document document;

        try {
            document = authorizationService.requireReadAccess(
                    documentId, email);
        } catch (AccessDeniedException exception) {
            recordDeniedSafely(
                    email,
                    documentId,
                    "Read permission denied");
            throw exception;
        }

        try {
            Path path = storageService.resolve(document.getStorageKey());

            if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
                throw new java.nio.file.NoSuchFileException(
                        "Stored document is unavailable");
            }

            byte[] content = Files.readAllBytes(path);

            auditService.recordSuccess(
                    email,
                    "DOCUMENT_DOWNLOAD",
                    documentId,
                    "Document downloaded: " + document.getTitle()
                            + " (" + document.getOriginalFilename() + ")");

            return new DownloadedDocument(
                    content,
                    document.getOriginalFilename());

        } catch (IOException | RuntimeException exception) {
            if (!(exception instanceof AccessDeniedException)) {
                recordFailureSafely(
                        email,
                        documentId,
                        "Download failed");
            }
            throw exception;
        }
    }

    private void recordDeniedSafely(
            String email,
            Long documentId,
            String details) {
        try {
            auditService.recordDenied(
                    normalizeEmail(email),
                    "DOCUMENT_DOWNLOAD",
                    documentId,
                    details);
        } catch (RuntimeException ignored) {
            // Preserve the original access-denied exception.
        }
    }

    private void recordFailureSafely(
            String email,
            Long documentId,
            String details) {
        try {
            auditService.record(
                    normalizeEmail(email),
                    "DOCUMENT_DOWNLOAD",
                    documentId,
                    details,
                    "FAILED");
        } catch (RuntimeException ignored) {
            // Preserve the original download failure.
        }
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return "unknown";
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public record DownloadedDocument(byte[] content, String filename) {

        public DownloadedDocument {
            content = content.clone();
        }

        @Override
        public byte[] content() {
            return content.clone();
        }
    }
}
