package com.platform.security.internal;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * DashboardWidget entity for individual dashboard components.
 *
 * This entity represents configurable widgets within security dashboards
 * for displaying charts, tables, metrics, alerts, and threat maps.
 *
 * Key features:
 * - Many-to-one relationship with Dashboard
 * - JSON configuration for widget-specific settings
 * - Position-based grid layout system
 * - Configurable refresh intervals
 * - Role-based permissions per widget
 * - Data source specification for flexible backend queries
 */
@Entity
@Table(name = "dashboard_widgets",
       indexes = {
           @Index(name = "idx_dashboard_widgets_dashboard_id", columnList = "dashboard_id"),
           @Index(name = "idx_dashboard_widgets_type", columnList = "type")
       },
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_widget_name_dashboard", columnNames = {"name", "dashboard_id"})
       })
public class DashboardWidget {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dashboard_id", nullable = false)
    private Dashboard dashboard;

    @NotNull
    @Size(min = 1, max = 255)
    @Column(nullable = false, length = 255)
    private String name;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private WidgetType type;

    @NotNull
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> configuration = new HashMap<>();

    @NotNull
    @Column(nullable = false, columnDefinition = "jsonb")
    private WidgetPosition position;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "widget_permissions", joinColumns = @JoinColumn(name = "widget_id"))
    @Column(name = "permission")
    private Set<String> permissions = new HashSet<>();

    @NotNull
    @Column(name = "refresh_interval", nullable = false)
    private Duration refreshInterval = Duration.ofSeconds(30);

    @Column(name = "data_source", columnDefinition = "TEXT")
    private String dataSource;

    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "created_by", nullable = false, length = 255)
    private String createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "last_modified", nullable = false)
    private Instant lastModified;

    /**
     * Widget types supported by the security dashboard
     */
    public enum WidgetType {
        CHART,      // Line charts, bar charts, pie charts
        TABLE,      // Data tables with sorting and filtering
        METRIC,     // Single metric displays with gauges
        ALERT_LIST, // List of active security alerts
        THREAT_MAP  // Geographic threat visualization
    }

    /**
     * Widget position within the dashboard grid layout
     */
    @Embeddable
    public static class WidgetPosition {
        @Min(0)
        @Column(nullable = false)
        private Integer x = 0;

        @Min(0)
        @Column(nullable = false)
        private Integer y = 0;

        @Min(1)
        @Max(12)
        @Column(nullable = false)
        private Integer width = 4;

        @Min(1)
        @Max(20)
        @Column(nullable = false)
        private Integer height = 4;

        // Constructors
        public WidgetPosition() {
        }

        public WidgetPosition(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        // Getters and Setters
        public Integer getX() {
            return x;
        }

        public void setX(Integer x) {
            this.x = x;
        }

        public Integer getY() {
            return y;
        }

        public void setY(Integer y) {
            this.y = y;
        }

        public Integer getWidth() {
            return width;
        }

        public void setWidth(Integer width) {
            this.width = width;
        }

        public Integer getHeight() {
            return height;
        }

        public void setHeight(Integer height) {
            this.height = height;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            WidgetPosition that = (WidgetPosition) o;
            return Objects.equals(x, that.x) &&
                   Objects.equals(y, that.y) &&
                   Objects.equals(width, that.width) &&
                   Objects.equals(height, that.height);
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y, width, height);
        }

        @Override
        public String toString() {
            return "WidgetPosition{" +
                    "x=" + x +
                    ", y=" + y +
                    ", width=" + width +
                    ", height=" + height +
                    '}';
        }
    }

    // Constructors
    protected DashboardWidget() {
        // JPA constructor
    }

    public DashboardWidget(@NotNull String name,
                          @NotNull WidgetType type,
                          @NotNull String createdBy,
                          @NotNull Set<String> permissions) {
        this.name = name;
        this.type = type;
        this.createdBy = createdBy;
        this.permissions = new HashSet<>(permissions);
        this.position = new WidgetPosition();
        this.refreshInterval = Duration.ofSeconds(30);
    }

    public DashboardWidget(@NotNull String name,
                          @NotNull WidgetType type,
                          @NotNull Map<String, Object> configuration,
                          @NotNull WidgetPosition position,
                          @NotNull String createdBy,
                          @NotNull Set<String> permissions) {
        this.name = name;
        this.type = type;
        this.configuration = new HashMap<>(configuration);
        this.position = position;
        this.createdBy = createdBy;
        this.permissions = new HashSet<>(permissions);
        this.refreshInterval = Duration.ofSeconds(30);
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public Dashboard getDashboard() {
        return dashboard;
    }

    public void setDashboard(Dashboard dashboard) {
        this.dashboard = dashboard;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public WidgetType getType() {
        return type;
    }

    public void setType(WidgetType type) {
        this.type = type;
    }

    public Map<String, Object> getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Map<String, Object> configuration) {
        this.configuration = configuration;
    }

    public WidgetPosition getPosition() {
        return position;
    }

    public void setPosition(WidgetPosition position) {
        this.position = position;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<String> permissions) {
        this.permissions = permissions;
    }

    public Duration getRefreshInterval() {
        return refreshInterval;
    }

    public void setRefreshInterval(Duration refreshInterval) {
        if (refreshInterval != null && refreshInterval.getSeconds() >= 5) {
            this.refreshInterval = refreshInterval;
        } else {
            throw new IllegalArgumentException("Refresh interval must be at least 5 seconds");
        }
    }

    public String getDataSource() {
        return dataSource;
    }

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastModified() {
        return lastModified;
    }

    // Business Logic Methods

    /**
     * Check if user has permission to view this widget
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
     * Check if user can modify this widget
     *
     * @param userId The user ID
     * @param userRoles The roles of the user
     * @return true if user can modify
     */
    public boolean canModify(String userId, Set<String> userRoles) {
        // Creator can always modify
        if (Objects.equals(createdBy, userId)) {
            return true;
        }

        // Dashboard owner can modify
        if (dashboard != null && dashboard.isOwnedBy(userId)) {
            return true;
        }

        // Admin roles can modify any widget
        return userRoles.contains("ROLE_SECURITY_ADMIN") || userRoles.contains("ROLE_ADMIN");
    }

    /**
     * Update widget configuration with validation
     *
     * @param newConfiguration The new configuration
     */
    public void updateConfiguration(Map<String, Object> newConfiguration) {
        if (newConfiguration == null) {
            throw new IllegalArgumentException("Configuration cannot be null");
        }

        // Validate configuration based on widget type
        validateConfiguration(newConfiguration);
        this.configuration = new HashMap<>(newConfiguration);
    }

    /**
     * Validate configuration based on widget type
     *
     * @param config The configuration to validate
     */
    private void validateConfiguration(Map<String, Object> config) {
        switch (type) {
            case CHART:
                if (!config.containsKey("chartType")) {
                    throw new IllegalArgumentException("Chart widgets must specify chartType");
                }
                break;
            case TABLE:
                // Table configuration validation
                break;
            case METRIC:
                if (!config.containsKey("metricType")) {
                    throw new IllegalArgumentException("Metric widgets must specify metricType");
                }
                break;
            case ALERT_LIST:
                // Alert list configuration validation
                break;
            case THREAT_MAP:
                if (!config.containsKey("mapType")) {
                    throw new IllegalArgumentException("Threat map widgets must specify mapType");
                }
                break;
        }
    }

    /**
     * Update widget position with bounds checking
     *
     * @param newPosition The new position
     */
    public void updatePosition(WidgetPosition newPosition) {
        if (newPosition == null) {
            throw new IllegalArgumentException("Position cannot be null");
        }

        // Validate position bounds
        if (newPosition.getWidth() < 1 || newPosition.getWidth() > 12) {
            throw new IllegalArgumentException("Width must be between 1 and 12");
        }
        if (newPosition.getHeight() < 1 || newPosition.getHeight() > 20) {
            throw new IllegalArgumentException("Height must be between 1 and 20");
        }
        if (newPosition.getX() < 0 || newPosition.getY() < 0) {
            throw new IllegalArgumentException("Position coordinates cannot be negative");
        }

        this.position = newPosition;
    }

    /**
     * Add a permission to this widget
     *
     * @param permission The permission to add
     */
    public void addPermission(String permission) {
        if (permission != null && !permission.trim().isEmpty()) {
            permissions.add(permission.trim());
        }
    }

    /**
     * Remove a permission from this widget
     *
     * @param permission The permission to remove
     */
    public void removePermission(String permission) {
        if (permission != null) {
            permissions.remove(permission.trim());
        }
    }

    /**
     * Get configuration value by key
     *
     * @param key The configuration key
     * @return the value or null if not found
     */
    public Object getConfigurationValue(String key) {
        return configuration.get(key);
    }

    /**
     * Set configuration value
     *
     * @param key The configuration key
     * @param value The configuration value
     */
    public void setConfigurationValue(String key, Object value) {
        if (key != null) {
            configuration.put(key, value);
        }
    }

    /**
     * Check if this widget overlaps with another widget position
     *
     * @param other The other widget position
     * @return true if widgets overlap
     */
    public boolean overlaps(WidgetPosition other) {
        if (other == null) return false;

        return !(position.getX() >= other.getX() + other.getWidth() ||
                 other.getX() >= position.getX() + position.getWidth() ||
                 position.getY() >= other.getY() + other.getHeight() ||
                 other.getY() >= position.getY() + position.getHeight());
    }

    /**
     * Check if this is a chart widget
     *
     * @return true if widget type is CHART
     */
    public boolean isChart() {
        return type == WidgetType.CHART;
    }

    /**
     * Check if this is a real-time widget (requires frequent updates)
     *
     * @return true if widget needs real-time updates
     */
    public boolean isRealTime() {
        return type == WidgetType.ALERT_LIST || type == WidgetType.THREAT_MAP ||
               refreshInterval.getSeconds() <= 60;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DashboardWidget widget = (DashboardWidget) o;
        return Objects.equals(id, widget.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "DashboardWidget{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", type=" + type +
                ", position=" + position +
                ", refreshInterval=" + refreshInterval +
                ", createdBy='" + createdBy + '\'' +
                '}';
    }
}