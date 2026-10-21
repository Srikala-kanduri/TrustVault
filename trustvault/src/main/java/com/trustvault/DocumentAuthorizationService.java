
package com.trustvault;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

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

public List<Document> listAccessibleDocuments(String email) {
    AppUser user = findUser(email);

    return documentRepository.findAccessibleDocuments(user.getId())
            .stream()
            .filter(document -> {
                try {
                    requireReadAccess(document.getId(), email);
                    return true;
                } catch (AccessDeniedException exception) {
                    return false;
                }
            })
            .collect(Collectors.toList());
}


public List<Document> searchAccessibleDocuments(
        String email,
        String title,
        String category,
        String ownerEmail,
        Instant fromDate,
        Instant toDate) {

    if (fromDate != null && toDate != null
            && fromDate.isAfter(toDate)) {
        throw new IllegalArgumentException(
                "fromDate must not be after toDate");
    }

    AppUser user = findUser(email);

    return documentRepository.searchAccessibleDocuments(
                    user.getId(),
                    normalizeFilter(title),
                    normalizeFilter(category),
                    normalizeEmailFilter(ownerEmail),
                    fromDate,
                    toDate)
            .stream()
            .filter(document -> {
                try {
                    requireReadAccess(document.getId(), email);
                    return true;
                } catch (AccessDeniedException exception) {
                    return false;
                }
            })
            .collect(Collectors.toList());
}

private String normalizeFilter(String value) {
    if (value == null || value.isBlank()) {
        return null;
    }
    return value.trim();
}

private String normalizeEmailFilter(String value) {
    if (value == null || value.isBlank()) {
        return null;
    }
    return value.trim().toLowerCase(java.util.Locale.ROOT);
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
