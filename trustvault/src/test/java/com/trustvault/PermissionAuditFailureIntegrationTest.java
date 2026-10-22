
package com.trustvault;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@ActiveProfiles("test")
class PermissionAuditFailureIntegrationTest {

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private DocumentPermissionRepository permissionRepository;

    @Autowired
    private DocumentPermissionService permissionService;

    @MockitoBean
    private AuditService auditService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void failedAuditWriteRollsBackPermissionGrant() {
        AppUser owner = userRepository.save(new AppUser(
                "audit-failure-owner@example.com",
                "test-password",
                UserRole.OWNER));

        AppUser viewer = userRepository.save(new AppUser(
                "audit-failure-viewer@example.com",
                "test-password",
                UserRole.VIEWER));

        Document document = documentRepository.save(new Document(
                "Audit Failure Test",
                "audit-failure-test.pdf",
                "Testing",
                "22345678-1234-1234-1234-123456789012",
                owner));

        Long documentId = document.getId();
        Long viewerId = viewer.getId();

        doThrow(new RuntimeException("Simulated audit database failure"))
                .when(auditService)
                .record(
                        "audit-failure-owner@example.com",
                        "PERMISSION_GRANT",
                        documentId,
                        "Permission READ granted or updated for audit-failure-viewer@example.com",
                        "SUCCESS");

        TransactionTemplate transactionTemplate =
                new TransactionTemplate(transactionManager);

        assertThrows(RuntimeException.class, () ->
                transactionTemplate.executeWithoutResult(status ->
                        permissionService.grantPermission(
                                documentId,
                                "audit-failure-owner@example.com",
                                "audit-failure-viewer@example.com",
                                DocumentPermission.PermissionLevel.READ)));

        assertFalse(permissionRepository
                .existsByDocument_IdAndUser_Id(documentId, viewerId));
    }
    
    @Test
    void failedAuditWriteRollsBackPermissionRevocation() {
        AppUser owner = userRepository.save(new AppUser(
                "revoke-audit-owner@example.com",
                "test-password",
                UserRole.OWNER));

        AppUser viewer = userRepository.save(new AppUser(
                "revoke-audit-viewer@example.com",
                "test-password",
                UserRole.VIEWER));

        Document document = documentRepository.save(new Document(
                "Revocation Audit Failure Test",
                "revoke-audit-test.pdf",
                "Testing",
                "32345678-1234-1234-1234-123456789012",
                owner));

        DocumentPermission permission = permissionRepository.save(
                new DocumentPermission(
                        document,
                        viewer,
                        DocumentPermission.PermissionLevel.READ));

        Long documentId = document.getId();
        Long viewerId = viewer.getId();

        doThrow(new RuntimeException("Simulated audit database failure"))
                .when(auditService)
                .record(
                        "revoke-audit-owner@example.com",
                        "PERMISSION_REVOKE",
                        documentId,
                        "Permission revoked for revoke-audit-viewer@example.com",
                        "SUCCESS");

        TransactionTemplate transactionTemplate =
                new TransactionTemplate(transactionManager);

        assertThrows(RuntimeException.class, () ->
                transactionTemplate.executeWithoutResult(status ->
                        permissionService.revokePermission(
                                documentId,
                                "revoke-audit-owner@example.com",
                                "revoke-audit-viewer@example.com")));

        // The permission should remain because revocation rolled back.
        org.junit.jupiter.api.Assertions.assertTrue(
                permissionRepository.existsByDocument_IdAndUser_Id(
                        documentId, viewerId));
    }

}
