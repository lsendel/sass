package com.platform.auth.internal;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

/**
 * TokenMetadata entity for secure session and opaque token management.
 * Implements the constitutional requirement for opaque tokens (no JWT).
 */
@Entity
@Table(
        name = "token_metadata",
        indexes = {
                @Index(name = "idx_token_metadata_user", columnList = "user_id"),
                @Index(name = "idx_token_metadata_hash", columnList = "token_hash", unique = true),
                @Index(name = "idx_token_metadata_expires", columnList = "expires_at")
        })
public class TokenMetadata {

    private static final long SECONDS_PER_MINUTE = 60L;
    private static final long MINUTES_PER_HOUR = 60L;
    private static final long HOURS_PER_DAY = 24L;
    private static final long STALE_DAYS = 30L;
    private static final long EXPIRY_SOON_WINDOW_SECONDS = 5L * SECONDS_PER_MINUTE;
    private static final long STALE_THRESHOLD_SECONDS =
            STALE_DAYS * HOURS_PER_DAY * MINUTES_PER_HOUR * SECONDS_PER_MINUTE;

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotNull
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @NotBlank
    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @NotBlank
    @Column(name = "salt", nullable = false)
    private String salt;

    @Column(name = "last_used", nullable = false)
    private Instant lastUsed;

    @NotNull
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "metadata")
    @Convert(converter = TokenMetadataConverter.class)
    private Map<String, Object> metadata = Map.of();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Version
    private Long version;

    // Constructors
    protected TokenMetadata() {
        // JPA constructor
    }

    public TokenMetadata(final UUID userId, final String tokenHash, final String salt, final Instant expiresAt) {
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.salt = salt;
        this.expiresAt = expiresAt;
        this.lastUsed = Instant.now();
    }

    // Factory methods
    public static TokenMetadata createWebSession(final UUID userId, final String tokenHash, final String salt,
                                                 final Instant expiresAt, final String ipAddress,
                                                 final String userAgent) {
        var token = new TokenMetadata(userId, tokenHash, salt, expiresAt);
        token.addMetadata("sessionType", "web");
        token.addMetadata("ipAddress", ipAddress);
        token.addMetadata("userAgent", userAgent);
        return token;
    }

    public static TokenMetadata createApiToken(final UUID userId, final String tokenHash, final String salt,
                                               final Instant expiresAt, final String apiKeyName) {
        var token = new TokenMetadata(userId, tokenHash, salt, expiresAt);
        token.addMetadata("sessionType", "api");
        token.addMetadata("apiKeyName", apiKeyName);
        return token;
    }

    public static TokenMetadata createOAuthSession(final UUID userId, final String tokenHash, final String salt,
                                                   final Instant expiresAt, final String oauthProvider,
                                                   final String ipAddress) {
        var token = new TokenMetadata(userId, tokenHash, salt, expiresAt);
        token.addMetadata("sessionType", "oauth");
        token.addMetadata("oauthProvider", oauthProvider);
        token.addMetadata("ipAddress", ipAddress);
        return token;
    }

    // Business methods
    /** Returns true if the token is expired at the current time. */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    /** Returns true if the token is still valid (not expired). */
    public boolean isValid() {
        return !isExpired();
    }

    /** Updates the last-used timestamp to the current instant. */
    public void updateLastUsed() {
        this.lastUsed = Instant.now();
    }

    /** Extends the expiration timestamp; must be in the future. */
    public void extendExpiration(final Instant newExpiresAt) {
        if (newExpiresAt.isBefore(Instant.now())) {
            throw new IllegalArgumentException("New expiration time cannot be in the past");
        }
        this.expiresAt = newExpiresAt;
    }

    /** Immediately revokes the token by setting expiration to now. */
    public void revoke() {
        this.expiresAt = Instant.now();
    }

    /** Indicates whether the token will expire within the next five minutes. */
    public boolean willExpireSoon() {
        return expiresAt.isBefore(Instant.now().plusSeconds(EXPIRY_SOON_WINDOW_SECONDS));
    }

    // Metadata helper methods
    /** Adds or replaces a metadata entry (immutable copy maintained). */
    public void addMetadata(final String key, final Object value) {
        var updatedMetadata = new java.util.HashMap<>(this.metadata);
        updatedMetadata.put(key, value);
        this.metadata = Map.copyOf(updatedMetadata);
    }

    /** Removes a metadata entry if present (immutable copy maintained). */
    public void removeMetadata(final String key) {
        var updatedMetadata = new java.util.HashMap<>(this.metadata);
        updatedMetadata.remove(key);
        this.metadata = Map.copyOf(updatedMetadata);
    }

    @SuppressWarnings("unchecked")
    /** Retrieves a typed metadata value with a default fallback. */
    public <T> T getMetadata(final String key, final T defaultValue) {
        return (T) metadata.getOrDefault(key, defaultValue);
    }

    /** Returns the session type stored in metadata (api, web, oauth). */
    public String getSessionType() {
        return getMetadata("sessionType", "unknown");
    }

    /** Returns the originating IP address if present. */
    public String getIpAddress() {
        return getMetadata("ipAddress", null);
    }

    /** Returns the originating user agent if present. */
    public String getUserAgent() {
        return getMetadata("userAgent", null);
    }

    /** Returns the OAuth provider if present. */
    public String getOAuthProvider() {
        return getMetadata("oauthProvider", null);
    }

    /** Returns the associated API key name if present. */
    public String getApiKeyName() {
        return getMetadata("apiKeyName", null);
    }

    // Session type checks
    /** True for browser-based sessions (web or oauth). */
    public boolean isWebSession() {
        return "web".equals(getSessionType()) || "oauth".equals(getSessionType());
    }

    /** True for API tokens. */
    public boolean isApiToken() {
        return "api".equals(getSessionType());
    }

    /** True for OAuth sessions. */
    public boolean isOAuthSession() {
        return "oauth".equals(getSessionType());
    }

    // Security methods
    /**
     * Checks whether the access appears to originate from the same device (IP + user agent).
     */
    public boolean isFromSameDevice(final String ipAddress, final String userAgent) {
        return ipAddress != null && ipAddress.equals(getIpAddress())
                && userAgent != null && userAgent.equals(getUserAgent());
    }

    /** Seconds remaining until expiration (may be negative if already expired). */
    public long getTimeUntilExpiration() {
        return expiresAt.getEpochSecond() - Instant.now().getEpochSecond();
    }

    /** Seconds elapsed since the token was last used. */
    public long getTimeSinceLastUsed() {
        return Instant.now().getEpochSecond() - lastUsed.getEpochSecond();
    }

    /** True if not used for more than 30 days. */
    public boolean isStale() {
        // Consider token stale if not used for more than configured threshold
        return getTimeSinceLastUsed() > STALE_THRESHOLD_SECONDS;
    }

    // Getters
    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public String getSalt() {
        return salt;
    }

    public Instant getLastUsed() {
        return lastUsed;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Map<String, Object> getMetadata() {
        return Map.copyOf(metadata);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Long getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof TokenMetadata other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "TokenMetadata{"
                + "id=" + id
                + ", userId=" + userId
                + ", sessionType='" + getSessionType() + '\''
                + ", expiresAt=" + expiresAt
                + ", lastUsed=" + lastUsed
                + ", createdAt=" + createdAt
                + '}';
    }
}
