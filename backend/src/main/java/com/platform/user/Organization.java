package com.platform.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

/**
 * Organization entity representing a tenant in the multi-tenant architecture.
 * Each organization can have multiple users and maintains its own isolated data.
 *
 * <p>This is the aggregate root for the organization subdomain.
 *
 * @since 1.0.0
 */
@Entity
@Table(name = "organizations")
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    @Column(length = 255)
    private String domain;

    @Column(name = "billing_email", length = 255)
    private String billingEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private OrganizationStatus status = OrganizationStatus.ACTIVE;

    @Column(name = "settings", columnDefinition = "TEXT")
    private String settings; // JSON string for organization settings

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    private Long version;

    /**
     * Default constructor for JPA.
     */
    protected Organization() {
    }

    /**
     * Creates a new organization with the given name and slug.
     *
     * @param name the organization name
     * @param slug the URL-friendly slug (unique identifier)
     */
    public Organization(final String name, final String slug) {
        this.name = name;
        this.slug = slug;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.status = OrganizationStatus.ACTIVE;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    /**
     * Checks if the organization is active.
     *
     * @return true if status is ACTIVE and not soft deleted
     */
    public boolean isActive() {
        return this.status == OrganizationStatus.ACTIVE && this.deletedAt == null;
    }

    /**
     * Suspends the organization (e.g., for non-payment).
     */
    public void suspend() {
        if (this.status == OrganizationStatus.ACTIVE) {
            this.status = OrganizationStatus.SUSPENDED;
        }
    }

    /**
     * Activates a suspended organization.
     */
    public void activate() {
        if (this.status == OrganizationStatus.SUSPENDED) {
            this.status = OrganizationStatus.ACTIVE;
            this.deletedAt = null;
        }
    }

    /**
     * Soft deletes the organization.
     */
    public void delete() {
        this.deletedAt = Instant.now();
        this.status = OrganizationStatus.DELETED;
    }

    // Getters and setters

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(final String slug) {
        this.slug = slug;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(final String domain) {
        this.domain = domain;
    }

    public String getBillingEmail() {
        return billingEmail;
    }

    public void setBillingEmail(final String billingEmail) {
        this.billingEmail = billingEmail;
    }

    public OrganizationStatus getStatus() {
        return status;
    }

    public void setStatus(final OrganizationStatus status) {
        this.status = status;
    }

    public String getSettings() {
        return settings;
    }

    public void setSettings(final String settings) {
        this.settings = settings;
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

    /**
     * Organization status enumeration.
     */
    public enum OrganizationStatus {
        /** Organization is active and can be used */
        ACTIVE,
        /** Organization is temporarily suspended */
        SUSPENDED,
        /** Organization has been deleted */
        DELETED
    }
}
