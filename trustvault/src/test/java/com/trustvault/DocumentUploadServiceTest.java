
package com.trustvault;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class DocumentUploadServiceTest {

    @Mock
    private DocumentFileStorageService storageService;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private AppUser owner;

    @Mock
    private DocumentFileValidator fileValidator;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private DocumentUploadService uploadService;

    @Test
    void uploadRejectsBlankTitle() throws IOException {
        when(userRepository.findByEmail("owner@example.com"))
                .thenReturn(Optional.of(owner));
        when(owner.getRole()).thenReturn(UserRole.OWNER);
        MultipartFile file = new MockMultipartFile(
                "file", "report.pdf", "application/pdf",
                "%PDF-1.7 test".getBytes());

        assertThrows(
                IllegalArgumentException.class,
                () -> uploadService.upload(
                        "owner@example.com", " ", "Finance", file));

        verify(storageService, never()).store(any());
        verify(documentRepository, never()).saveAndFlush(any());
    }

    @Test
    void uploadRejectsEmptyFile() throws IOException {
        when(userRepository.findByEmail("owner@example.com"))
                .thenReturn(Optional.of(owner));
        when(owner.getRole()).thenReturn(UserRole.OWNER);
        MultipartFile file = new MockMultipartFile(
                "file", "empty.pdf", "application/pdf", new byte[0]);

        assertThrows(
                IllegalArgumentException.class,
                () -> uploadService.upload(
                        "owner@example.com", "Report", "Finance", file));

        verify(storageService, never()).store(any());
    }

    @Test
    void uploadRejectsUnknownOwner() throws IOException {
        MultipartFile file = new MockMultipartFile(
                "file", "report.pdf", "application/pdf",
                "%PDF-1.7 test".getBytes());

        when(userRepository.findByEmail("missing@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                AccessDeniedException.class,
                () -> uploadService.upload(
                        "missing@example.com", "Report", "Finance", file));

        verify(fileValidator, never()).validate(file);
        verify(storageService, never()).store(any());

        verify(auditService).recordDenied(
                "missing@example.com",
                "DOCUMENT_UPLOAD",
                null,
                "Upload denied: only document owners may upload");
    }

    @Test
    void viewerCannotUploadDocument() throws IOException {
        MultipartFile file = new MockMultipartFile(
                "file", "report.pdf", "application/pdf",
                "%PDF-1.7 test".getBytes());
        AppUser viewer = org.mockito.Mockito.mock(AppUser.class);
        when(userRepository.findByEmail("viewer@example.com"))
                .thenReturn(Optional.of(viewer));
        when(viewer.getRole()).thenReturn(UserRole.VIEWER);

        assertThrows(
                AccessDeniedException.class,
                () -> uploadService.upload(
                        "viewer@example.com", "Report", "Finance", file));

        verify(auditService).recordDenied(
                "viewer@example.com",
                "DOCUMENT_UPLOAD",
                null,
                "Upload denied: only document owners may upload");
        verify(fileValidator, never()).validate(file);
        verify(storageService, never()).store(any());
    }

    @Test
    void uploadNormalizesOwnerEmailAndRecordsSuccess() throws IOException {
        when(userRepository.findByEmail("owner@example.com"))
                .thenReturn(Optional.of(owner));
        when(owner.getRole()).thenReturn(UserRole.OWNER);
        MultipartFile file = new MockMultipartFile(
                "file", "report.pdf", "application/pdf",
                "%PDF-1.7 test".getBytes());

        when(storageService.store(file)).thenReturn(
                "123e4567-e89b-12d3-a456-426614174000");

        Document document = new Document(
                "Report",
                "report.pdf",
                "Finance",
                "123e4567-e89b-12d3-a456-426614174000",
                owner);

        when(documentRepository.saveAndFlush(any(Document.class)))
                .thenReturn(document);
        when(owner.getEmail()).thenReturn("owner@example.com");

        DocumentResponse response = uploadService.upload(
                " OWNER@EXAMPLE.COM ", " Report ", " Finance ", file);

        assertEquals("Report", response.title());
        assertEquals("Finance", response.category());
        assertEquals("owner@example.com", response.ownerEmail());

        verify(fileValidator).validate(file);
        verify(storageService).store(file);
        verify(documentRepository).saveAndFlush(any(Document.class));

        verify(auditService).recordInCurrentTransaction(
                "owner@example.com",
                "DOCUMENT_UPLOAD",
                null,
                "Document uploaded: Report (report.pdf)",
                "SUCCESS");
    }
}
