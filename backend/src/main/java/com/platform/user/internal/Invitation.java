package com.platform.user.internal;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

/**
 * Represents an invitation for a user to join an organization.
 *
 * <p>This entity stores the details of an invitation, including the recipient's email, the role
 * they are invited to, and a unique token for accepting or declining the invitation. It also tracks
 * the invitation's lifecycle through the {@link Status} enum.
 *
 * @see Organization
 * @see OrganizationMember
 */
@Entity
@Table(
    name = "invitations",
    indexes = {
      @Index(name = "idx_invitations_token", columnList = "token", unique = true),
      @Index(name = "idx_invitations_organization", columnList = "organization_id"),
      @Index(name = "idx_invitations_email", columnList = "email"),
      @Index(name = "idx_invitations_status", columnList = "status"),
      @Index(name = "idx_invitations_expires_at", columnList = "expires_at")
    })
public class Invitation {

  /** The unique identifier for the invitation. */
  @Id
  @UuidGenerator
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  /** The ID of the organization the user is invited to. */
  @NotNull
  @Column(name = "organization_id", nullable = false)
  private UUID organizationId;

  /** The ID of the user who sent the invitation. */
  @NotNull
  @Column(name = "invited_by", nullable = false)
  private UUID invitedBy;

  /** The email address of the invited user. */
  @NotBlank
  @Email
  @Column(name = "email", nullable = false)
  private String email;

