
package com.trustvault;

import java.util.Locale;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DocumentPermissionService {

    private final DocumentRepository documentRepository;
    private final DocumentPermissionRepository permissionRepository;
    private final AppUserRepository userRepository;
    private final AuditService auditService;

    public DocumentPermissionService(
            DocumentRepository documentRepository,
            DocumentPermissionRepository permissionRepository,
            AppUserRepository userRepository,
            AuditService auditService) {
        this.documentRepository = documentRepository;
        this.permissionRepository = permissionRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Transactional
    public DocumentPermission grantPermission(
            Long documentId,
            String requesterEmail,
            String targetEmail,
            DocumentPermission.PermissionLevel level) {

        try {
            if (level == null) {
                throw new IllegalArgumentException(
                        "Permission level is required");
            }

            AppUser requester = findUser(requesterEmail);
            Document document = findDocument(documentId);
            requireOwner(document, requester);

            AppUser target = findUser(targetEmail);

            if (target.getId().equals(requester.getId())) {
                throw new IllegalArgumentException(
                        "The owner already has access to this document");
            }

            DocumentPermission existing = permissionRepository
                    .findByDocument_IdAndUser_Id(documentId, target.getId())
                    .orElse(null);

            DocumentPermission savedPermission;

            if (existing != null) {
                existing.setPermissionLevel(level);
                savedPermission = existing;
            } else {
                DocumentPermission permission =
                        new DocumentPermission(document, target, level);
                savedPermission = permissionRepository.save(permission);
            }

            auditRequired(
                    normalizeEmail(requesterEmail),
                    "PERMISSION_GRANT",
                    documentId,
                    "Permission " + level + " granted or updated for "
                            + normalizeEmail(targetEmail),
                    "SUCCESS");

            return savedPermission;

        } catch (AccessDeniedException exception) {
            auditDeniedSafely(
                    normalizeEmail(requesterEmail),
                    "PERMISSION_GRANT",
                    documentId,
                    "Permission grant denied");

            throw exception;

        } catch (IllegalArgumentException exception) {
            auditDeniedSafely(
                    normalizeEmail(requesterEmail),
                    "PERMISSION_GRANT",
                    documentId,
                    "Permission grant denied");

            throw exception;
        }
    }

    @Transactional
    public void revokePermission(
            Long documentId,
            String requesterEmail,
            String targetEmail) {

        try {
            AppUser requester = findUser(requesterEmail);
            Document document = findDocument(documentId);
            requireOwner(document, requester);

            AppUser target = findUser(targetEmail);

            if (target.getId().equals(requester.getId())) {
                throw new IllegalArgumentException(
                        "The owner does not need a permission grant");
            }

            boolean permissionExists =
                    permissionRepository.existsByDocument_IdAndUser_Id(
                            documentId, target.getId());

            if (!permissionExists) {
                throw new IllegalArgumentException(
                        "No permission grant exists for this user");
            }

            permissionRepository.deleteByDocument_IdAndUser_Id(
                    documentId, target.getId());

            auditRequired(
                    normalizeEmail(requesterEmail),
                    "PERMISSION_REVOKE",
                    documentId,
                    "Permission revoked for "
                            + normalizeEmail(targetEmail),
                    "SUCCESS");

        } catch (AccessDeniedException exception) {
            auditDeniedSafely(
                    normalizeEmail(requesterEmail),
                    "PERMISSION_REVOKE",
                    documentId,
                    "Permission revocation denied");
            throw exception;

        } catch (IllegalArgumentException exception) {
            auditDeniedSafely(
                    normalizeEmail(requesterEmail),
                    "PERMISSION_REVOKE",
                    documentId,
                    "Permission revocation denied");
            throw exception;
        }
    }

    
    private void auditRequired(
            String actor,
            String action,
            Long documentId,
            String details,
            String outcome) {

        auditService.record(
                actor, action, documentId, details, outcome);
    }

    private void auditDeniedSafely(
            String actor,
            String action,
            Long documentId,
            String details) {

        try {
            auditService.record(
                    actor, action, documentId, details, "DENIED");
        } catch (RuntimeException exception) {
            // Preserve the original authorization failure.
        }
    }


    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return "unknown";
        }

        return email.trim().toLowerCase(Locale.ROOT);
    }

    private AppUser findUser(String email) {
        if (email == null || email.isBlank()) {
            throw new AccessDeniedException(
                    "Permission operation denied");
        }

        return userRepository.findByEmail(
                        email.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() ->
                        new AccessDeniedException(
                                "Permission operation denied"));
    }

    private Document findDocument(Long documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() ->
                        new AccessDeniedException(
                                "Permission operation denied"));
    }

    private void requireOwner(Document document, AppUser requester) {
        if (!document.getOwner().getId().equals(requester.getId())) {
            throw new AccessDeniedException(
                    "Permission operation denied");
        }
    }
}
