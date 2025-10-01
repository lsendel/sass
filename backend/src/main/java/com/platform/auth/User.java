package com.platform.auth;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import jakarta.persistence.PreUpdate;
import java.time.Instant;
import java.util.UUID;

/**
 * User entity for authentication and authorization.
 * This is the aggregate root for the auth module.
 *
 * @since 1.0.0
 */
@Entity
@Table(name = "auth_users")
public class User {

    private static final int SECONDS_PER_MINUTE = 60;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private UserStatus status = UserStatus.PENDING_VERIFICATION;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts = 0;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    /**
     * Default constructor for JPA.
     */
    protected User() {
    }

    /**
     * Creates a new user with email and password hash.
     *
     * @param email the user's email address
     * @param passwordHash the BCrypt password hash
     */
    public User(final String email, final String passwordHash) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.status = UserStatus.PENDING_VERIFICATION;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    /**
     * Checks if the user account is currently locked.
     *
     * @return true if locked and lock has not expired
     */
    public boolean isLocked() {
        if (this.status == UserStatus.LOCKED && this.lockedUntil != null) {
            if (Instant.now().isBefore(this.lockedUntil)) {
                return true;
            }
            // Lock expired, automatically unlock
            this.status = UserStatus.ACTIVE;
            this.lockedUntil = null;
            return false;
        }
        return false;
    }

    /**
     * Checks if the user account is active and can authenticate.
     *
     * @return true if status is ACTIVE and not locked
     */
    public boolean isActive() {
        return this.status == UserStatus.ACTIVE && !isLocked();
    }

    /**
     * Increments failed login attempts and locks account if threshold exceeded.
     *
     * @param maxAttempts maximum allowed failed attempts before locking
     * @param lockDurationMinutes duration to lock the account in minutes
     */
    public void recordFailedLogin(final int maxAttempts, final long lockDurationMinutes) {
        this.failedLoginAttempts++;
        if (this.failedLoginAttempts >= maxAttempts) {
            this.status = UserStatus.LOCKED;
            this.lockedUntil = Instant.now().plusSeconds(lockDurationMinutes * SECONDS_PER_MINUTE);
        }
    }

    /**
     * Resets failed login attempts after successful authentication.
     */
    public void resetFailedAttempts() {
        this.failedLoginAttempts = 0;
    }

    /**
     * Marks the user as verified.
     */
    public void verify() {
        if (this.status == UserStatus.PENDING_VERIFICATION) {
            this.status = UserStatus.ACTIVE;
        }
    }

    /**
     * Soft deletes the user account.
     */
    public void delete() {
        this.deletedAt = Instant.now();
        this.status = UserStatus.DISABLED;
    }

    // Getters and setters

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(final String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(final String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(final String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(final String lastName) {
        this.lastName = lastName;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(final UserStatus status) {
        this.status = status;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public void setFailedLoginAttempts(final int failedLoginAttempts) {
        this.failedLoginAttempts = failedLoginAttempts;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public void setLockedUntil(final Instant lockedUntil) {
        this.lockedUntil = lockedUntil;
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

    /**
     * User account status enumeration.
     */
    public enum UserStatus {
        /** Account is active and can authenticate */
        ACTIVE,
        /** Account is temporarily locked due to failed login attempts */
        LOCKED,
        /** Account is permanently disabled */
        DISABLED,
        /** Account is pending email verification */
        PENDING_VERIFICATION
    }
}
