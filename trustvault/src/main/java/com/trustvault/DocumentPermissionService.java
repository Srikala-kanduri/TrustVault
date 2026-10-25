
package com.trustvault;

import java.util.Locale;
import java.util.List;
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
            String permissionChangeDetails;

            if (existing != null) {
                DocumentPermission.PermissionLevel previousLevel =
                        existing.getPermissionLevel();
                existing.setPermissionLevel(level);
                savedPermission = existing;
                permissionChangeDetails = "Permission for "
                        + normalizeEmail(targetEmail) + " changed from "
                        + previousLevel + " to " + level;
            } else {
                DocumentPermission permission =
                        new DocumentPermission(document, target, level);
                savedPermission = permissionRepository.save(permission);
                permissionChangeDetails = "Permission " + level
                        + " granted to " + normalizeEmail(targetEmail);
            }

            auditRequired(
                    normalizeEmail(requesterEmail),
                    "PERMISSION_GRANT",
                    documentId,
                    permissionChangeDetails,
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

            DocumentPermission existingPermission = permissionRepository
                    .findByDocument_IdAndUser_Id(documentId, target.getId())
                    .orElse(null);

            if (existingPermission == null) {
                throw new IllegalArgumentException(
                        "No permission grant exists for this user");
            }

            DocumentPermission.PermissionLevel revokedLevel =
                    existingPermission.getPermissionLevel();

            permissionRepository.deleteByDocument_IdAndUser_Id(
                    documentId, target.getId());

            auditRequired(
                    normalizeEmail(requesterEmail),
                    "PERMISSION_REVOKE",
                    documentId,
                    "Permission " + revokedLevel + " revoked for "
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

        auditService.recordInCurrentTransaction(
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


@Transactional(readOnly = true)
public List<DocumentPermission> listPermissions(
        Long documentId,
        String requesterEmail) {

    AppUser requester = findUser(requesterEmail);
    Document document = findDocument(documentId);

    requireOwner(document, requester);

    return permissionRepository.findAllByDocument_Id(documentId);
}


@Transactional(readOnly = true)
public List<AccessGrantResponse> listAllGrants(
        String requesterEmail) {

    AppUser requester = findUser(requesterEmail);

    return permissionRepository
            .findAllByDocument_Owner_Id(requester.getId())
            .stream()
            .map(permission -> new AccessGrantResponse(
                    permission.getDocument().getId(),
                    permission.getDocument().getTitle(),
                    permission.getUser().getEmail(),
                    permission.getPermissionLevel()))
            .toList();
}


}
