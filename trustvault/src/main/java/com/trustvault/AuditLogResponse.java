
package com.trustvault;

import java.time.Instant;

public record AuditLogResponse(
        Long id,
        String actorEmail,
        String action,
        Long documentId,
        String details,
        String outcome,
        Instant occurredAt) {

    public static AuditLogResponse from(AuditLog auditLog) {
        return new AuditLogResponse(
                auditLog.getId(),
                auditLog.getActorEmail(),
                auditLog.getAction(),
                auditLog.getDocumentId(),
                auditLog.getDetails(),
                auditLog.getOutcome(),
                auditLog.getOccurredAt());
    }
}
