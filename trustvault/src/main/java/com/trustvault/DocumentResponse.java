
package com.trustvault;

import java.time.Instant;

public record DocumentResponse(
        Long id,
        String title,
        String originalFilename,
        String category,
        String ownerEmail,
        Instant createdAt,
        Instant updatedAt) {

    public static DocumentResponse from(Document document) {
        return new DocumentResponse(
                document.getId(),
                document.getTitle(),
                document.getOriginalFilename(),
                document.getCategory(),
                document.getOwner().getEmail(),
                document.getCreatedAt(),
                document.getUpdatedAt());
    }
}
