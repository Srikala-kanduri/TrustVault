
package com.trustvault;

import java.time.Instant;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentAuthorizationService authorizationService;

    public DocumentController(
            DocumentAuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
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
}
