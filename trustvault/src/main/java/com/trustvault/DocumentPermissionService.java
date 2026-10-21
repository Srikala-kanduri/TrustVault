
package com.trustvault;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DocumentPermissionService {

    private final DocumentRepository documentRepository;
    private final DocumentPermissionRepository permissionRepository;
    private final AppUserRepository userRepository;

    public DocumentPermissionService(
            DocumentRepository documentRepository,
            DocumentPermissionRepository permissionRepository,
            AppUserRepository userRepository) {
        this.documentRepository = documentRepository;
        this.permissionRepository = permissionRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public DocumentPermission grantPermission(
            Long documentId,
            String requesterEmail,
            String targetEmail,
            DocumentPermission.PermissionLevel level) {

        if (level == null) {
            throw new IllegalArgumentException("Permission level is required");
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

        if (existing != null) {
            existing.setPermissionLevel(level);
            return existing;
        }

        DocumentPermission permission =
                new DocumentPermission(document, target, level);

        return permissionRepository.save(permission);
    }

    @Transactional
    public void revokePermission(
            Long documentId,
            String requesterEmail,
            String targetEmail) {

        AppUser requester = findUser(requesterEmail);
        Document document = findDocument(documentId);

        requireOwner(document, requester);

        AppUser target = findUser(targetEmail);

        if (target.getId().equals(requester.getId())) {
            throw new IllegalArgumentException(
                    "The owner does not need a permission grant");
        }

        permissionRepository.deleteByDocument_IdAndUser_Id(
                documentId, target.getId());
    }

    private AppUser findUser(String email) {
        if (email == null || email.isBlank()) {
            throw new AccessDeniedException("Permission operation denied");
        }

        return userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() ->
                        new AccessDeniedException("Permission operation denied"));
    }

    private Document findDocument(Long documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() ->
                        new AccessDeniedException("Permission operation denied"));
    }

    private void requireOwner(Document document, AppUser requester) {
        if (!document.getOwner().getId().equals(requester.getId())) {
            throw new AccessDeniedException("Permission operation denied");
        }
    }
}
