package com.platform.user.internal;

import com.platform.shared.types.Email;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * User entity representing authenticated users in the system.
 * Supports OAuth2 authentication with multiple providers.
 */
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_users_email", columnList = "email", unique = true),
    @Index(name = "idx_users_provider", columnList = "provider, provider_id", unique = true),
    @Index(name = "idx_users_deleted_at", columnList = "deleted_at")
})
public class User {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "email", nullable = false, unique = true))
    private Email email;

    @NotBlank
    @Column(name = "name", nullable = false)
    private String name;

    @NotBlank
    @Column(name = "provider", nullable = false, length = 50)
    private String provider;

    @NotBlank
    @Column(name = "provider_id", nullable = false)
    private String providerId;

    @Column(name = "preferences")
    @Convert(converter = MapToJsonConverter.class)
    private Map<String, Object> preferences = Map.of();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "last_active_at")
    private Instant lastActiveAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    private Long version;

    // Constructors
    protected User() {
        // JPA constructor
    }

    public User(Email email, String name, String provider, String providerId) {
        this.email = email;
        this.name = name;
        this.provider = provider;
        this.providerId = providerId;
    }

    // Business methods
    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void markAsDeleted() {
        this.deletedAt = Instant.now();
    }

    public void updateProfile(String name, Map<String, Object> preferences) {
        this.name = name;
        this.preferences = preferences != null ? Map.copyOf(preferences) : Map.of();
    }

    public void updatePreferences(Map<String, Object> preferences) {
        this.preferences = preferences != null ? Map.copyOf(preferences) : Map.of();
    }

    // Getters
    public UUID getId() {
        return id;
    }

    public Email getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public String getProvider() {
        return provider;
    }

    public String getProviderId() {
        return providerId;
    }

    public Map<String, Object> getPreferences() {
        return Map.copyOf(preferences);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getLastActiveAt() {
        return lastActiveAt;
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
        if (!(obj instanceof User other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", email=" + email +
                ", name='" + name + '\'' +
                ", provider='" + provider + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}