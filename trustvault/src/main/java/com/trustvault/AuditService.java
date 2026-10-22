
package com.trustvault;

import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(
            String actorEmail,
            String action,
            Long documentId,
            String details,
            String outcome) {

        saveAuditLog(actorEmail, action, documentId, details, outcome);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(
            String actorEmail,
            String action,
            Long documentId,
            String details) {

        saveAuditLog(actorEmail, action, documentId, details, "SUCCESS");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordDenied(
            String actorEmail,
            String action,
            Long documentId,
            String details) {

        saveAuditLog(actorEmail, action, documentId, details, "DENIED");
    }

    private void saveAuditLog(
            String actorEmail,
            String action,
            Long documentId,
            String details,
            String outcome) {

        AuditLog auditLog = new AuditLog(
                normalizeActor(actorEmail),
                action,
                documentId,
                details,
                outcome);

        auditLogRepository.save(auditLog);
    }

    private String normalizeActor(String actorEmail) {
        if (actorEmail == null || actorEmail.isBlank()) {
            return "unknown";
        }

        return actorEmail.trim().toLowerCase(Locale.ROOT);
    }
}
