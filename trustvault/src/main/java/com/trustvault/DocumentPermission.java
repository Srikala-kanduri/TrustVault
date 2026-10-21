
package com.trustvault;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(
    name = "document_permissions",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_document_permission_user",
        columnNames = {"document_id", "user_id"}
    )
)
public class DocumentPermission {

    public enum PermissionLevel {
        READ,
        EDIT
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(name = "permission_level", nullable = false, length = 10)
    private PermissionLevel permissionLevel;

    @Column(name = "granted_at", nullable = false, updatable = false)
    private Instant grantedAt;

    protected DocumentPermission() {
        // Required by JPA.
    }

    public DocumentPermission(
            Document document,
            AppUser user,
            PermissionLevel permissionLevel) {
        this.document = document;
        this.user = user;
        this.permissionLevel = permissionLevel;
    }

    @jakarta.persistence.PrePersist
    protected void onCreate() {
        grantedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Document getDocument() {
        return document;
    }

    public AppUser getUser() {
        return user;
    }

    public PermissionLevel getPermissionLevel() {
        return permissionLevel;
    }

    public Instant getGrantedAt() {
        return grantedAt;
    }

    public void setPermissionLevel(PermissionLevel permissionLevel) {
        this.permissionLevel = permissionLevel;
    }
}
