package com.platform.user.internal;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Objects;

/**
 * JPA Entity representing the assignment of permissions to roles.
 * This is the junction table that connects roles to their permissions.
 *
 * Tracks when permissions were added to roles for audit purposes.
 */
@Entity
@Table(name = "role_permissions", indexes = {
    @Index(name = "idx_role_permissions_role_permission", columnList = "role_id, permission_id", unique = true),
    @Index(name = "idx_role_permissions_role", columnList = "role_id"),
    @Index(name = "idx_role_permissions_permission", columnList = "permission_id"),
    @Index(name = "idx_role_permissions_created_at", columnList = "created_at")
})
public class RolePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @NotNull
    @Column(name = "permission_id", nullable = false)
    private Long permissionId;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public RolePermission() {
        this.createdAt = Instant.now();
    }

    public RolePermission(Long roleId, Long permissionId) {
        this();
        this.roleId = roleId;
        this.permissionId = permissionId;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public Long getPermissionId() {
        return permissionId;
    }

    public void setPermissionId(Long permissionId) {
        this.permissionId = permissionId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    // Business methods
    public boolean isValid() {
        return roleId != null && permissionId != null;
    }

    // Equals, hashCode, toString
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RolePermission that = (RolePermission) o;
        return Objects.equals(id, that.id) &&
               Objects.equals(roleId, that.roleId) &&
               Objects.equals(permissionId, that.permissionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, roleId, permissionId);
    }

    @Override
    public String toString() {
        return "RolePermission{" +
               "id=" + id +
               ", roleId=" + roleId +
               ", permissionId=" + permissionId +
               ", createdAt=" + createdAt +
               '}';
    }
}