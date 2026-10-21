
package com.trustvault;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
