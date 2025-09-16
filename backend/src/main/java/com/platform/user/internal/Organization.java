package com.platform.user.internal;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Organization entity for multi-tenant isolation and team management.
 */
@Entity
@Table(name = "organizations", indexes = {
    @Index(name = "idx_organizations_slug", columnList = "slug", unique = true),
    @Index(name = "idx_organizations_owner", columnList = "owner_id"),
    @Index(name = "idx_organizations_deleted_at", columnList = "deleted_at")
})
public class Organization {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotBlank
    @Size(max = 255)
    @Column(name = "name", nullable = false)
    private String name;

    @NotBlank
    @Size(max = 100)
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Slug must contain only lowercase letters, numbers, and hyphens")
    @Column(name = "slug", nullable = false, unique = true, length = 100)
    private String slug;

    @Column(name = "owner_id")
    private UUID ownerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status = Status.ACTIVE;

    @Column(name = "settings")
    @Convert(converter = MapToJsonConverter.class)
    private Map<String, Object> settings = Map.of();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    private Long version;

    // Constructors
    protected Organization() {
        // JPA constructor
    }

    public Organization(String name, String slug, UUID ownerId) {
        this.name = name;
        this.slug = validateAndNormalizeSlug(slug);
        this.ownerId = ownerId;
    }

    // Business methods
    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void markAsDeleted() {
        this.deletedAt = Instant.now();
    }

    public void updateDetails(String name, Map<String, Object> settings) {
        this.name = name;
        this.settings = settings != null ? Map.copyOf(settings) : Map.of();
    }

    public void updateSettings(Map<String, Object> settings) {
        this.settings = settings != null ? Map.copyOf(settings) : Map.of();
    }

    public void changeOwner(UUID newOwnerId) {
        this.ownerId = newOwnerId;
    }

    private String validateAndNormalizeSlug(String slug) {
        if (slug == null || slug.trim().isEmpty()) {
            throw new IllegalArgumentException("Organization slug cannot be null or empty");
        }

        String normalized = slug.trim().toLowerCase();
        if (!normalized.matches("^[a-z0-9-]+$")) {
            throw new IllegalArgumentException("Slug must contain only lowercase letters, numbers, and hyphens");
        }

        if (normalized.length() > 100) {
            throw new IllegalArgumentException("Slug cannot be longer than 100 characters");
        }

        return normalized;
    }

    // Settings helper methods
    @SuppressWarnings("unchecked")
    public <T> T getSetting(String key, T defaultValue) {
        return (T) settings.getOrDefault(key, defaultValue);
    }

    public void setSetting(String key, Object value) {
        var updatedSettings = new java.util.HashMap<>(settings);
        updatedSettings.put(key, value);
        this.settings = Map.copyOf(updatedSettings);
    }

    // Getters
    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSlug() {
        return slug;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Map<String, Object> getSettings() {
        return Map.copyOf(settings);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public Long getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Organization other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Organization{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", slug='" + slug + '\'' +
                ", ownerId=" + ownerId +
                ", createdAt=" + createdAt +
                '}';
    }

    /**
     * Organization status enumeration
     */
    public enum Status {
        ACTIVE,
        INACTIVE,
        SUSPENDED,
        PENDING
    }
}