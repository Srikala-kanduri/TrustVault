
package com.trustvault;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByActorEmailOrderByOccurredAtDesc(String actorEmail);

    List<AuditLog> findByDocumentIdOrderByOccurredAtDesc(Long documentId);

    List<AuditLog> findAllByOrderByOccurredAtDesc();
}
