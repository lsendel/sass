package com.platform.auth.internal;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * OAuth2 Provider entity representing configured OAuth2 authentication providers
 * Stores provider configuration and metadata for Google, GitHub, Microsoft, etc.
 *
 * This entity is part of the auth module's internal domain model and should not
 * be directly exposed outside the auth module boundary.
 */
@Entity
@Table(name = "oauth2_providers",
       uniqueConstraints = @UniqueConstraint(columnNames = "name"),
       indexes = {
           @Index(name = "idx_oauth2_provider_name", columnList = "name"),
           @Index(name = "idx_oauth2_provider_enabled", columnList = "enabled")
       })
@EntityListeners(AuditingEntityListener.class)
public class OAuth2Provider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Provider name (google, github, microsoft, etc.)
     * Must be lowercase and match OAuth2 registration names
     */
    @NotBlank
    @Pattern(regexp = "^[a-z][a-z0-9_-]*$", message = "Provider name must be lowercase alphanumeric with hyphens/underscores")
    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name;

    /**
     * Human-readable display name for the provider
     */
    @NotBlank
    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    /**
     * OAuth2 authorization endpoint URL
     */
    @NotBlank
    @Pattern(regexp = "^https://.*", message = "Authorization URL must use HTTPS")
    @Column(name = "authorization_url", nullable = false, length = 500)
    private String authorizationUrl;

    /**
     * OAuth2 token endpoint URL
     */
    @NotBlank
    @Pattern(regexp = "^https://.*", message = "Token URL must use HTTPS")
    @Column(name = "token_url", nullable = false, length = 500)
    private String tokenUrl;

    /**
     * User info endpoint URL for retrieving user profile information
     */
    @NotBlank
    @Pattern(regexp = "^https://.*", message = "User info URL must use HTTPS")
    @Column(name = "user_info_url", nullable = false, length = 500)
    private String userInfoUrl;

    /**
     * OAuth2 scopes requested from this provider
     * Stored as comma-separated list
     */
    @NotEmpty
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "oauth2_provider_scopes",
                     joinColumns = @JoinColumn(name = "provider_id"))
    @Column(name = "scope", length = 100)
    private List<String> scopes;

    /**
     * Client ID for this OAuth2 provider registration
     * Should be configured via environment variables
     */
    @NotBlank
    @Column(name = "client_id", nullable = false, length = 255)
    private String clientId;

    /**
     * JSON Web Key Set URI for token validation (if applicable)
     */
    @Column(name = "jwk_set_uri", length = 500)
    private String jwkSetUri;

    /**
     * Attribute name in user info response that contains the user identifier
     * Default is 'sub' for standard OpenID Connect
     */
    @NotBlank
    @Column(name = "user_name_attribute", nullable = false, length = 50)
    private String userNameAttribute = "sub";

    /**
     * Whether this provider is currently enabled for authentication
     */
    @NotNull
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    /**
     * Provider-specific configuration as JSON
     * Used for additional settings that don't warrant separate columns
     */
    @Column(name = "configuration")
    private String configuration;

    /**
     * Sort order for displaying providers in UI
     */
    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Version field for optimistic locking
     */
    @Version
    @Column(name = "version")
    private Long version;

    // Default constructor for JPA
    protected OAuth2Provider() {}

    /**
     * Constructor for creating a new OAuth2 provider
     */
    public OAuth2Provider(String name, String displayName, String authorizationUrl,
                         String tokenUrl, String userInfoUrl, List<String> scopes, String clientId) {
        this.name = name;
        this.displayName = displayName;
        this.authorizationUrl = authorizationUrl;
        this.tokenUrl = tokenUrl;
        this.userInfoUrl = userInfoUrl;
        this.scopes = scopes;
        this.clientId = clientId;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getAuthorizationUrl() {
        return authorizationUrl;
    }

    public void setAuthorizationUrl(String authorizationUrl) {
        this.authorizationUrl = authorizationUrl;
    }

    public String getTokenUrl() {
        return tokenUrl;
    }

    public void setTokenUrl(String tokenUrl) {
        this.tokenUrl = tokenUrl;
    }

    public String getUserInfoUrl() {
        return userInfoUrl;
    }

    public void setUserInfoUrl(String userInfoUrl) {
        this.userInfoUrl = userInfoUrl;
    }

    public List<String> getScopes() {
        return scopes;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getJwkSetUri() {
        return jwkSetUri;
    }

    public void setJwkSetUri(String jwkSetUri) {
        this.jwkSetUri = jwkSetUri;
    }

    public String getUserNameAttribute() {
        return userNameAttribute;
    }

    public void setUserNameAttribute(String userNameAttribute) {
        this.userNameAttribute = userNameAttribute;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getConfiguration() {
        return configuration;
    }

    public void setConfiguration(String configuration) {
        this.configuration = configuration;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    // Business methods

    /**
     * Checks if this provider is available for authentication
     */
    public boolean isAvailable() {
        return enabled != null && enabled && clientId != null && !clientId.trim().isEmpty();
    }

    /**
     * Gets scope list as comma-separated string for OAuth2 requests
     */
    public String getScopesAsString() {
        return scopes != null ? String.join(" ", scopes) : "";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OAuth2Provider that)) return false;
        return Objects.equals(id, that.id) && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }

    @Override
    public String toString() {
        return "OAuth2Provider{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", displayName='" + displayName + '\'' +
                ", enabled=" + enabled +
                ", sortOrder=" + sortOrder +
                '}';
    }
}