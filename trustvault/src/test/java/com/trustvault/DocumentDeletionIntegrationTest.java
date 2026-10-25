
package com.trustvault;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DocumentDeletionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
private DocumentDeletionService deletionService;

    @Autowired
private AppUserRepository userRepository;

@Autowired
private DocumentRepository documentRepository;

@Autowired
private DocumentFileStorageService storageService;

@Autowired
private PlatformTransactionManager transactionManager;

@Value("${trustvault.storage.directory}")
private String storageDirectory;

@MockitoBean
private AuditService auditService;

    @Test
    void unauthenticatedUserCannotDeleteDocument() throws Exception {
        mockMvc.perform(delete("/api/documents/1").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedNonOwnerCannotDeleteDocument() throws Exception {
        mockMvc.perform(
                delete("/api/documents/1")
                        .with(user("viewer@example.com").roles("VIEWER")))
                .andExpect(status().isForbidden());
    }
    @Test
void failedDeletionAuditRollsBackDatabaseAndRestoresFile()
        throws Exception {

    AppUser owner = userRepository.save(new AppUser(
            "rollback-owner@example.com",
            "test-password",
            UserRole.OWNER));

    String storageKey = UUID.randomUUID().toString();
    Path directory = Path.of(storageDirectory)
            .toAbsolutePath()
            .normalize();
    Path originalFile = directory.resolve(storageKey);

    Files.createDirectories(directory);
    Files.writeString(originalFile, "rollback-test-content");

    Document document = documentRepository.saveAndFlush(
            new Document(
                    "Rollback Test",
                    "rollback-test.pdf",
                    "Testing",
                    storageKey,
                    owner));

    Long documentId = document.getId();

    doThrow(new RuntimeException("Simulated audit failure"))
            .when(auditService)
            .recordInCurrentTransaction(
                    "rollback-owner@example.com",
                    "DOCUMENT_DELETE",
                    documentId,
                    "Document deletion committed for Rollback Test",
                    "SUCCESS");

    TransactionTemplate transactionTemplate =
            new TransactionTemplate(transactionManager);

    assertThrows(
        RuntimeException.class,
        () -> transactionTemplate.executeWithoutResult(status -> {
            try {
                deletionService.deleteDocument(
                        documentId,
                        "rollback-owner@example.com");
            } catch (java.io.IOException exception) {
                throw new RuntimeException(exception);
            }
        }));

    assertTrue(
            documentRepository.existsById(documentId),
            "Document database record should remain after rollback");

    assertTrue(
            Files.exists(originalFile),
            "Original file should be restored after rollback");

    assertTrue(
            Files.readString(originalFile).equals("rollback-test-content"),
            "Restored file should retain its original content");
}
}
