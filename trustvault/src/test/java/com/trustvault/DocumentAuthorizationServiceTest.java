
package com.trustvault;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentAuthorizationServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentPermissionRepository permissionRepository;

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private AppUser owner;

    @Mock
    private AppUser viewer;

    @Mock
    private Document document;

    @Mock
    private DocumentPermission permission;

    @InjectMocks
    private DocumentAuthorizationService authorizationService;

    @BeforeEach
void setUp() {
    when(owner.getId()).thenReturn(1L);
}
    private void setUpViewerDocument() {
        when(viewer.getId()).thenReturn(2L);
        when(userRepository.findByEmail("viewer@example.com"))
                .thenReturn(Optional.of(viewer));
        when(documentRepository.findById(10L))
                .thenReturn(Optional.of(document));
        when(document.getOwner()).thenReturn(owner);
    }

    @Test
    void ownerCanReadOwnDocument() {
        when(userRepository.findByEmail("owner@example.com"))
                .thenReturn(Optional.of(owner));
        when(documentRepository.findById(10L))
                .thenReturn(Optional.of(document));
        when(document.getOwner()).thenReturn(owner);

        Document result = authorizationService.requireReadAccess(
                10L, "owner@example.com");

        assertSame(document, result);
    }

    @Test
    void readPermissionAllowsReading() {
        setUpViewerDocument();

        when(permissionRepository.findByDocument_IdAndUser_Id(10L, 2L))
                .thenReturn(Optional.of(permission));
        when(permission.getPermissionLevel())
                .thenReturn(DocumentPermission.PermissionLevel.READ);

        Document result = authorizationService.requireReadAccess(
                10L, "viewer@example.com");

        assertSame(document, result);
    }

    @Test
    void readPermissionDoesNotAllowEditing() {
        setUpViewerDocument();

        when(permissionRepository.findByDocument_IdAndUser_Id(10L, 2L))
                .thenReturn(Optional.of(permission));
        when(permission.getPermissionLevel())
                .thenReturn(DocumentPermission.PermissionLevel.READ);

        assertThrows(
                AccessDeniedException.class,
                () -> authorizationService.requireEditAccess(
                        10L, "viewer@example.com"));
    }

    @Test
    void editPermissionAllowsEditing() {
        setUpViewerDocument();

        when(permissionRepository.findByDocument_IdAndUser_Id(10L, 2L))
                .thenReturn(Optional.of(permission));
        when(permission.getPermissionLevel())
                .thenReturn(DocumentPermission.PermissionLevel.EDIT);

        Document result = authorizationService.requireEditAccess(
                10L, "viewer@example.com");

        assertSame(document, result);
    }

    @Test
    void missingPermissionDeniesReading() {
        setUpViewerDocument();

        when(permissionRepository.findByDocument_IdAndUser_Id(10L, 2L))
                .thenReturn(Optional.empty());

        assertThrows(
                AccessDeniedException.class,
                () -> authorizationService.requireReadAccess(
                        10L, "viewer@example.com"));
    }

    @Test
    void emailIsNormalizedBeforeLookup() {
        when(userRepository.findByEmail("owner@example.com"))
                .thenReturn(Optional.of(owner));
        when(documentRepository.findById(10L))
                .thenReturn(Optional.of(document));
        when(document.getOwner()).thenReturn(owner);

        Document result = authorizationService.requireReadAccess(
                10L, " OWNER@EXAMPLE.COM ");

        assertSame(document, result);
    }
}
