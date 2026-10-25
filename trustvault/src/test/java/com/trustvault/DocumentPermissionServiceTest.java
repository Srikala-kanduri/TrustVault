
package com.trustvault;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;
import static org.mockito.Mockito.never;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentPermissionServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentPermissionRepository permissionRepository;

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private AppUser owner;

    @Mock
    private AppUser target;

    @Mock
    private Document document;

    @Mock
    private DocumentPermission existingPermission;

    @InjectMocks
    private DocumentPermissionService permissionService;

    private void setUpOwnerAndTarget() {
        when(userRepository.findByEmail("owner@example.com"))
                .thenReturn(Optional.of(owner));
        when(userRepository.findByEmail("target@example.com"))
                .thenReturn(Optional.of(target));
        when(documentRepository.findById(10L))
                .thenReturn(Optional.of(document));
        when(document.getOwner()).thenReturn(owner);
        when(owner.getId()).thenReturn(1L);
        when(target.getId()).thenReturn(2L);
    }

    @Test
    void grantPermissionCreatesNewGrant() {
        setUpOwnerAndTarget();

        when(permissionRepository.findByDocument_IdAndUser_Id(10L, 2L))
                .thenReturn(Optional.empty());

        when(permissionRepository.save(any(DocumentPermission.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DocumentPermission result = permissionService.grantPermission(
                10L,
                "owner@example.com",
                "target@example.com",
                DocumentPermission.PermissionLevel.READ);

        assertSame(
                DocumentPermission.PermissionLevel.READ,
                result.getPermissionLevel());

        verify(permissionRepository).save(result);
        verify(auditService).recordInCurrentTransaction(
                "owner@example.com",
                "PERMISSION_GRANT",
                10L,
                "Permission READ granted to target@example.com",
                "SUCCESS");
    }

    @Test
    void grantPermissionUpdatesExistingGrant() {
        setUpOwnerAndTarget();

        when(permissionRepository.findByDocument_IdAndUser_Id(10L, 2L))
                .thenReturn(Optional.of(existingPermission));
        when(existingPermission.getPermissionLevel())
                .thenReturn(DocumentPermission.PermissionLevel.READ);

        DocumentPermission result = permissionService.grantPermission(
                10L,
                "owner@example.com",
                "target@example.com",
                DocumentPermission.PermissionLevel.EDIT);

        assertSame(existingPermission, result);

        verify(existingPermission).setPermissionLevel(
                DocumentPermission.PermissionLevel.EDIT);

        verify(auditService).recordInCurrentTransaction(
                "owner@example.com",
                "PERMISSION_GRANT",
                10L,
                "Permission for target@example.com changed from READ to EDIT",
                "SUCCESS");
    }

    
@Test
void revokePermissionDeletesExistingGrant() {
    setUpOwnerAndTarget();

    when(permissionRepository.findByDocument_IdAndUser_Id(10L, 2L))
            .thenReturn(Optional.of(existingPermission));
    when(existingPermission.getPermissionLevel())
            .thenReturn(DocumentPermission.PermissionLevel.READ);

    permissionService.revokePermission(
            10L, "owner@example.com", "target@example.com");

    verify(permissionRepository)
            .deleteByDocument_IdAndUser_Id(10L, 2L);

    verify(auditService).recordInCurrentTransaction(
            "owner@example.com",
            "PERMISSION_REVOKE",
            10L,
            "Permission READ revoked for target@example.com",
            "SUCCESS");
}


    @Test
    void nonOwnerCannotGrantPermission() {
        when(userRepository.findByEmail("target@example.com"))
                .thenReturn(Optional.of(target));
        when(documentRepository.findById(10L))
                .thenReturn(Optional.of(document));
        when(document.getOwner()).thenReturn(owner);
        when(owner.getId()).thenReturn(1L);
        when(target.getId()).thenReturn(2L);

        assertThrows(
                AccessDeniedException.class,
                () -> permissionService.grantPermission(
                        10L,
                        "target@example.com",
                        "owner@example.com",
                        DocumentPermission.PermissionLevel.READ));

        verify(auditService).record(
                "target@example.com",
                "PERMISSION_GRANT",
                10L,
                "Permission grant denied",
                "DENIED");
    }
    
@Test
void revokePermissionFailsWhenGrantDoesNotExist() {
    setUpOwnerAndTarget();

    when(permissionRepository.findByDocument_IdAndUser_Id(10L, 2L))
            .thenReturn(Optional.empty());

    assertThrows(
            IllegalArgumentException.class,
            () -> permissionService.revokePermission(
                    10L, "owner@example.com", "target@example.com"));

    verify(permissionRepository, never())
            .deleteByDocument_IdAndUser_Id(10L, 2L);

    verify(auditService, never()).recordInCurrentTransaction(
            "owner@example.com",
            "PERMISSION_REVOKE",
            10L,
            "Permission READ revoked for target@example.com",
            "SUCCESS");
}


    @Test
    void grantPermissionPropagatesAuditWriteFailure() {
        setUpOwnerAndTarget();

        when(permissionRepository.findByDocument_IdAndUser_Id(10L, 2L))
                .thenReturn(Optional.empty());

        when(permissionRepository.save(any(DocumentPermission.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        org.mockito.Mockito.doThrow(
                new RuntimeException("Audit database unavailable"))
                .when(auditService)
                .recordInCurrentTransaction(
                        "owner@example.com",
                        "PERMISSION_GRANT",
                        10L,
                        "Permission READ granted to target@example.com",
                        "SUCCESS");

        assertThrows(
                RuntimeException.class,
                () -> permissionService.grantPermission(
                        10L,
                        "owner@example.com",
                        "target@example.com",
                        DocumentPermission.PermissionLevel.READ));
    }

    @Test
    void auditFailureDoesNotHidePermissionDenial() {
        when(userRepository.findByEmail("target@example.com"))
                .thenReturn(Optional.of(target));
        when(documentRepository.findById(10L))
                .thenReturn(Optional.of(document));
        when(document.getOwner()).thenReturn(owner);
        when(owner.getId()).thenReturn(1L);
        when(target.getId()).thenReturn(2L);

        org.mockito.Mockito.doThrow(
                new RuntimeException("Audit database unavailable"))
                .when(auditService)
                .record(
                        "target@example.com",
                        "PERMISSION_GRANT",
                        10L,
                        "Permission grant denied",
                        "DENIED");

        assertThrows(
                AccessDeniedException.class,
                () -> permissionService.grantPermission(
                        10L,
                        "target@example.com",
                        "owner@example.com",
                        DocumentPermission.PermissionLevel.READ));
    }

}
