
package com.trustvault;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DocumentAuthorizationService {

    private final DocumentRepository documentRepository;
    private final DocumentPermissionRepository permissionRepository;
    private final AppUserRepository userRepository;
    private final EntityManager entityManager;

    public DocumentAuthorizationService(
            DocumentRepository documentRepository,
            DocumentPermissionRepository permissionRepository,
            AppUserRepository userRepository,
            EntityManager entityManager) {
        this.documentRepository = documentRepository;
        this.permissionRepository = permissionRepository;
        this.userRepository = userRepository;
        this.entityManager = entityManager;
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

    StringBuilder jpql = new StringBuilder("""
            SELECT DISTINCT d
            FROM Document d
            WHERE (d.owner.id = :userId
               OR EXISTS (
                   SELECT p.id
                   FROM DocumentPermission p
                   WHERE p.document = d
                     AND p.user.id = :userId
                     AND p.permissionLevel IN (
                         com.trustvault.DocumentPermission.PermissionLevel.READ,
                         com.trustvault.DocumentPermission.PermissionLevel.EDIT
                     )
               ))
            """);

    String normalizedTitle = normalizeFilter(title);
    String normalizedCategory = normalizeFilter(category);
    String normalizedOwnerEmail = normalizeEmailFilter(ownerEmail);

    if (normalizedTitle != null) {
        jpql.append(" AND LOWER(d.title) LIKE :title");
    }
    if (normalizedCategory != null) {
        jpql.append(" AND LOWER(d.category) = :category");
    }
    if (normalizedOwnerEmail != null) {
        jpql.append(" AND LOWER(d.owner.email) = :ownerEmail");
    }
    if (fromDate != null) {
        jpql.append(" AND d.createdAt >= :fromDate");
    }
    if (toDate != null) {
        jpql.append(" AND d.createdAt <= :toDate");
    }
    jpql.append(" ORDER BY d.createdAt DESC");

    TypedQuery<Document> query = entityManager.createQuery(
            jpql.toString(), Document.class);
    query.setParameter("userId", user.getId());
    if (normalizedTitle != null) {
        query.setParameter("title", "%" + normalizedTitle.toLowerCase(Locale.ROOT) + "%");
    }
    if (normalizedCategory != null) {
        query.setParameter("category", normalizedCategory.toLowerCase(Locale.ROOT));
    }
    if (normalizedOwnerEmail != null) {
        query.setParameter("ownerEmail", normalizedOwnerEmail);
    }
    if (fromDate != null) {
        query.setParameter("fromDate", fromDate);
    }
    if (toDate != null) {
        query.setParameter("toDate", toDate);
    }

    return query.getResultList()
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
    if (email == null || email.isBlank()) {
        throw new AccessDeniedException("Document access denied");
    }

    return userRepository.findByEmail(
                    email.trim().toLowerCase(Locale.ROOT))
            .orElseThrow(() ->
                    new AccessDeniedException("Document access denied"));
}

    private Document findDocument(Long documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() ->
                        new AccessDeniedException("Document access denied"));
    }
}
