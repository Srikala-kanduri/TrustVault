
package com.trustvault;

import java.util.Locale;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DocumentUpdateService {

    private final DocumentAuthorizationService authorizationService;
    private final DocumentRepository documentRepository;
    private final AuditService auditService;

    public DocumentUpdateService(
            DocumentAuthorizationService authorizationService,
            DocumentRepository documentRepository,
            AuditService auditService) {
        this.authorizationService = authorizationService;
        this.documentRepository = documentRepository;
        this.auditService = auditService;
    }

    @Transactional
    public DocumentResponse updateMetadata(
            Long documentId,
            String authenticatedEmail,
            String title,
            String category) {

        String actor = normalizeEmail(authenticatedEmail);

        try {
            if (title == null || title.isBlank()
                    || title.trim().length() > 200) {
                throw new IllegalArgumentException(
                        "Title must contain 1–200 characters");
            }

            if (category == null || category.isBlank()
                    || category.trim().length() > 100) {
                throw new IllegalArgumentException(
                        "Category must contain 1–100 characters");
            }

            Document document = authorizationService.requireEditAccess(
                    documentId, actor);

            String oldTitle = document.getTitle();
            String oldCategory = document.getCategory();

            document.updateMetadata(title.trim(), category.trim());

            Document saved = documentRepository.saveAndFlush(document);

            auditService.recordInCurrentTransaction(
                    actor,
                    "DOCUMENT_UPDATED",
                    documentId,
                    "Metadata changed: title '" + oldTitle + "' -> '"
                            + saved.getTitle() + "'; category '" + oldCategory
                            + "' -> '" + saved.getCategory() + "'",
                    "SUCCESS");

            return DocumentResponse.from(saved);

        } catch (AccessDeniedException exception) {
            recordDenied(actor, documentId);
            throw exception;
        }
    }

    private void recordDenied(String actor, Long documentId) {
        try {
            auditService.recordDenied(
                    actor,
                    "DOCUMENT_UPDATED",
                    documentId,
                    "Document metadata update denied");
        } catch (RuntimeException ignored) {
            // Preserve the original authorization denial.
        }
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new AccessDeniedException("Document access denied");
        }

        return email.trim().toLowerCase(Locale.ROOT);
    }
}
