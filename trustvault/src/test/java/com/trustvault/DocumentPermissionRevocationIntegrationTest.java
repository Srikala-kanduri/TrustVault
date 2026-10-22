
package com.trustvault;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
class DocumentPermissionRevocationIntegrationTest {

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private DocumentPermissionRepository permissionRepository;

    @Autowired
    private DocumentPermissionService permissionService;

    @Autowired
    private DocumentAuthorizationService authorizationService;

    @Autowired
private AuditLogRepository auditLogRepository;

    @Test
    @Transactional
    void revokedViewerCannotReadDocument() {
        AppUser owner = userRepository.save(
                new AppUser(
                        "revocation-owner@example.com",
                        "test-password",
                        UserRole.OWNER));

        AppUser viewer = userRepository.save(
                new AppUser(
                        "revocation-viewer@example.com",
                        "test-password",
                        UserRole.VIEWER));

        Document document = documentRepository.save(
                new Document(
                        "Revocation Test Document",
                        "revocation-test.pdf",
                        "Testing",
                        "12345678-1234-1234-1234-123456789012",
                        owner));

        DocumentPermission permission = permissionRepository.save(
                new DocumentPermission(
                        document,
                        viewer,
                        DocumentPermission.PermissionLevel.READ));

        Long documentId = document.getId();
        Long viewerId = viewer.getId();

        // Confirm the viewer initially has read access.
        assertTrue(
                permissionRepository.existsByDocument_IdAndUser_Id(
                        documentId, viewerId));

        authorizationService.requireReadAccess(
                documentId, "revocation-viewer@example.com");

        // The owner revokes the viewer's permission.
        permissionService.revokePermission(
                documentId,
                "revocation-owner@example.com",
                "revocation-viewer@example.com");
        
        assertTrue(
        auditLogRepository
                .findByDocumentIdOrderByOccurredAtDesc(documentId)
                .stream()
                .anyMatch(log ->
                        "PERMISSION_REVOKE".equals(log.getAction())
                        && "SUCCESS".equals(log.getOutcome())
                        && "revocation-owner@example.com"
                                .equals(log.getActorEmail())));
        
        // Confirm the permission has been removed.
        assertTrue(
                !permissionRepository.existsByDocument_IdAndUser_Id(
                        documentId, viewerId));

        // The viewer's next access check must be denied.
        assertThrows(
                AccessDeniedException.class,
                () -> authorizationService.requireReadAccess(
                        documentId,
                        "revocation-viewer@example.com"));
    }
}