  /** The role the user is invited to have in the organization. */
  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false)
  private OrganizationMember.Role role;

  /** The current status of the invitation. */
  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private Status status = Status.PENDING;

  /** The unique token used to accept or decline the invitation. */
  @NotBlank
  @Column(name = "token", nullable = false, unique = true)
  private String token;

  /** The timestamp when the invitation expires. */
  @NotNull
  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  /** The timestamp of when the invitation was created. */
  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  /** The timestamp of when the invitation was accepted. */
  @Column(name = "accepted_at")
  private Instant acceptedAt;

  /** The timestamp of when the invitation was declined. */
  @Column(name = "declined_at")
  private Instant declinedAt;

  /** The version number for optimistic locking. */
  @Version private Long version;

  /**
   * Protected no-argument constructor for JPA.
   *
   * <p>This constructor is required by JPA and should not be used directly.
   */
  protected Invitation() {
    // JPA constructor
  }

  /**
   * Constructs a new Invitation with a default expiration of 7 days.
   *
   * @param organizationId the ID of the organization
   * @param invitedBy the ID of the user who sent the invitation
   * @param email the email address of the recipient
   * @param role the role assigned to the invited user
   * @param token the unique invitation token
   */
  public Invitation(
      UUID organizationId,
      UUID invitedBy,
      String email,
      OrganizationMember.Role role,
      String token) {
    this.organizationId = organizationId;
    this.invitedBy = invitedBy;
    this.email = email;
    this.role = role;
    this.token = token;
    this.expiresAt = Instant.now().plus(7, ChronoUnit.DAYS); // Default 7 days expiry
  }

  /**
   * Constructs a new Invitation with a specific expiration date.
   *
   * @param organizationId the ID of the organization
   * @param invitedBy the ID of the user who sent the invitation
   * @param email the email address of the recipient
   * @param role the role assigned to the invited user
   * @param token the unique invitation token
   * @param expiresAt the specific expiration timestamp
   */
  public Invitation(
      UUID organizationId,
      UUID invitedBy,
      String email,
      OrganizationMember.Role role,
      String token,
      Instant expiresAt) {
    this.organizationId = organizationId;
    this.invitedBy = invitedBy;
    this.email = email;
    this.role = role;
    this.token = token;
    this.expiresAt = expiresAt;
  }

  // Business methods

  /**
   * Checks if the invitation has expired.
   *
   * @return {@code true} if the current time is after the expiration time, {@code false} otherwise
   */
  public boolean isExpired() {
    return Instant.now().isAfter(expiresAt);
  }

  /**
   * Checks if the invitation is currently pending and has not expired.
   *
   * @return {@code true} if the invitation is in a pending state, {@code false} otherwise
   */
  public boolean isPending() {
    return status == Status.PENDING && !isExpired();
  }

  /**
   * Marks the invitation as accepted.
   *
   * @throws IllegalStateException if the invitation is not in a pending state
   */
  public void accept() {
    if (!isPending()) {
      throw new IllegalStateException("Cannot accept invitation that is not pending or is expired");
    }
    this.status = Status.ACCEPTED;
    this.acceptedAt = Instant.now();
  }

  /**
   * Marks the invitation as declined.
   *
   * @throws IllegalStateException if the invitation is not in a pending state
   */
  public void decline() {
    if (!isPending()) {
      throw new IllegalStateException(
          "Cannot decline invitation that is not pending or is expired");
    }
    this.status = Status.DECLINED;
    this.declinedAt = Instant.now();
  }

  /**
   * Revokes the invitation, making it invalid.
   *
   * @throws IllegalStateException if the invitation has already been accepted or declined
   */
  public void revoke() {
    if (status == Status.ACCEPTED || status == Status.DECLINED) {
      throw new IllegalStateException(
          "Cannot revoke invitation that has already been responded to");
    }
    this.status = Status.REVOKED;
  }

  /**
   * Expires a pending invitation.
   *
   * <p>If the invitation is currently pending, its status will be changed to EXPIRED.
   */
  public void expire() {
    if (status == Status.PENDING) {
      this.status = Status.EXPIRED;
    }
  }

  /** Marks the invitation as expired, regardless of its current state. */
  public void markAsExpired() {
    this.status = Status.EXPIRED;
  }

  // Static factory methods

  /**
   * Creates a new invitation with a specific expiration date.
   *
   * @param organizationId the ID of the organization
   * @param invitedBy the ID of the user sending the invitation
   * @param email the email address of the recipient
   * @param role the role to be assigned
   * @param expiresAt the expiration timestamp
   * @return a new {@link Invitation} instance
   */
  public static Invitation create(
      UUID organizationId,
      UUID invitedBy,
      String email,
      OrganizationMember.Role role,
      Instant expiresAt) {
    String token = java.util.UUID.randomUUID().toString();
    return new Invitation(organizationId, invitedBy, email, role, token, expiresAt);
  }

  /**
   * Creates a new invitation with a default expiration of 7 days.
   *
   * @param organizationId the ID of the organization
   * @param invitedBy the ID of the user sending the invitation
   * @param email the email address of the recipient
   * @param role the role to be assigned
   * @return a new {@link Invitation} instance
   */
  public static Invitation create(
      UUID organizationId, UUID invitedBy, String email, OrganizationMember.Role role) {
    String token = java.util.UUID.randomUUID().toString();
    return new Invitation(organizationId, invitedBy, email, role, token);
  }

  // Getters

  /**
   * Gets the unique identifier for the invitation.
   *
   * @return the ID of the invitation
   */
  public UUID getId() {
    return id;
  }

  /**
   * Gets the ID of the organization the user is invited to.
   *
   * @return the organization ID
   */
  public UUID getOrganizationId() {
    return organizationId;
  }

  /**
   * Gets the ID of the user who sent the invitation.
   *
   * @return the ID of the inviting user
   */
  public UUID getInvitedBy() {
    return invitedBy;
  }

  /**
   * Gets the email address of the invited user.
   *
   * @return the recipient's email address
   */
  public String getEmail() {
    return email;
  }

  /**
   * Gets the role the user is invited to have.
   *
   * @return the assigned role
   */
  public OrganizationMember.Role getRole() {
    return role;
  }

  /**
   * Gets the current status of the invitation.
   *
   * @return the invitation status
   */
  public Status getStatus() {
    return status;
  }

  /**
   * Gets the unique token for the invitation.
   *
   * @return the invitation token
   */
  public String getToken() {
    return token;
  }

  /**
   * Gets the expiration timestamp of the invitation.
   *
   * @return the expiration time
   */
  public Instant getExpiresAt() {
    return expiresAt;
  }

  /**
   * Gets the creation timestamp of the invitation.
   *
   * @return the creation time
   */
  public Instant getCreatedAt() {
    return createdAt;
  }

  /**
   * Gets the timestamp when the invitation was accepted.
   *
   * @return the acceptance time, or null if not accepted
   */
  public Instant getAcceptedAt() {
    return acceptedAt;
  }

  /**
   * Gets the timestamp when the invitation was declined.
   *
   * @return the decline time, or null if not declined
   */
  public Instant getDeclinedAt() {
    return declinedAt;
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
    if (!(obj instanceof Invitation other)) return false;
    return id != null && id.equals(other.id);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }

  @Override
  public String toString() {
    return "Invitation{"
        + "id="
        + id
        + ", organizationId="
        + organizationId
        + ", email='"
        + email
        + '\''
        + ", role="
        + role
        + ", status="
        + status
        + ", expiresAt="
        + expiresAt
        + '}';
  }

  /**
   * Enumerates the possible statuses of an invitation.
   */
  public enum Status {
    /** The invitation has been sent and is awaiting a response. */
    PENDING,
    /** The invitation has been accepted by the recipient. */
    ACCEPTED,
    /** The invitation has been declined by the recipient. */
    DECLINED,
    /** The invitation has expired and can no longer be acted upon. */
    EXPIRED,
    /** The invitation was revoked by the sender before it was actioned. */
    REVOKED
  }
}
