package com.platform.user.internal;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Objects;

/**
 * JPA Entity representing a role in the RBAC system.
 * Roles are organization-scoped and can be either predefined (system) or custom (user-created).
 *
 * Predefined roles (Owner, Admin, Member, Viewer) are automatically created for each organization
 * and cannot be modified or deleted. Custom roles can be created by organization admins
 * with specific permission combinations.
 */
@Entity
@Table(name = "roles", indexes = {
    @Index(name = "idx_roles_org_name_type", columnList = "organization_id, name, role_type", unique = true),
    @Index(name = "idx_roles_organization", columnList = "organization_id"),
    @Index(name = "idx_roles_active", columnList = "is_active"),
    @Index(name = "idx_roles_created_by", columnList = "created_by")
})
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @NotBlank
    @Size(max = 100)
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Size(max = 500)
    @Column(name = "description", length = 500)
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "role_type", nullable = false, length = 20)
    private RoleType roleType;

    @NotNull
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @NotNull
    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "updated_by")
    private Long updatedBy;

    public Role() {
        this.createdAt = Instant.now();
        this.isActive = true;
    }

    public Role(Long organizationId, String name, String description, RoleType roleType, Long createdBy) {
        this();
        this.organizationId = organizationId;
        this.name = name;
        this.description = description;
        this.roleType = roleType;
        this.createdBy = createdBy;
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

    public Long getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public RoleType getRoleType() {
        return roleType;
    }

    public void setRoleType(RoleType roleType) {
        this.roleType = roleType;
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

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
    }

    // Business methods
    public boolean isPredefined() {
        return roleType == RoleType.PREDEFINED;
    }

    public boolean isCustom() {
        return roleType == RoleType.CUSTOM;
    }

    public boolean canBeModified() {
        return isCustom() && isActive();
    }

    public boolean canBeDeleted() {
        return isCustom() && isActive();
    }

    public String getDisplayName() {
        return name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
    }

    // Equals, hashCode, toString
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Role role = (Role) o;
        return Objects.equals(id, role.id) &&
               Objects.equals(organizationId, role.organizationId) &&
               Objects.equals(name, role.name) &&
               Objects.equals(roleType, role.roleType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, organizationId, name, roleType);
    }

    @Override
    public String toString() {
        return "Role{" +
               "id=" + id +
               ", organizationId=" + organizationId +
               ", name='" + name + '\'' +
               ", description='" + description + '\'' +
               ", roleType=" + roleType +
               ", isActive=" + isActive +
               ", createdAt=" + createdAt +
               '}';
    }
}