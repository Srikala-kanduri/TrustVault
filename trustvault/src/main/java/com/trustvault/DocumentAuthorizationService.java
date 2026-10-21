
package com.trustvault;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DocumentAuthorizationService {

    private final DocumentRepository documentRepository;
    private final DocumentPermissionRepository permissionRepository;
    private final AppUserRepository userRepository;

    public DocumentAuthorizationService(
            DocumentRepository documentRepository,
            DocumentPermissionRepository permissionRepository,
            AppUserRepository userRepository) {
        this.documentRepository = documentRepository;
        this.permissionRepository = permissionRepository;
        this.userRepository = userRepository;
    }

    public Document requireReadAccess(Long documentId, String email) {
        AppUser user = findUser(email);
        Document document = findDocument(documentId);

        if (document.getOwner().getId().equals(user.getId())) {
            return document;
        }

        DocumentPermission permission = permissionRepository
                .findByDocument_IdAndUser_Id(documentId, user.getId())
                .orElseThrow(() ->
                        new AccessDeniedException("Document access denied"));

        if (permission.getPermissionLevel()
                != DocumentPermission.PermissionLevel.READ
                && permission.getPermissionLevel()
                != DocumentPermission.PermissionLevel.EDIT) {
            throw new AccessDeniedException("Document access denied");
        }

        return document;
    }

    public Document requireEditAccess(Long documentId, String email) {
        AppUser user = findUser(email);
        Document document = findDocument(documentId);

        if (document.getOwner().getId().equals(user.getId())) {
            return document;
        }

        DocumentPermission permission = permissionRepository
                .findByDocument_IdAndUser_Id(documentId, user.getId())
                .orElseThrow(() ->
                        new AccessDeniedException("Document access denied"));

        if (permission.getPermissionLevel()
                != DocumentPermission.PermissionLevel.EDIT) {
            throw new AccessDeniedException("Document access denied");
        }

        return document;
    }

    private AppUser findUser(String email) {
        return userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() ->
                        new AccessDeniedException("Document access denied"));
    }

    private Document findDocument(Long documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() ->
                        new AccessDeniedException("Document access denied"));
    }
}
