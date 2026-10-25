
package com.trustvault;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findByOwner_IdOrderByCreatedAtDesc(Long ownerId);

    @Query("""
        SELECT DISTINCT d
        FROM Document d
        WHERE d.owner.id = :userId
           OR EXISTS (
               SELECT p.id
               FROM DocumentPermission p
               WHERE p.document = d
                 AND p.user.id = :userId
                 AND p.permissionLevel IN (
                     com.trustvault.DocumentPermission.PermissionLevel.READ,
                     com.trustvault.DocumentPermission.PermissionLevel.EDIT
                 )
           )
        ORDER BY d.createdAt DESC
        """)
    List<Document> findAccessibleDocuments(
            @Param("userId") Long userId);

    @Query("""
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
          AND (:title IS NULL OR LOWER(d.title) LIKE LOWER(CONCAT('%', :title, '%')))
          AND (:category IS NULL OR LOWER(d.category) = LOWER(:category))
          AND (:ownerEmail IS NULL OR LOWER(d.owner.email) = LOWER(:ownerEmail))
          AND (:fromDate IS NULL OR d.createdAt >= :fromDate)
          AND (:toDate IS NULL OR d.createdAt <= :toDate)
        ORDER BY d.createdAt DESC
        """)
    List<Document> searchAccessibleDocuments(
            @Param("userId") Long userId,
            @Param("title") String title,
            @Param("category") String category,
            @Param("ownerEmail") String ownerEmail,
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate);

}
