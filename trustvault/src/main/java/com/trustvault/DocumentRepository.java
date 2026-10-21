
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
        LEFT JOIN DocumentPermission p ON p.document = d
        WHERE d.owner.id = :userId
           OR p.user.id = :userId
        ORDER BY d.createdAt DESC
        """)
    List<Document> findAccessibleDocuments(
            @Param("userId") Long userId);

    @Query("""
        SELECT DISTINCT d
        FROM Document d
        LEFT JOIN DocumentPermission p ON p.document = d
        WHERE (d.owner.id = :userId OR p.user.id = :userId)
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
