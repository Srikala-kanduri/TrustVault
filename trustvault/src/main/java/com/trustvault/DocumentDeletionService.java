
package com.trustvault;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class DocumentDeletionService {

    private final DocumentAuthorizationService authorizationService;
    private final DocumentRepository documentRepository;
    private final DocumentPermissionRepository permissionRepository;
    private final DocumentFileStorageService storageService;
    private final AuditService auditService;

    public DocumentDeletionService(
            DocumentAuthorizationService authorizationService,
            DocumentRepository documentRepository,
            DocumentPermissionRepository permissionRepository,
            DocumentFileStorageService storageService,
            AuditService auditService) {
        this.authorizationService = authorizationService;
        this.documentRepository = documentRepository;
        this.permissionRepository = permissionRepository;
        this.storageService = storageService;
        this.auditService = auditService;
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteDocument(Long documentId, String authenticatedEmail)
            throws IOException {

        String actor = normalizeEmail(authenticatedEmail);

        Document document = authorizationService.requireReadAccess(
                documentId, actor);

        if (!document.getOwner().getEmail().equalsIgnoreCase(actor)) {
            auditDeniedSafely(actor, documentId);
            throw new AccessDeniedException(
                    "Only the document owner can delete it");
        }

        if (!TransactionSynchronizationManager.isSynchronizationActive()
                || !TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException(
                    "Document deletion requires an active transaction");
        }

        String storageKey = document.getStorageKey();

        // Move the file out of its normal location before database deletion.
        Path quarantined = storageService.quarantine(storageKey);

        try {
            permissionRepository.deleteByDocument_Id(documentId);
            documentRepository.delete(document);
            documentRepository.flush();

            // REQUIRES_NEW keeps the audit record independent of this transaction.
           auditService.record(
        actor,
        "DOCUMENT_DELETE",
        documentId,
        "Document deletion initiated; awaiting transaction commit",
        "PENDING");

        } catch (RuntimeException | Error exception) {
    restoreAfterFailure(storageKey, quarantined, exception);
    throw exception;
}

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
public void afterCommit() {
    try {
        auditService.recordSuccess(
                actor,
                "DOCUMENT_DELETE",
                documentId,
                "Document deletion committed successfully"
        );
    } catch (RuntimeException auditException) {
        // The database deletion is committed. Do not pretend it rolled back.
        // The earlier PENDING audit entry remains for investigation.
    }

    try {
        Files.deleteIfExists(quarantined);
    } catch (IOException cleanupException) {
        try {
            auditService.record(
                    actor,
                    "DOCUMENT_DELETE_CLEANUP",
                    documentId,
                    "Quarantined file cleanup failed; manual cleanup may be required",
                    "FAILED"
            );
        } catch (RuntimeException auditException) {
            // Preserve transaction completion.
        }
    }
}

                    @Override
                    public void afterCompletion(int status) {
                        if (status == STATUS_ROLLED_BACK) {
                            try {
                                storageService.restore(storageKey, quarantined);
                            } catch (IOException | RuntimeException ignored) {
                                // Restoration failure needs operational recovery.
                            }
                        }
                    }
                });
    }

    private void restoreAfterFailure(
            String storageKey,
            Path quarantined,
            Throwable originalException) {
        try {
            storageService.restore(storageKey, quarantined);
        } catch (IOException | RuntimeException restoreException) {
            originalException.addSuppressed(restoreException);
        }
    }

    private void auditDeniedSafely(String actor, Long documentId) {
        try {
            auditService.recordDenied(
                    actor,
                    "DOCUMENT_DELETE",
                    documentId,
                    "Only the document owner can delete it");
        } catch (RuntimeException ignored) {
            // Preserve the original access-denied exception.
        }
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new AccessDeniedException("Authentication required");
        }

        return email.trim().toLowerCase(Locale.ROOT);
    }
}
