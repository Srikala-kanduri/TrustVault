
package com.trustvault;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(
        name = "audit_logs",
        indexes = {
                @Index(name = "idx_audit_actor", columnList = "actor_email"),
                @Index(name = "idx_audit_document", columnList = "document_id"),
                @Index(name = "idx_audit_time", columnList = "occurred_at")
        }
)
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actor_email", nullable = false, length = 254)
    private String actorEmail;

    @Column(name = "action", nullable = false, length = 50)
    private String action;

    @Column(name = "document_id")
    private Long documentId;

    @Column(name = "details", length = 1000)
    private String details;

    @Column(name = "outcome", nullable = false, length = 20)
    private String outcome;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected AuditLog() {
    }

    public AuditLog(
            String actorEmail,
            String action,
            Long documentId,
            String details,
            String outcome) {
        this.actorEmail = actorEmail;
        this.action = action;
        this.documentId = documentId;
        this.details = details;
        this.outcome = outcome;
        this.occurredAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getActorEmail() {
        return actorEmail;
    }

    public String getAction() {
        return action;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public String getDetails() {
        return details;
    }

    public String getOutcome() {
        return outcome;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
