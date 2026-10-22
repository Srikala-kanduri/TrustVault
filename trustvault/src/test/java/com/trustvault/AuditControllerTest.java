
package com.trustvault;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class AuditControllerTest {

    private void setAuthorities(
            Authentication authentication, String role) {

        doReturn(true).when(authentication).isAuthenticated();

        doReturn(List.of(new SimpleGrantedAuthority(role)))
                .when(authentication).getAuthorities();
    }

    @Test
    void adminCanRetrieveAuditLogs() {
        AuditLogRepository repository = mock(AuditLogRepository.class);
        AuditController controller = new AuditController(repository);
        Authentication authentication = mock(Authentication.class);

        setAuthorities(authentication, "ROLE_ADMIN");

        when(repository.findAllByOrderByOccurredAtDesc())
                .thenReturn(List.of());

        List<AuditLogResponse> result =
                controller.getAuditLogs(authentication);

        assertEquals(0, result.size());
        verify(repository).findAllByOrderByOccurredAtDesc();
    }

    @Test
    void ordinaryUserCannotRetrieveAuditLogs() {
        AuditLogRepository repository = mock(AuditLogRepository.class);
        AuditController controller = new AuditController(repository);
        Authentication authentication = mock(Authentication.class);

        setAuthorities(authentication, "ROLE_OWNER");

        assertThrows(
                AccessDeniedException.class,
                () -> controller.getAuditLogs(authentication));

        verify(repository, never())
                .findAllByOrderByOccurredAtDesc();
    }
}
