
package com.trustvault;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

@Entity
@Table(name = "documents")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String title;

    @NotBlank
    @Size(max = 255)
    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String category;

    @NotBlank
    @Size(max = 500)
    @Column(name = "storage_key", nullable = false, unique = true, length = 500)
    private String storageKey;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private AppUser owner;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Document() {
        // Required by JPA.
    }

    public Document(
            String title,
            String originalFilename,
            String category,
            String storageKey,
            AppUser owner) {
        this.title = title;
        this.originalFilename = originalFilename;
        this.category = category;
        this.storageKey = storageKey;
        this.owner = owner;
    }

    @jakarta.persistence.PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @jakarta.persistence.PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getCategory() {
        return category;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public AppUser getOwner() {
        return owner;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

public void updateMetadata(String title, String category) {
    this.title = title;
    this.category = category;
}

public void replaceFileMetadata(String originalFilename, String storageKey) {
    this.originalFilename = originalFilename;
    this.storageKey = storageKey;
}

}
