
package com.trustvault;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocumentControllerTest {

    @Test
    void getDocumentChecksReadAccessBeforeReturningMetadata() {
        DocumentAuthorizationService authorizationService =
                mock(DocumentAuthorizationService.class);

        Authentication authentication = mock(Authentication.class);
        Document document = mock(Document.class);
        AppUser owner = mock(AppUser.class);

        DocumentController controller =
                new DocumentController(authorizationService);

        when(authentication.getName()).thenReturn("owner@example.com");
        when(authorizationService.requireReadAccess(
                10L, "owner@example.com")).thenReturn(document);

        when(document.getId()).thenReturn(10L);
        when(document.getTitle()).thenReturn("Contract");
        when(document.getOriginalFilename()).thenReturn("contract.pdf");
        when(document.getCategory()).thenReturn("Legal");
        when(document.getOwner()).thenReturn(owner);
        when(owner.getEmail()).thenReturn("owner@example.com");

        DocumentResponse response =
                controller.getDocument(10L, authentication);

        assertEquals(10L, response.id());
        assertEquals("Contract", response.title());
        assertEquals("contract.pdf", response.originalFilename());

        verify(authorizationService)
                .requireReadAccess(10L, "owner@example.com");
    }

    @Test
    void deniedAccessDoesNotReturnMetadata() {
        DocumentAuthorizationService authorizationService =
                mock(DocumentAuthorizationService.class);

        Authentication authentication = mock(Authentication.class);

        DocumentController controller =
                new DocumentController(authorizationService);

        when(authentication.getName()).thenReturn("viewer@example.com");

        when(authorizationService.requireReadAccess(
                10L, "viewer@example.com"))
                .thenThrow(new AccessDeniedException("Document access denied"));

        assertThrows(
                AccessDeniedException.class,
                () -> controller.getDocument(10L, authentication));
    }
}
