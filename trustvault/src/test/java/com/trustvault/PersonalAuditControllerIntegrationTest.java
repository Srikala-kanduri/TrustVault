package com.trustvault;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PersonalAuditControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuditService auditService;

    @Test
    void ownerCanReadOnlyTheirOwnAuditEvents() throws Exception {
        String owner = "owner-" + UUID.randomUUID() + "@example.com";
        String viewer = "viewer-" + UUID.randomUUID() + "@example.com";
        auditService.recordSuccess(owner, "DOCUMENT_UPLOAD", 101L, "owner event");
        auditService.recordSuccess(viewer, "DOCUMENT_READ", 101L, "viewer event");

        mockMvc.perform(get("/api/audit-logs")
                        .with(user(owner).roles("OWNER"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].actorEmail").value(owner))
                .andExpect(jsonPath("$[0].details").value("owner event"));
    }

    @Test
    void viewerCanReadOnlyTheirOwnAuditEvents() throws Exception {
        String viewer = "viewer-" + UUID.randomUUID() + "@example.com";
        String owner = "owner-" + UUID.randomUUID() + "@example.com";
        auditService.recordSuccess(viewer, "DOCUMENT_READ", 102L, "viewer event");
        auditService.recordSuccess(owner, "DOCUMENT_UPLOAD", 102L, "owner event");

        mockMvc.perform(get("/api/audit-logs")
                        .with(user(viewer).roles("VIEWER"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].actorEmail").value(viewer))
                .andExpect(jsonPath("$[0].details").value("viewer event"));
    }

    @Test
    void unauthenticatedUserCannotReadPersonalAuditEvents() throws Exception {
        mockMvc.perform(get("/api/audit-logs")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}
