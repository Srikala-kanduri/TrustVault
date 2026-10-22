
package com.trustvault;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentUploadService {

    private final DocumentFileStorageService storageService;
    private final DocumentRepository documentRepository;
    private final AppUserRepository userRepository;
    private final DocumentFileValidator fileValidator;
    private final AuditService auditService;

    public DocumentUploadService(
            DocumentFileStorageService storageService,
            DocumentRepository documentRepository,
            AppUserRepository userRepository,
            DocumentFileValidator fileValidator,
            AuditService auditService) {
        this.storageService = storageService;
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.fileValidator = fileValidator;
        this.auditService = auditService;
    }

    @Transactional
    public DocumentResponse upload(
            String authenticatedEmail,
            String title,
            String category,
            MultipartFile file) throws IOException {

        String actor = normalizeEmail(authenticatedEmail);
        String storageKey = null;
        Long documentId = null;

        try {
            fileValidator.validate(file);

            if (title == null || title.isBlank()
                    || title.trim().length() > 200) {
                throw new IllegalArgumentException(
                        "Invalid document title");
            }

            if (category == null || category.isBlank()
                    || category.trim().length() > 100) {
                throw new IllegalArgumentException(
                        "Invalid document category");
            }

            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException(
                        "File must not be empty");
            }

            String originalFilename = file.getOriginalFilename();

            if (originalFilename == null
                    || originalFilename.isBlank()
                    || originalFilename.length() > 255) {
                throw new IllegalArgumentException(
                        "Invalid filename");
            }

            AppUser owner = userRepository.findByEmail(actor)
                    .orElseThrow(() ->
                            new AccessDeniedException(
                                    "Document access denied"));

            storageKey = storageService.store(file);

            try {
                Document document = new Document(
                        title.trim(),
                        originalFilename,
                        category.trim(),
                        storageKey,
                        owner);

                Document saved = documentRepository.saveAndFlush(document);
                documentId = saved.getId();

                auditService.recordSuccess(
                        actor,
                        "DOCUMENT_UPLOAD",
                        documentId,
                        "Document uploaded");

                return DocumentResponse.from(saved);

            } catch (RuntimeException exception) {
                cleanupStoredFile(storageKey, exception);
                throw exception;
            }

        } catch (IOException | RuntimeException exception) {
            try {
                auditService.record(
                        actor,
                        "DOCUMENT_UPLOAD",
                        documentId,
                        "Upload failed",
                        "FAILED");
            } catch (RuntimeException auditException) {
                exception.addSuppressed(auditException);
            }

            throw exception;
        }
    }

    private void cleanupStoredFile(
            String storageKey,
            RuntimeException originalException) {
        try {
            Path storedPath = storageService.resolve(storageKey);
            Files.deleteIfExists(storedPath);
        } catch (IOException | RuntimeException cleanupException) {
            originalException.addSuppressed(cleanupException);
        }
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return "unknown";
        }

        return email.trim().toLowerCase(Locale.ROOT);
    }
}
