package com.platform.security.internal;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Dashboard entity for security dashboard configurations and metadata.
 *
 * This entity represents user-customizable security monitoring dashboards
 * containing widgets for real-time security metrics visualization.
 *
 * Key features:
 * - One-to-many relationship with DashboardWidget
 * - Role-based permissions for access control
 * - Owner-based multi-tenancy support
 * - Shared dashboard capability
 * - Tag-based categorization
 * - Version tracking for concurrent modifications
 */
@Entity
@Table(name = "dashboards",
       indexes = {
           @Index(name = "idx_dashboards_owner", columnList = "owner"),
           @Index(name = "idx_dashboards_is_default", columnList = "isDefault"),
           @Index(name = "idx_dashboards_shared", columnList = "shared")
       },
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_dashboard_name_owner", columnNames = {"name", "owner"})
       })
public class Dashboard {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @Size(min = 1, max = 255)
    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @OneToMany(mappedBy = "dashboard", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("position")
    private List<DashboardWidget> widgets = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "dashboard_permissions", joinColumns = @JoinColumn(name = "dashboard_id"))
    @Column(name = "permission")
    private Set<String> permissions = new HashSet<>();

    @NotNull
    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "dashboard_tags", joinColumns = @JoinColumn(name = "dashboard_id"))
    @Column(name = "tag")
    private Set<String> tags = new HashSet<>();

    @NotNull
    @Size(min = 1, max = 255)
    @Column(nullable = false, length = 255)
    private String owner;

    @NotNull
    @Column(nullable = false)
    private Boolean shared = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "last_modified", nullable = false)
    private Instant lastModified;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    // Constructors
    protected Dashboard() {
        // JPA constructor
    }

    public Dashboard(@NotNull String name,
                    @NotNull String owner,
                    @NotNull Set<String> permissions) {
        this.name = name;
        this.owner = owner;
        this.permissions = new HashSet<>(permissions);
        this.isDefault = false;
        this.shared = false;
    }

    public Dashboard(@NotNull String name,
                    String description,
                    @NotNull String owner,
                    @NotNull Set<String> permissions,
                    boolean isDefault,
                    boolean shared) {
        this.name = name;
        this.description = description;
        this.owner = owner;
        this.permissions = new HashSet<>(permissions);
        this.isDefault = isDefault;
        this.shared = shared;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
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

    public List<DashboardWidget> getWidgets() {
        return widgets;
    }

    public void setWidgets(List<DashboardWidget> widgets) {
        this.widgets = widgets;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<String> permissions) {
        this.permissions = permissions;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public Boolean getShared() {
        return shared;
    }

    public void setShared(Boolean shared) {
        this.shared = shared;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastModified() {
        return lastModified;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    // Business Logic Methods

    /**
     * Add a widget to this dashboard
     *
     * @param widget The widget to add
     * @throws IllegalArgumentException if widget limit exceeded (max 20)
     */
    public void addWidget(DashboardWidget widget) {
        if (widgets.size() >= 20) {
            throw new IllegalArgumentException("Maximum 20 widgets allowed per dashboard");
        }

        if (widget.getName() != null && hasWidgetWithName(widget.getName())) {
            throw new IllegalArgumentException("Widget with name '" + widget.getName() + "' already exists");
        }

        widget.setDashboard(this);
        widgets.add(widget);
    }

    /**
     * Remove a widget from this dashboard
     *
     * @param widget The widget to remove
     */
    public void removeWidget(DashboardWidget widget) {
        widgets.remove(widget);
        widget.setDashboard(null);
    }

    /**
     * Remove a widget by ID
     *
     * @param widgetId The ID of the widget to remove
     * @return true if widget was removed, false if not found
     */
    public boolean removeWidgetById(UUID widgetId) {
        return widgets.removeIf(widget -> Objects.equals(widget.getId(), widgetId));
    }

    /**
     * Check if user has permission to access this dashboard
     *
     * @param userRoles The roles of the user
     * @return true if user has access
     */
    public boolean hasAccess(Set<String> userRoles) {
        if (permissions.isEmpty()) {
            return false;
        }
        return userRoles.stream().anyMatch(permissions::contains);
    }

    /**
     * Check if user can modify this dashboard
     *
     * @param userId The user ID
     * @param userRoles The roles of the user
     * @return true if user can modify
     */
    public boolean canModify(String userId, Set<String> userRoles) {
        // Owner can always modify
        if (Objects.equals(owner, userId)) {
            return true;
        }

        // Admin roles can modify any dashboard
        return userRoles.contains("ROLE_SECURITY_ADMIN") || userRoles.contains("ROLE_ADMIN");
    }

    /**
     * Check if this dashboard has a widget with the given name
     *
     * @param name The widget name to check
     * @return true if widget exists
     */
    public boolean hasWidgetWithName(String name) {
        return widgets.stream()
                .anyMatch(widget -> Objects.equals(widget.getName(), name));
    }

    /**
     * Get widget by ID
     *
     * @param widgetId The widget ID
     * @return the widget or null if not found
     */
    public DashboardWidget getWidgetById(UUID widgetId) {
        return widgets.stream()
                .filter(widget -> Objects.equals(widget.getId(), widgetId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Add a tag to this dashboard
     *
     * @param tag The tag to add
     */
    public void addTag(String tag) {
        if (tag != null && !tag.trim().isEmpty()) {
            tags.add(tag.trim().toLowerCase());
        }
    }

    /**
     * Remove a tag from this dashboard
     *
     * @param tag The tag to remove
     */
    public void removeTag(String tag) {
        if (tag != null) {
            tags.remove(tag.trim().toLowerCase());
        }
    }

    /**
     * Check if dashboard has a specific tag
     *
     * @param tag The tag to check
     * @return true if tag exists
     */
    public boolean hasTag(String tag) {
        return tag != null && tags.contains(tag.trim().toLowerCase());
    }

    /**
     * Add a permission to this dashboard
     *
     * @param permission The permission to add
     */
    public void addPermission(String permission) {
        if (permission != null && !permission.trim().isEmpty()) {
            permissions.add(permission.trim());
        }
    }

    /**
     * Remove a permission from this dashboard
     *
     * @param permission The permission to remove
     */
    public void removePermission(String permission) {
        if (permission != null) {
            permissions.remove(permission.trim());
        }
    }

    /**
     * Check if this is the owner's dashboard
     *
     * @param userId The user ID to check
     * @return true if user is the owner
     */
    public boolean isOwnedBy(String userId) {
        return Objects.equals(owner, userId);
    }

    /**
     * Get the total number of widgets
     *
     * @return widget count
     */
    public int getWidgetCount() {
        return widgets.size();
    }

    /**
     * Check if dashboard is at widget limit
     *
     * @return true if at maximum widget capacity
     */
    public boolean isAtWidgetLimit() {
        return widgets.size() >= 20;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Dashboard dashboard = (Dashboard) o;
        return Objects.equals(id, dashboard.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Dashboard{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", owner='" + owner + '\'' +
                ", widgetCount=" + widgets.size() +
                ", shared=" + shared +
                ", isDefault=" + isDefault +
                '}';
    }
}