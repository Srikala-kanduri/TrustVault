
package com.trustvault;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.Optional;
import java.time.Instant;

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
        when(owner.getId()).thenReturn(1L);
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


@Test
void ownerCanListOwnDocuments() {
    when(owner.getId()).thenReturn(1L);
    when(userRepository.findByEmail("owner@example.com"))
            .thenReturn(Optional.of(owner));
    when(documentRepository.findAccessibleDocuments(1L))
            .thenReturn(List.of(document));
    when(documentRepository.findById(10L))
            .thenReturn(Optional.of(document));
    when(document.getId()).thenReturn(10L);
    when(document.getOwner()).thenReturn(owner);

    List<Document> result =
            authorizationService.listAccessibleDocuments(
                    "owner@example.com");

    assertEquals(1, result.size());
    assertSame(document, result.get(0));
}

@Test
void viewerCanListDocumentWithReadPermission() {
    setUpViewerDocument();

    when(documentRepository.findAccessibleDocuments(2L))
            .thenReturn(List.of(document));
    when(permissionRepository.findByDocument_IdAndUser_Id(10L, 2L))
            .thenReturn(Optional.of(permission));
    when(document.getId()).thenReturn(10L);
    when(permission.getPermissionLevel())
            .thenReturn(DocumentPermission.PermissionLevel.READ);

    List<Document> result =
            authorizationService.listAccessibleDocuments(
                    "viewer@example.com");

    assertEquals(1, result.size());
    assertSame(document, result.get(0));
}

@Test
void viewerCannotListDocumentWithoutPermission() {
    setUpViewerDocument();

    when(documentRepository.findAccessibleDocuments(2L))
            .thenReturn(List.of(document));
    when(permissionRepository.findByDocument_IdAndUser_Id(10L, 2L))
            .thenReturn(Optional.empty());
    when(document.getId()).thenReturn(10L);

    List<Document> result =
            authorizationService.listAccessibleDocuments(
                    "viewer@example.com");

    assertEquals(0, result.size());
}


@Test
void searchReturnsDocumentWhenUserHasReadPermission() {
    setUpViewerDocument();

    when(documentRepository.searchAccessibleDocuments(
            2L, "report", "Finance", null, null, null))
            .thenReturn(List.of(document));
    when(document.getId()).thenReturn(10L);
    when(permissionRepository.findByDocument_IdAndUser_Id(10L, 2L))
            .thenReturn(Optional.of(permission));
    when(permission.getPermissionLevel())
            .thenReturn(DocumentPermission.PermissionLevel.READ);

    List<Document> result =
            authorizationService.searchAccessibleDocuments(
                    "viewer@example.com",
                    " report ",
                    "Finance",
                    null,
                    null,
                    null);

    assertEquals(1, result.size());
    assertSame(document, result.get(0));
}

@Test
void searchExcludesDocumentWithoutPermission() {
    setUpViewerDocument();

    when(documentRepository.searchAccessibleDocuments(
            2L, null, null, null, null, null))
            .thenReturn(List.of(document));
    when(document.getId()).thenReturn(10L);
    when(permissionRepository.findByDocument_IdAndUser_Id(10L, 2L))
            .thenReturn(Optional.empty());

    List<Document> result =
            authorizationService.searchAccessibleDocuments(
                    "viewer@example.com",
                    null,
                    null,
                    null,
                    null,
                    null);

    assertEquals(0, result.size());
}

@Test
void searchRejectsInvalidDateRange() {
    assertThrows(
            IllegalArgumentException.class,
            () -> authorizationService.searchAccessibleDocuments(
                    "viewer@example.com",
                    null,
                    null,
                    null,
                    Instant.parse("2026-10-22T00:00:00Z"),
                    Instant.parse("2026-10-21T00:00:00Z")));
}

}
