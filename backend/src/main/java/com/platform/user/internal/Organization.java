package com.platform.user.internal;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

/**
 * Represents an organization, which serves as a container for users and resources.
 *
 * <p>This entity is central to the application's multi-tenancy model, providing isolation between
 * different customer accounts. It includes details such as the organization's name, a unique slug
 * for URL identification, and a flexible settings map for custom attributes.
 *
 * @see User
 * @see OrganizationMember
 */
@Entity
@Table(
    name = "organizations",
    indexes = {
      @Index(name = "idx_organizations_slug", columnList = "slug", unique = true),
      @Index(name = "idx_organizations_owner", columnList = "owner_id"),
      @Index(name = "idx_organizations_deleted_at", columnList = "deleted_at")
    })
public class Organization {

  /** The unique identifier for the organization. */
  @Id
  @UuidGenerator
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  /** The name of the organization. */
  @NotBlank
  @Size(max = 255)
  @Column(name = "name", nullable = false)
  private String name;

  /** A unique, URL-friendly slug for the organization. */
  @NotBlank
  @Size(max = 100)
  @Pattern(
      regexp = "^[a-z0-9-]+$",
      message = "Slug must contain only lowercase letters, numbers, and hyphens")
  @Column(name = "slug", nullable = false, unique = true, length = 100)
  private String slug;

  /** The ID of the user who owns the organization. */
  @Column(name = "owner_id")
  private UUID ownerId;

  /** The current status of the organization. */
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private Status status = Status.ACTIVE;

  /** A flexible map for storing custom settings for the organization. */
  @Column(name = "settings")
  @Convert(converter = MapToJsonConverter.class)
  private Map<String, Object> settings = Map.of();

  /** The timestamp of when the organization was created. */
  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  /** The timestamp of the last update to the organization. */
  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  /** The timestamp of when the organization was soft-deleted. */
  @Column(name = "deleted_at")
  private Instant deletedAt;

  /** The version number for optimistic locking. */
  @Version private Long version;

  /**
   * Protected no-argument constructor for JPA.
   *
   * <p>This constructor is required by JPA and should not be used directly.
   */
  protected Organization() {
    // JPA constructor
  }

  /**
   * Constructs a new Organization.
   *
   * @param name the name of the organization
   * @param slug a URL-friendly slug for the organization
   * @param ownerId the ID of the user who owns the organization
   */
  public Organization(String name, String slug, UUID ownerId) {
    this.name = name;
    this.slug = validateAndNormalizeSlug(slug);
    this.ownerId = ownerId;
  }

  /**
   * Convenience constructor for testing purposes.
   *
   * @param name the name of the organization
   * @param slug a URL-friendly slug for the organization
   * @param description a description to be stored in the settings map
   */
  public Organization(String name, String slug, String description) {
    this.name = name;
    this.slug = validateAndNormalizeSlug(slug);
    // Description can be stored in settings
    if (description != null) {
      this.settings = Map.of("description", description);
    }
  }

  // Business methods

  /**
   * Checks if the organization has been soft-deleted.
   *
   * @return {@code true} if the organization is marked as deleted, {@code false} otherwise
   */
  public boolean isDeleted() {
    return deletedAt != null;
  }

  /** Marks the organization as soft-deleted by setting the deleted timestamp. */
  public void markAsDeleted() {
    this.deletedAt = Instant.now();
  }

  /**
   * Updates the name and settings of the organization.
   *
   * @param name the new name for the organization
   * @param settings a map containing the new settings
   */
  public void updateDetails(String name, Map<String, Object> settings) {
    this.name = name;
    this.settings = settings != null ? Map.copyOf(settings) : Map.of();
  }

  /**
   * Updates the settings of the organization.
   *
   * @param settings a map containing the new settings
   */
  public void updateSettings(Map<String, Object> settings) {
    this.settings = settings != null ? Map.copyOf(settings) : Map.of();
  }

  /**
   * Changes the owner of the organization.
   *
   * @param newOwnerId the ID of the new owner
   */
  public void changeOwner(UUID newOwnerId) {
    this.ownerId = newOwnerId;
  }

