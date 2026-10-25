
package com.trustvault;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Locale;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentFileReplacementService {

    private final DocumentAuthorizationService authorizationService;
    private final DocumentRepository documentRepository;
    private final DocumentFileStorageService storageService;
    private final DocumentFileValidator fileValidator;
    private final AuditService auditService;

    public DocumentFileReplacementService(
            DocumentAuthorizationService authorizationService,
            DocumentRepository documentRepository,
            DocumentFileStorageService storageService,
            DocumentFileValidator fileValidator,
            AuditService auditService) {
        this.authorizationService = authorizationService;
        this.documentRepository = documentRepository;
        this.storageService = storageService;
        this.fileValidator = fileValidator;
        this.auditService = auditService;
    }

    @Transactional
    public DocumentResponse replaceFile(
            Long documentId,
            String authenticatedEmail,
            MultipartFile file) throws IOException {

        String actor = normalizeEmail(authenticatedEmail);

        Document document;
        try {
            document = authorizationService.requireEditAccess(documentId, actor);
        } catch (AccessDeniedException exception) {
            recordDenied(actor, documentId);
            throw exception;
        }

        fileValidator.validate(file);

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null
                || originalFilename.isBlank()
                || originalFilename.length() > 255) {
            throw new IllegalArgumentException("Invalid filename");
        }

        String oldStorageKey = document.getStorageKey();
        String oldFilename = document.getOriginalFilename();
        String newStorageKey = storageService.store(file);

        try {
            document.replaceFileMetadata(originalFilename, newStorageKey);
            Document saved = documentRepository.saveAndFlush(document);

            auditService.recordInCurrentTransaction(
                    actor,
                    "DOCUMENT_FILE_REPLACED",
                    documentId,
                    "File replaced: '" + oldFilename + "' -> '"
                            + originalFilename + "'",
                    "SUCCESS");

            registerTransactionCleanup(
                    actor, documentId, newStorageKey, oldStorageKey);

            return DocumentResponse.from(saved);

        } catch (RuntimeException exception) {
            document.replaceFileMetadata(oldFilename, oldStorageKey);

            try {
                Files.deleteIfExists(storageService.resolve(newStorageKey));
            } catch (IOException | RuntimeException cleanupException) {
                exception.addSuppressed(cleanupException);
            }

            throw exception;
        }
    }

    private void recordDenied(String actor, Long documentId) {
        try {
            auditService.recordDenied(
                    actor,
                    "DOCUMENT_FILE_REPLACED",
                    documentId,
                    "File replacement denied");
        } catch (RuntimeException ignored) {
            // Preserve the original authorization denial.
        }
    }

    private void registerTransactionCleanup(
            String actor,
            Long documentId,
            String newStorageKey,
            String oldStorageKey) {

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException(
                    "Transaction synchronization is not active");
        }

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {

                    @Override
                    public void afterCommit() {
                        try {
                            Files.deleteIfExists(
                                    storageService.resolve(oldStorageKey));
                        } catch (IOException | RuntimeException cleanupException) {
                            try {
                                auditService.record(
                                        actor,
                                        "DOCUMENT_FILE_CLEANUP",
                                        documentId,
                                        "Old file cleanup failed",
                                        "FAILED");
                            } catch (RuntimeException ignored) {
                                // Keep the old file if cleanup or audit fails.
                            }
                        }
                    }

                    @Override
                    public void afterCompletion(int status) {
                        if (status != STATUS_COMMITTED) {
                            try {
                                Files.deleteIfExists(
                                        storageService.resolve(newStorageKey));
                            } catch (IOException | RuntimeException ignored) {
                                // Failed cleanup requires investigation.
                            }
                        }
                    }
                });
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new AccessDeniedException("Document access denied");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
