
package com.trustvault;

public record AccessGrantResponse(
        Long documentId,
        String documentTitle,
        String recipientEmail,
        DocumentPermission.PermissionLevel permissionLevel) {
}
