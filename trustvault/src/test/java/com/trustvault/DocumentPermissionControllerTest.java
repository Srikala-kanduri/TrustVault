
package com.trustvault;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class DocumentPermissionControllerTest {

    @Mock
    private DocumentPermissionService permissionService;

    @InjectMocks
    private DocumentPermissionController controller;

    @Test
    void grantPermissionDelegatesToService() {
        Document document = org.mockito.Mockito.mock(Document.class);
        AppUser target = org.mockito.Mockito.mock(AppUser.class);
        DocumentPermission permission =
                org.mockito.Mockito.mock(DocumentPermission.class);

        when(permissionService.grantPermission(
                10L,
                "owner@example.com",
                "viewer@example.com",
                DocumentPermission.PermissionLevel.READ))
                .thenReturn(permission);

        when(permission.getPermissionLevel())
                .thenReturn(DocumentPermission.PermissionLevel.READ);

        var authentication = new TestingAuthenticationToken(
                "owner@example.com", "password");

        var response = controller.grantPermission(
                10L,
                new GrantPermissionRequest(
                        "viewer@example.com",
                        DocumentPermission.PermissionLevel.READ),
                authentication);

        assertEquals(10L, response.documentId());
        assertEquals("viewer@example.com", response.email());
        assertSame(
                DocumentPermission.PermissionLevel.READ,
                response.permissionLevel());

        verify(permissionService).grantPermission(
                10L,
                "owner@example.com",
                "viewer@example.com",
                DocumentPermission.PermissionLevel.READ);
    }

    @Test
    void revokePermissionDelegatesToService() {
        var authentication = new TestingAuthenticationToken(
                "owner@example.com", "password");

        controller.revokePermission(
                10L, "viewer@example.com", authentication);

        verify(permissionService).revokePermission(
                10L, "owner@example.com", "viewer@example.com");
    }
}
