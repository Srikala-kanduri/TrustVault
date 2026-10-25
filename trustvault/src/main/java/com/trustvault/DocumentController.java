
package com.trustvault;

import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentAuthorizationService authorizationService;
    private final DocumentUploadService uploadService;
    private final DocumentDownloadService downloadService;
    private final DocumentDeletionService deletionService;
    private final DocumentUpdateService updateService;
    private final DocumentFileReplacementService fileReplacementService;
    private final AuditService auditService;

    public DocumentController(
            DocumentAuthorizationService authorizationService,
            DocumentUploadService uploadService,
            DocumentDownloadService downloadService,
            DocumentDeletionService deletionService,
            DocumentUpdateService updateService,
            DocumentFileReplacementService fileReplacementService,
            AuditService auditService) {
        this.authorizationService = authorizationService;
        this.uploadService = uploadService;
        this.downloadService = downloadService;
        this.deletionService = deletionService;
        this.updateService = updateService;
        this.fileReplacementService = fileReplacementService;
        this.auditService = auditService;
    }

    @PostMapping(consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentResponse uploadDocument(
            @RequestParam String title,
            @RequestParam String category,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) throws IOException {

        return uploadService.upload(
                authentication.getName(),
                title,
                category,
                file);
    }

    
    @GetMapping("/{id}/download")
    public org.springframework.http.ResponseEntity<byte[]> downloadDocument(
            @PathVariable Long id,
            Authentication authentication) throws IOException {

        DocumentDownloadService.DownloadedDocument file =
                downloadService.download(id, authentication.getName());

        String filename = file.filename().replace("\\", "/");
        filename = filename.substring(filename.lastIndexOf("/") + 1);
        filename = filename.replaceAll("[\\r\\n\"]", "_");

        byte[] content = file.content();

        return org.springframework.http.ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .contentLength(content.length)
                .body(content);
    }


    @GetMapping("/{id}")
    public DocumentResponse getDocument(
            @PathVariable Long id,
            Authentication authentication) {

        Document document;
        try {
            document = authorizationService.requireReadAccess(
                    id, authentication.getName());
        } catch (AccessDeniedException exception) {
            try {
                auditService.recordDenied(
                        authentication.getName(),
                        "DOCUMENT_READ",
                        id,
                        "Document detail read denied");
            } catch (RuntimeException auditException) {
                exception.addSuppressed(auditException);
            }
            throw exception;
        }

        auditService.recordSuccess(
                authentication.getName(),
                "DOCUMENT_READ",
                id,
                "Document details viewed: " + document.getTitle());

        return DocumentResponse.from(document);
    }

    @GetMapping
    public List<DocumentResponse> listDocuments(
            Authentication authentication) {

        return authorizationService
                .listAccessibleDocuments(authentication.getName())
                .stream()
                .map(DocumentResponse::from)
                .toList();
    }

    @GetMapping("/search")
    public List<DocumentResponse> searchDocuments(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String ownerEmail,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant fromDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant toDate,
            Authentication authentication) {

        return authorizationService.searchAccessibleDocuments(
                        authentication.getName(),
                        title,
                        category,
                        ownerEmail,
                        fromDate,
                        toDate)
                .stream()
                .map(DocumentResponse::from)
                .toList();
    }
    
@DeleteMapping("/{id}")
@ResponseStatus(HttpStatus.NO_CONTENT)
public void deleteDocument(
        @PathVariable Long id,
        Authentication authentication) throws IOException {

    deletionService.deleteDocument(id, authentication.getName());
}

public record UpdateDocumentRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 100) String category) {
}

@PatchMapping("/{id}")
public DocumentResponse updateDocument(
        @PathVariable Long id,
        @Valid @RequestBody UpdateDocumentRequest request,
        Authentication authentication) {

    return updateService.updateMetadata(
            id,
            authentication.getName(),
            request.title(),
            request.category());
}

@org.springframework.web.bind.annotation.PutMapping(
        value = "/{id}/file",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public DocumentResponse replaceDocumentFile(
        @PathVariable Long id,
        @RequestParam("file") MultipartFile file,
        Authentication authentication) throws IOException {

    return fileReplacementService.replaceFile(
            id,
            authentication.getName(),
            file);
}

@GetMapping("/{id}/my-permissions")
public java.util.Map<String, Boolean> getMyPermissions(
        @PathVariable Long id,
        Authentication authentication) {

    Document document = authorizationService.requireReadAccess(
            id, authentication.getName());

    boolean isOwner = document.getOwner().getEmail()
            .equalsIgnoreCase(authentication.getName());

    boolean canEdit = isOwner;

    if (!isOwner) {
        try {
            authorizationService.requireEditAccess(
                    id, authentication.getName());
            canEdit = true;
        } catch (org.springframework.security.access.AccessDeniedException exception) {
            canEdit = false;
        }
    }

    return java.util.Map.of(
            "isOwner", isOwner,
            "canEdit", canEdit);
}

}
