
package com.trustvault;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DocumentRepositoryTest {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private DocumentPermissionRepository permissionRepository;

    private AppUser owner;
    private AppUser viewer;
    private AppUser outsider;
    private Document document;

    @BeforeEach
    void setUp() {
        permissionRepository.deleteAll();
        documentRepository.deleteAll();
        userRepository.deleteAll();

        owner = userRepository.save(new AppUser(
                "repo-owner@example.com",
                "test-password",
                UserRole.OWNER));

        viewer = userRepository.save(new AppUser(
                "repo-viewer@example.com",
                "test-password",
                UserRole.VIEWER));

        outsider = userRepository.save(new AppUser(
                "repo-outsider@example.com",
                "test-password",
                UserRole.VIEWER));

        document = documentRepository.save(new Document(
                "Finance Report",
                "finance-report.pdf",
                "Finance",
                "42345678-1234-1234-1234-123456789012",
                owner));
    }

    @Test
    void ownerCanListOwnDocument() {
        List<Document> results =
                documentRepository.findAccessibleDocuments(owner.getId());

        assertEquals(1, results.size());
        assertEquals(document.getId(), results.get(0).getId());
    }

    @Test
    void viewerWithReadPermissionCanListAndSearchDocument() {
        permissionRepository.save(new DocumentPermission(
                document,
                viewer,
                DocumentPermission.PermissionLevel.READ));

        List<Document> listed =
                documentRepository.findAccessibleDocuments(viewer.getId());

        List<Document> searched =
                documentRepository.searchAccessibleDocuments(
                        viewer.getId(),
                        "Finance",
                        "Finance",
                        null,
                        null,
                        null);

        assertEquals(1, listed.size());
        assertEquals(document.getId(), listed.get(0).getId());

        assertEquals(1, searched.size());
        assertEquals(document.getId(), searched.get(0).getId());
    }

    @Test
    void userWithoutPermissionCannotListOrSearchDocument() {
        List<Document> listed =
                documentRepository.findAccessibleDocuments(outsider.getId());

        List<Document> searched =
                documentRepository.searchAccessibleDocuments(
                        outsider.getId(),
                        null,
                        null,
                        null,
                        null,
                        null);

        assertTrue(listed.isEmpty());
        assertTrue(searched.isEmpty());
    }

    @Test
    void revokedPermissionRemovesDocumentFromListAndSearch() {
        DocumentPermission grant = permissionRepository.save(
                new DocumentPermission(
                        document,
                        viewer,
                        DocumentPermission.PermissionLevel.READ));

        assertEquals(
                1,
                documentRepository.findAccessibleDocuments(viewer.getId())
                        .size());

        permissionRepository.delete(grant);
        permissionRepository.flush();

        List<Document> listed =
                documentRepository.findAccessibleDocuments(viewer.getId());

        List<Document> searched =
                documentRepository.searchAccessibleDocuments(
                        viewer.getId(),
                        null,
                        null,
                        null,
                        null,
                        null);

        assertTrue(listed.isEmpty());
        assertTrue(searched.isEmpty());
    }
}
