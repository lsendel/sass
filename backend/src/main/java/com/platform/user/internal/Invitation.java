package com.platform.user.internal;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Invitation entity for organization member invitations.
 */
@Entity
@Table(name = "invitations", indexes = {
    @Index(name = "idx_invitations_token", columnList = "token", unique = true),
    @Index(name = "idx_invitations_organization", columnList = "organization_id"),
    @Index(name = "idx_invitations_email", columnList = "email"),
    @Index(name = "idx_invitations_status", columnList = "status"),
    @Index(name = "idx_invitations_expires_at", columnList = "expires_at")
})
public class Invitation {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotNull
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @NotNull
    @Column(name = "invited_by", nullable = false)
    private UUID invitedBy;

    @NotBlank
    @Email
    @Column(name = "email", nullable = false)
    private String email;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private OrganizationMember.Role role;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status = Status.PENDING;

    @NotBlank
    @Column(name = "token", nullable = false, unique = true)
    private String token;

    @NotNull
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "declined_at")
    private Instant declinedAt;

    @Version
    private Long version;

    // Constructors
    protected Invitation() {
        // JPA constructor
    }

    public Invitation(UUID organizationId, UUID invitedBy, String email, OrganizationMember.Role role, String token) {
        this.organizationId = organizationId;
        this.invitedBy = invitedBy;
        this.email = email;
        this.role = role;
        this.token = token;
        this.expiresAt = Instant.now().plus(7, ChronoUnit.DAYS); // Default 7 days expiry
    }

    public Invitation(UUID organizationId, UUID invitedBy, String email, OrganizationMember.Role role, String token, Instant expiresAt) {
        this.organizationId = organizationId;
        this.invitedBy = invitedBy;
        this.email = email;
        this.role = role;
        this.token = token;
        this.expiresAt = expiresAt;
    }

    // Business methods
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isPending() {
        return status == Status.PENDING && !isExpired();
    }

    public void accept() {
        if (!isPending()) {
            throw new IllegalStateException("Cannot accept invitation that is not pending or is expired");
        }
        this.status = Status.ACCEPTED;
        this.acceptedAt = Instant.now();
    }

    public void decline() {
        if (!isPending()) {
            throw new IllegalStateException("Cannot decline invitation that is not pending or is expired");
        }
        this.status = Status.DECLINED;
        this.declinedAt = Instant.now();
    }

    public void revoke() {
        if (status == Status.ACCEPTED || status == Status.DECLINED) {
            throw new IllegalStateException("Cannot revoke invitation that has already been responded to");
        }
        this.status = Status.REVOKED;
    }

    public void expire() {
        if (status == Status.PENDING) {
            this.status = Status.EXPIRED;
        }
    }

    public void markAsExpired() {
        this.status = Status.EXPIRED;
    }

    // Static factory methods
    public static Invitation create(UUID organizationId, UUID invitedBy, String email,
                                   OrganizationMember.Role role, Instant expiresAt) {
        String token = java.util.UUID.randomUUID().toString();
        return new Invitation(organizationId, invitedBy, email, role, token, expiresAt);
    }

    public static Invitation create(UUID organizationId, UUID invitedBy, String email,
                                   OrganizationMember.Role role) {
        String token = java.util.UUID.randomUUID().toString();
        return new Invitation(organizationId, invitedBy, email, role, token);
    }

    // Getters
    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getInvitedBy() {
        return invitedBy;
    }

    public String getEmail() {
        return email;
    }

    public OrganizationMember.Role getRole() {
        return role;
    }

    public Status getStatus() {
        return status;
    }

    public String getToken() {
        return token;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getAcceptedAt() {
        return acceptedAt;
    }

    public Instant getDeclinedAt() {
        return declinedAt;
    }

    public Long getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Invitation other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Invitation{" +
                "id=" + id +
                ", organizationId=" + organizationId +
                ", email='" + email + '\'' +
                ", role=" + role +
                ", status=" + status +
                ", expiresAt=" + expiresAt +
                '}';
    }

    /**
     * Invitation status enumeration
     */
    public enum Status {
        PENDING,
        ACCEPTED,
        DECLINED,
        EXPIRED,
        REVOKED
    }
}