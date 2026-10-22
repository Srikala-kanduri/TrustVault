
package com.trustvault;

import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.time.Instant;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
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

    public DocumentController(
            DocumentAuthorizationService authorizationService,
            DocumentUploadService uploadService,
            DocumentDownloadService downloadService,
            DocumentDeletionService deletionService) {
        this.authorizationService = authorizationService;
        this.uploadService = uploadService;
        this.downloadService = downloadService;
        this.deletionService = deletionService;
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

        Document document = authorizationService.requireReadAccess(
                id, authentication.getName());

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

}
