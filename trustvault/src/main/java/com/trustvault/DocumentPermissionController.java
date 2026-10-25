
package com.trustvault;

import java.util.Locale;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/documents/{documentId}/permissions")
@Validated
public class DocumentPermissionController {

    private final DocumentPermissionService permissionService;

    public DocumentPermissionController(
            DocumentPermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @PostMapping
    public PermissionResponse grantPermission(
            @PathVariable Long documentId,
            @Valid @RequestBody GrantPermissionRequest request,
            Authentication authentication) {

        DocumentPermission permission = permissionService.grantPermission(
                documentId,
                authentication.getName(),
                request.email(),
                request.permissionLevel());

        return new PermissionResponse(
                documentId,
                request.email().trim().toLowerCase(Locale.ROOT),
                permission.getPermissionLevel());
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokePermission(
            @PathVariable Long documentId,
            @RequestParam @NotBlank @Email String email,
            Authentication authentication) {

        permissionService.revokePermission(
                documentId,
                authentication.getName(),
                email);
    }

    public record PermissionResponse(
            Long documentId,
            String email,
            DocumentPermission.PermissionLevel permissionLevel) {
    }

    @GetMapping
public List<PermissionResponse> listPermissions(
        @PathVariable Long documentId,
        Authentication authentication) {

    return permissionService.listPermissions(
                    documentId,
                    authentication.getName())
            .stream()
            .map(permission -> new PermissionResponse(
                    documentId,
                    permission.getUser().getEmail(),
                    permission.getPermissionLevel()))
            .toList();
}
}
