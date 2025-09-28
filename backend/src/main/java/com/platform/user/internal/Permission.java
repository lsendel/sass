package com.platform.user.internal;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Objects;

/**
 * JPA Entity representing a system permission in the RBAC system.
 * Permissions define what actions can be performed on specific resources.
 * Uses Resource×Action model (e.g., USERS:READ, PAYMENTS:WRITE).
 *
 * This entity is managed at the system level and shared across all organizations.
 * Individual organizations cannot create custom permissions - they can only
 * assign existing system permissions to their custom roles.
 */
@Entity
@Table(name = "permissions", indexes = {
    @Index(name = "idx_permissions_resource_action", columnList = "resource, action", unique = true),
    @Index(name = "idx_permissions_active", columnList = "is_active")
})
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotBlank
    @Size(max = 50)
    @Column(name = "resource", nullable = false, length = 50)
    private String resource;

    @NotBlank
    @Size(max = 50)
    @Column(name = "action", nullable = false, length = 50)
    private String action;

    @Size(max = 255)
    @Column(name = "description")
    private String description;

    @NotNull
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public Permission() {
        this.createdAt = Instant.now();
        this.isActive = true;
    }

    public Permission(String resource, String action, String description) {
        this();
        this.resource = resource;
        this.action = action;
        this.description = description;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public boolean isActive() {
        return Boolean.TRUE.equals(isActive);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Business methods
    public String getPermissionKey() {
        return resource + ":" + action;
    }

    public boolean matches(String resource, String action) {
        return Objects.equals(this.resource, resource) &&
               Objects.equals(this.action, action) &&
               isActive();
    }

    // Equals, hashCode, toString
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Permission that = (Permission) o;
        return Objects.equals(id, that.id) &&
               Objects.equals(resource, that.resource) &&
               Objects.equals(action, that.action);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, resource, action);
    }

    @Override
    public String toString() {
        return "Permission{" +
               "id=" + id +
               ", resource='" + resource + '\'' +
               ", action='" + action + '\'' +
               ", description='" + description + '\'' +
               ", isActive=" + isActive +
               ", createdAt=" + createdAt +
               '}';
    }
}