  /**
   * Validates and normalizes a slug string.
   *
   * @param slug the slug to validate
   * @return the normalized slug
   * @throws IllegalArgumentException if the slug is invalid
   */
  private String validateAndNormalizeSlug(String slug) {
    if (slug == null || slug.trim().isEmpty()) {
      throw new IllegalArgumentException("Organization slug cannot be null or empty");
    }

    String normalized = slug.trim().toLowerCase();
    if (!normalized.matches("^[a-z0-9-]+$")) {
      throw new IllegalArgumentException(
          "Slug must contain only lowercase letters, numbers, and hyphens");
    }

    if (normalized.length() > 100) {
      throw new IllegalArgumentException("Slug cannot be longer than 100 characters");
    }

    return normalized;
  }

  // Settings helper methods

  /**
   * Retrieves a specific setting's value from the settings map.
   *
   * @param key the key of the setting to retrieve
   * @param defaultValue the value to return if the setting is not found
   * @param <T> the type of the setting's value
   * @return the setting's value, or the default value if not present
   */
  @SuppressWarnings("unchecked")
  public <T> T getSetting(String key, T defaultValue) {
    return (T) settings.getOrDefault(key, defaultValue);
  }

  /**
   * Sets a specific setting's value in the settings map.
   *
   * @param key the key of the setting to set
   * @param value the value to set for the setting
   */
  public void setSetting(String key, Object value) {
    var updatedSettings = new java.util.HashMap<>(settings);
    updatedSettings.put(key, value);
    this.settings = Map.copyOf(updatedSettings);
  }

  // Getters

  /**
   * Gets the unique identifier for the organization.
   *
   * @return the ID of the organization
   */
  public UUID getId() {
    return id;
  }

  /**
   * Gets the name of the organization.
   *
   * @return the name of the organization
   */
  public String getName() {
    return name;
  }

  /**
   * Gets the URL-friendly slug for the organization.
   *
   * @return the slug of the organization
   */
  public String getSlug() {
    return slug;
  }

  /**
   * Gets the ID of the user who owns the organization.
   *
   * @return the owner's ID
   */
  public UUID getOwnerId() {
    return ownerId;
  }

  /**
   * Gets the current status of the organization.
   *
   * @return the organization status
   */
  public Status getStatus() {
    return status;
  }

  /**
   * Sets the status of the organization.
   *
   * @param status the new status
   */
  public void setStatus(Status status) {
    this.status = status;
  }

  /**
   * Gets a copy of the settings map for the organization.
   *
   * @return an immutable map of settings
   */
  public Map<String, Object> getSettings() {
    return Map.copyOf(settings);
  }

  /**
   * Convenience accessor to get the description from the settings map.
   *
   * @return the description, or null if not set
   */
  public String getDescription() {
    Object desc = settings != null ? settings.get("description") : null;
    return desc != null ? String.valueOf(desc) : null;
  }

  /**
   * Gets the creation timestamp of the organization.
   *
   * @return the creation time
   */
  public Instant getCreatedAt() {
    return createdAt;
  }

  /**
   * Gets the last update timestamp of the organization.
   *
   * @return the last update time
   */
  public Instant getUpdatedAt() {
    return updatedAt;
  }

  /**
   * Gets the soft-deletion timestamp of the organization.
   *
   * @return the deletion time, or null if not deleted
   */
  public Instant getDeletedAt() {
    return deletedAt;
  }

  /**
   * Gets the version number for optimistic locking.
   *
   * @return the version number
   */
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
    return "Organization{"
        + "id="
        + id
        + ", name='"
        + name
        + '\''
        + ", slug='"
        + slug
        + '\''
        + ", ownerId="
        + ownerId
        + ", createdAt="
        + createdAt
        + '}';
  }

  /**
   * Enumerates the possible statuses of an organization.
   */
  public enum Status {
    /** The organization is active and operational. */
    ACTIVE,
    /** The organization is inactive and not operational. */
    INACTIVE,
    /** The organization has been suspended. */
    SUSPENDED,
    /** The organization is pending setup or verification. */
    PENDING
  }
}
