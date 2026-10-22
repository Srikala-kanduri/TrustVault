
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
class DocumentDownloadIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void unauthenticatedUserCannotDownloadDocument() throws Exception {
        mockMvc.perform(
                get("/api/documents/1/download")
                        .accept(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedUserWithoutPermissionCannotDownload()
            throws Exception {

        mockMvc.perform(
                get("/api/documents/1/download")
                        .with(user("viewer@example.com")
                                .roles("VIEWER"))
                        .accept(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(status().isForbidden());
    }
}
