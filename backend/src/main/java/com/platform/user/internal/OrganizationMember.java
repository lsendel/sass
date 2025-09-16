package com.platform.user.internal;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

/**
 * Many-to-many relationship entity between Users and Organizations with role information.
 */
@Entity
@Table(name = "organization_members",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "organization_id"}),
    indexes = {
        @Index(name = "idx_org_members_user", columnList = "user_id"),
        @Index(name = "idx_org_members_org", columnList = "organization_id"),
        @Index(name = "idx_org_members_role", columnList = "role")
    })
public class OrganizationMember {

    /**
     * Member status enumeration
     */
    public enum Status {
        ACTIVE,
        INACTIVE,
        PENDING
    }

    /**
     * Available organization roles
     */
    public enum Role {
        OWNER("owner"),
        ADMIN("admin"),
        MEMBER("member");

        private final String value;

        Role(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static Role fromString(String role) {
            for (Role r : values()) {
                if (r.value.equals(role)) {
                    return r;
                }
            }
            throw new IllegalArgumentException("Invalid role: " + role);
        }
    }

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotNull
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @NotNull
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 50)
    private Role role = Role.MEMBER;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status = Status.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Version
    private Long version;

    // Constructors
    protected OrganizationMember() {
        // JPA constructor
    }

    public OrganizationMember(UUID userId, UUID organizationId, Role role) {
        this.userId = userId;
        this.organizationId = organizationId;
        this.role = role != null ? role : Role.MEMBER;
    }

    // Business methods
    public boolean hasRole(Role role) {
        return this.role == role;
    }

    public boolean isOwner() {
        return role == Role.OWNER;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public boolean isMember() {
        return role == Role.MEMBER;
    }

    public boolean canManageMembers() {
        return role == Role.OWNER || role == Role.ADMIN;
    }

    public boolean canManageBilling() {
        return role == Role.OWNER || role == Role.ADMIN;
    }

    public void changeRole(Role newRole) {
        if (newRole == null) {
            throw new IllegalArgumentException("Role cannot be null");
        }
        this.role = newRole;
    }

    // Static factory methods
    public static OrganizationMember createOwner(UUID userId, UUID organizationId) {
        return new OrganizationMember(userId, organizationId, Role.OWNER);
    }

    public static OrganizationMember createAdmin(UUID userId, UUID organizationId) {
        return new OrganizationMember(userId, organizationId, Role.ADMIN);
    }

    public static OrganizationMember createMember(UUID userId, UUID organizationId) {
        return new OrganizationMember(userId, organizationId, Role.MEMBER);
    }

    // Getters
    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public Role getRole() {
        return role;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getJoinedAt() {
        return createdAt; // joinedAt is the same as createdAt for this entity
    }

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
        return "OrganizationMember{" +
                "id=" + id +
                ", userId=" + userId +
                ", organizationId=" + organizationId +
                ", role=" + role +
                ", createdAt=" + createdAt +
                '}';
    }
}