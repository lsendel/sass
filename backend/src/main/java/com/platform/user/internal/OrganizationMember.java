package com.platform.user.internal;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

/**
 * Represents the membership of a {@link User} in an {@link Organization}.
 *
 * <p>This entity acts as a join table in the many-to-many relationship between users and
 * organizations, and it includes additional information about the membership, such as the member's
 * {@link Role} and {@link Status}.
 *
 * @see User
 * @see Organization
 */
@Entity
@Table(
    name = "organization_members",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "organization_id"}),
    indexes = {
      @Index(name = "idx_org_members_user", columnList = "user_id"),
      @Index(name = "idx_org_members_org", columnList = "organization_id"),
      @Index(name = "idx_org_members_role", columnList = "role")
    })
public class OrganizationMember {

  /** Enumerates the possible statuses of a member within an organization. */
  public enum Status {
    /** The member is currently active in the organization. */
    ACTIVE,
    /** The member is inactive and has limited access. */
    INACTIVE,
    /** The member's invitation is pending acceptance. */
    PENDING
  }

  /**
   * Enumerates the available roles a member can have within an organization.
   *
   * <p>Each role defines a set of permissions and responsibilities.
   */
  public enum Role {
    /** The highest level of access, with full control over the organization. */
    OWNER("owner"),
    /** Administrative access, with permissions to manage members and settings. */
    ADMIN("admin"),
    /** Basic membership with limited permissions. */
    MEMBER("member");

    private final String value;

    Role(String value) {
      this.value = value;
    }

    /**
     * Gets the string value of the role.
     *
     * @return the string value of the role
     */
    public String getValue() {
      return value;
    }

    /**
     * Creates a {@link Role} enum from a string value.
     *
     * @param role the string value of the role
     * @return the corresponding {@link Role} enum
     * @throws IllegalArgumentException if the role string is invalid
     */
    public static Role fromString(String role) {
      for (Role r : values()) {
        if (r.value.equals(role)) {
          return r;
        }
      }
      throw new IllegalArgumentException("Invalid role: " + role);
    }
  }

  /** The unique identifier for the membership record. */
  @Id
  @UuidGenerator
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  /** The ID of the user who is the member. */
  @NotNull
  @Column(name = "user_id", nullable = false)
  private UUID userId;

  /** The ID of the organization the user belongs to. */
  @NotNull
  @Column(name = "organization_id", nullable = false)
  private UUID organizationId;

  /** The role of the member within the organization. */
  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false, length = 50)
  private Role role = Role.MEMBER;

  /** The status of the member within the organization. */
  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private Status status = Status.ACTIVE;

  /** The timestamp of when the membership was created. */
  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  /** The version number for optimistic locking. */
  @Version private Long version;

  /**
   * Protected no-argument constructor for JPA.
   *
   * <p>This constructor is required by JPA and should not be used directly.
   */
  protected OrganizationMember() {
    // JPA constructor
  }

  /**
   * Constructs a new OrganizationMember.
   *
   * @param userId the ID of the user
   * @param organizationId the ID of the organization
   * @param role the role of the member
   */
  public OrganizationMember(UUID userId, UUID organizationId, Role role) {
    this.userId = userId;
    this.organizationId = organizationId;
    this.role = role != null ? role : Role.MEMBER;
  }

  // Business methods

  /**
   * Checks if the member has a specific role.
   *
   * @param role the role to check for
   * @return {@code true} if the member has the specified role, {@code false} otherwise
   */
  public boolean hasRole(Role role) {
    return this.role == role;
  }

  /**
   * Checks if the member is an owner of the organization.
   *
   * @return {@code true} if the member is an owner, {@code false} otherwise
   */
  public boolean isOwner() {
    return role == Role.OWNER;
  }

  /**
   * Checks if the member is an administrator of the organization.
   *
   * @return {@code true} if the member is an admin, {@code false} otherwise
   */
  public boolean isAdmin() {
    return role == Role.ADMIN;
  }

  /**
   * Checks if the member has the basic member role.
   *
   * @return {@code true} if the member is a standard member, {@code false} otherwise
   */
  public boolean isMember() {
    return role == Role.MEMBER;
  }

  /**
   * Checks if the member has permissions to manage other members.
   *
   * @return {@code true} if the member can manage other members, {@code false} otherwise
   */
  public boolean canManageMembers() {
    return role == Role.OWNER || role == Role.ADMIN;
  }

  /**
   * Checks if the member has permissions to manage billing.
   *
   * @return {@code true} if the member can manage billing, {@code false} otherwise
   */
  public boolean canManageBilling() {
    return role == Role.OWNER || role == Role.ADMIN;
  }

  /**
   * Changes the role of the member.
   *
   * @param newRole the new role to assign
   * @throws IllegalArgumentException if the new role is null
   */
  public void changeRole(Role newRole) {
    if (newRole == null) {
      throw new IllegalArgumentException("Role cannot be null");
    }
    this.role = newRole;
  }

  // Static factory methods

  /**
   * Creates a new organization member with the OWNER role.
   *
   * @param userId the ID of the user
   * @param organizationId the ID of the organization
   * @return a new {@link OrganizationMember} instance with the OWNER role
   */
  public static OrganizationMember createOwner(UUID userId, UUID organizationId) {
    return new OrganizationMember(userId, organizationId, Role.OWNER);
  }

  /**
   * Creates a new organization member with the ADMIN role.
   *
   * @param userId the ID of the user
   * @param organizationId the ID of the organization
   * @return a new {@link OrganizationMember} instance with the ADMIN role
   */
  public static OrganizationMember createAdmin(UUID userId, UUID organizationId) {
    return new OrganizationMember(userId, organizationId, Role.ADMIN);
  }

  /**
   * Creates a new organization member with the MEMBER role.
   *
   * @param userId the ID of the user
   * @param organizationId the ID of the organization
   * @return a new {@link OrganizationMember} instance with the MEMBER role
   */
  public static OrganizationMember createMember(UUID userId, UUID organizationId) {
    return new OrganizationMember(userId, organizationId, Role.MEMBER);
  }

  // Getters

  /**
   * Gets the unique identifier for the membership.
   *
   * @return the ID of the membership
   */
  public UUID getId() {
    return id;
  }

  /**
   * Gets the ID of the user.
   *
   * @return the user ID
   */
  public UUID getUserId() {
    return userId;
  }

  /**
   * Gets the ID of the organization.
   *
   * @return the organization ID
   */
  public UUID getOrganizationId() {
    return organizationId;
  }

  /**
   * Gets the role of the member.
   *
   * @return the member's role
   */
  public Role getRole() {
    return role;
  }

  /**
   * Gets the status of the member.
   *
   * @return the member's status
   */
  public Status getStatus() {
    return status;
  }

  /**
   * Sets the status of the member.
   *
   * @param status the new status
   */
  public void setStatus(Status status) {
    this.status = status;
  }

  /**
   * Gets the creation timestamp of the membership.
   *
   * @return the creation time
   */
  public Instant getCreatedAt() {
    return createdAt;
  }

  /**
   * Gets the timestamp when the member joined the organization.
   *
   * @return the join time, which is the same as the creation time
   */
  public Instant getJoinedAt() {
    return createdAt; // joinedAt is the same as createdAt for this entity
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
    if (!(obj instanceof OrganizationMember other)) return false;
    return id != null && id.equals(other.id);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }

  @Override
  public String toString() {
    return "OrganizationMember{"
        + "id="
        + id
        + ", userId="
        + userId
        + ", organizationId="
        + organizationId
        + ", role="
        + role
        + ", createdAt="
        + createdAt
        + '}';
  }
}
