
package com.trustvault;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/api/admin/audit-logs")
public class AuditController {

    private final AuditLogRepository auditLogRepository;

    public AuditController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping
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

        return auditLogRepository.findAllByOrderByOccurredAtDesc()
                .stream()
                .map(AuditLogResponse::from)
                .toList();
    }
}
