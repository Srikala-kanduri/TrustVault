
package com.trustvault;

import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/api")
public class AuditController {

    private final AuditLogRepository auditLogRepository;
    private final AuditService auditService;

    public AuditController(
            AuditLogRepository auditLogRepository,
            AuditService auditService) {
        this.auditLogRepository = auditLogRepository;
        this.auditService = auditService;
    }

    @GetMapping("/admin/audit-logs")
    @ResponseBody
    public List<AuditLogResponse> getAuditLogs(
            Authentication authentication) {

        if (authentication == null
                || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication required");
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_ADMIN")
                        || authority.getAuthority().equals("ADMIN"));

        if (!isAdmin) {
            throw new AccessDeniedException(
                    "Administrator access required");
        }

        AuditService.IntegrityResult integrity = auditService.verifyIntegrity();
        if (!integrity.valid()) {
            String location = integrity.invalidRecordId() == null
                    ? "the audit chain head"
                    : "audit record " + integrity.invalidRecordId();
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Audit integrity verification failed at " + location);
        }

        return auditLogRepository.findAllByOrderByOccurredAtDesc()
                .stream()
                .map(AuditLogResponse::from)
                .toList();
    }

    @GetMapping("/audit-logs")
    @ResponseBody
    public List<AuditLogResponse> getPersonalAuditLogs(
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        AuditService.IntegrityResult integrity = auditService.verifyIntegrity();
        if (!integrity.valid()) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Audit integrity verification failed");
        }

        String actorEmail = authentication.getName().trim()
                .toLowerCase(Locale.ROOT);
        return auditLogRepository
                .findByActorEmailOrderByOccurredAtDesc(actorEmail)
                .stream()
                .map(AuditLogResponse::from)
                .toList();
    }
}
