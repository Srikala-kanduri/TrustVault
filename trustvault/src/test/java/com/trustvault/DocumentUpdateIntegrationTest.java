
package com.trustvault;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
class DocumentUpdateIntegrationTest {

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private DocumentPermissionRepository permissionRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private DocumentPermissionService permissionService;

    @Autowired
    private DocumentUpdateService updateService;

    private AppUser createUser(String email, UserRole role) {
        return userRepository.saveAndFlush(
                new AppUser(email, "test-password", role));
    }

    private Document createDocument(AppUser owner) {
        return documentRepository.saveAndFlush(
                new Document(
                        "Original Title",
                        "original.pdf",
                        "General",
                        java.util.UUID.randomUUID().toString(),
                        owner));
    }

    @Test
    @Transactional
    void ownerCanUpdateMetadata() {
        AppUser owner = createUser(
                "update-owner@example.com", UserRole.OWNER);
        Document document = createDocument(owner);

        DocumentResponse response = updateService.updateMetadata(
                document.getId(),
                owner.getEmail(),
                "Updated Title",
                "Reports");

        assertEquals("Updated Title", response.title());
        assertEquals("Reports", response.category());

        AuditLog event = auditLogRepository
                .findByActorEmailOrderByOccurredAtDesc(owner.getEmail())
                .get(0);
        assertEquals("DOCUMENT_UPDATED", event.getAction());
        org.junit.jupiter.api.Assertions.assertTrue(
                event.getDetails().contains("Original Title' -> 'Updated Title"));
        org.junit.jupiter.api.Assertions.assertTrue(
                event.getDetails().contains("General' -> 'Reports"));
    }

    @Test
    @Transactional
    void userWithEditPermissionCanUpdateMetadata() {
        AppUser owner = createUser(
                "edit-owner@example.com", UserRole.OWNER);
        AppUser editor = createUser(
                "edit-user@example.com", UserRole.VIEWER);
        Document document = createDocument(owner);

        permissionRepository.saveAndFlush(
                new DocumentPermission(
                        document,
                        editor,
                        DocumentPermission.PermissionLevel.EDIT));

        DocumentResponse response = updateService.updateMetadata(
                document.getId(),
                editor.getEmail(),
                "Editor Updated Title",
                "Shared Reports");

        assertEquals("Editor Updated Title", response.title());
        assertEquals("Shared Reports", response.category());
    }

    @Test
    @Transactional
    void userWithReadPermissionCannotUpdateMetadata() {
        AppUser owner = createUser(
                "read-owner@example.com", UserRole.OWNER);
        AppUser viewer = createUser(
                "read-user@example.com", UserRole.VIEWER);
        Document document = createDocument(owner);

        permissionRepository.saveAndFlush(
                new DocumentPermission(
                        document,
                        viewer,
                        DocumentPermission.PermissionLevel.READ));

        assertThrows(
                AccessDeniedException.class,
                () -> updateService.updateMetadata(
                        document.getId(),
                        viewer.getEmail(),
                        "Unauthorized Title",
                        "Unauthorized Category"));

        Document unchanged = documentRepository
                .findById(document.getId())
                .orElseThrow();

        assertEquals("Original Title", unchanged.getTitle());
        assertEquals("General", unchanged.getCategory());
    }

    @Test
    @Transactional
    void revokedUserCannotUpdateMetadata() {
        AppUser owner = createUser(
                "revoke-owner@example.com", UserRole.OWNER);
        AppUser viewer = createUser(
                "revoke-user@example.com", UserRole.VIEWER);
        Document document = createDocument(owner);

        permissionRepository.saveAndFlush(
                new DocumentPermission(
                        document,
                        viewer,
                        DocumentPermission.PermissionLevel.EDIT));

        permissionService.revokePermission(
                document.getId(),
                owner.getEmail(),
                viewer.getEmail());

        assertThrows(
                AccessDeniedException.class,
                () -> updateService.updateMetadata(
                        document.getId(),
                        viewer.getEmail(),
                        "Unauthorized Title",
                        "Unauthorized Category"));

        Document unchanged = documentRepository
                .findById(document.getId())
                .orElseThrow();

        assertEquals("Original Title", unchanged.getTitle());
        assertEquals("General", unchanged.getCategory());
    }

    @Test
    @Transactional
    void unrelatedUserCannotUpdateMetadata() {
        AppUser owner = createUser(
                "unrelated-owner@example.com", UserRole.OWNER);
        AppUser stranger = createUser(
                "stranger@example.com", UserRole.VIEWER);
        Document document = createDocument(owner);

        assertThrows(
                AccessDeniedException.class,
                () -> updateService.updateMetadata(
                        document.getId(),
                        stranger.getEmail(),
                        "Unauthorized Title",
                        "Unauthorized Category"));

        Document unchanged = documentRepository
                .findById(document.getId())
                .orElseThrow();

        assertEquals("Original Title", unchanged.getTitle());
        assertEquals("General", unchanged.getCategory());
    }
}
