
package com.trustvault;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuditControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void unauthenticatedUserCannotAccessAuditLogs() throws Exception {
        mockMvc.perform(
                get("/api/admin/audit-logs")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminCanAccessAuditLogs() throws Exception {
        mockMvc.perform(
                get("/api/admin/audit-logs")
                        .with(user("admin@example.com").roles("ADMIN"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void ownerCannotAccessAuditLogs() throws Exception {
        mockMvc.perform(
                get("/api/admin/audit-logs")
                        .with(user("owner@example.com").roles("OWNER"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void viewerCannotAccessAuditLogs() throws Exception {
        mockMvc.perform(
                get("/api/admin/audit-logs")
                        .with(user("viewer@example.com").roles("VIEWER"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }
}
