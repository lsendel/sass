package com.platform.user.internal;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * JPA Entity representing the assignment of a role to a user.
 * This is the junction table that connects users to their roles within organizations.
 *
 * Supports role expiration for temporary access grants and tracks
 * assignment/removal audit information.
 */
@Entity
@Table(name = "user_roles", indexes = {
    @Index(name = "idx_user_roles_user_role", columnList = "user_id, role_id", unique = true),
    @Index(name = "idx_user_roles_user", columnList = "user_id"),
    @Index(name = "idx_user_roles_role", columnList = "role_id"),
    @Index(name = "idx_user_roles_expires", columnList = "expires_at"),
    @Index(name = "idx_user_roles_assigned_by", columnList = "assigned_by")
})
public class UserRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @NotNull
    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @NotNull
    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    @NotNull
    @Column(name = "assigned_by", nullable = false)
    private Long assignedBy;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "removed_at")
    private Instant removedAt;

    @Column(name = "removed_by")
    private Long removedBy;

    public UserRole() {
        this.assignedAt = Instant.now();
    }

    public UserRole(Long userId, Long roleId, Long assignedBy) {
        this();
        this.userId = userId;
        this.roleId = roleId;
        this.assignedBy = assignedBy;
    }

    public UserRole(Long userId, Long roleId, Long assignedBy, LocalDateTime expiresAt) {
        this(userId, roleId, assignedBy);
        this.expiresAt = expiresAt;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public Instant getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(Instant assignedAt) {
        this.assignedAt = assignedAt;
    }

    public Long getAssignedBy() {
        return assignedBy;
    }

    public void setAssignedBy(Long assignedBy) {
        this.assignedBy = assignedBy;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getRemovedAt() {
        return removedAt;
    }

    public void setRemovedAt(Instant removedAt) {
        this.removedAt = removedAt;
    }

    public Long getRemovedBy() {
        return removedBy;
    }

    public void setRemovedBy(Long removedBy) {
        this.removedBy = removedBy;
    }

    // Business methods
    public boolean isActive() {
        return removedAt == null && !isExpired();
    }

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isRemoved() {
        return removedAt != null;
    }

    public boolean hasExpiration() {
        return expiresAt != null;
    }

    public void markAsRemoved(Long removedBy) {
        this.removedAt = Instant.now();
        this.removedBy = removedBy;
    }

    public boolean isTemporary() {
        return hasExpiration();
    }

    public boolean isPermanent() {
        return !hasExpiration();
    }

    // Equals, hashCode, toString
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserRole userRole = (UserRole) o;
        return Objects.equals(id, userRole.id) &&
               Objects.equals(userId, userRole.userId) &&
               Objects.equals(roleId, userRole.roleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, userId, roleId);
    }

    @Override
    public String toString() {
        return "UserRole{" +
               "id=" + id +
               ", userId=" + userId +
               ", roleId=" + roleId +
               ", assignedAt=" + assignedAt +
               ", assignedBy=" + assignedBy +
               ", expiresAt=" + expiresAt +
               ", removedAt=" + removedAt +
               '}';
    }
}