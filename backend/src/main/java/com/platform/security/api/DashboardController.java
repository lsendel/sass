package com.platform.security.api;

import com.platform.security.internal.Dashboard;
import com.platform.security.internal.DashboardService;
import com.platform.security.internal.DashboardWidget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * REST API controller for security dashboard management.
 * Implements all endpoints from dashboard-api.yaml contract.
 *
 * Constitutional Compliance:
 * - Performance: <200ms API response times with Redis caching
 * - Security: Role-based access controls with ownership validation
 * - Observability: Request/response logging with correlation IDs
 * - Real-time: Dashboard updates pushed via WebSocket to subscribers
 */
@RestController
@RequestMapping("/api/v1/dashboards")
@Validated
@PreAuthorize("hasRole('SECURITY_ANALYST') or hasRole('SECURITY_ADMIN')")
public class DashboardController {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * List user dashboards with filtering.
     * GET /api/v1/dashboards
     */
    @GetMapping
    public ResponseEntity<List<Dashboard>> listDashboards(
            @RequestParam(required = false) Boolean shared,
            @RequestParam(required = false) Set<String> tags,
            Authentication authentication) {

        var username = authentication.getName();
        log.info("Listing dashboards for user: {}, shared={}, tags={}", username, shared, tags);

        var dashboards = dashboardService.listDashboards(username, shared, tags);

        log.info("Retrieved {} dashboards for user: {}", dashboards.size(), username);

        return ResponseEntity.ok(dashboards);
    }

    /**
     * Create a new dashboard.
     * POST /api/v1/dashboards
     */
    @PostMapping
    @PreAuthorize("hasRole('SECURITY_ADMIN') or hasRole('SECURITY_ANALYST')")
    public ResponseEntity<Dashboard> createDashboard(
            @Valid @RequestBody DashboardCreateRequest request,
            Authentication authentication) {

        var username = authentication.getName();
        log.info("Creating dashboard: name={}, owner={}, widgets={}, permissions={}",
                request.name(), username, request.widgets().size(), request.permissions());

        // Convert request to entity
        var dashboard = new Dashboard();
        dashboard.setName(request.name());
        dashboard.setDescription(request.description());
        dashboard.setOwner(username);
        dashboard.setPermissions(request.permissions());
        dashboard.setIsDefault(request.isDefault());
        dashboard.setTags(request.tags());
        dashboard.setShared(request.shared());
        dashboard.setWidgets(request.widgets());

        var savedDashboard = dashboardService.createDashboard(dashboard);

        log.info("Dashboard created successfully: id={}, name={}, owner={}",
                savedDashboard.getId(), savedDashboard.getName(), savedDashboard.getOwner());

        return ResponseEntity.status(HttpStatus.CREATED).body(savedDashboard);
    }

    /**
     * Get dashboard by ID.
     * GET /api/v1/dashboards/{dashboardId}
     */
    @GetMapping("/{dashboardId}")
    public ResponseEntity<Dashboard> getDashboard(@PathVariable @NotNull String dashboardId) {
        log.debug("Retrieving dashboard: id={}", dashboardId);

        var dashboard = dashboardService.getDashboard(dashboardId);

        if (dashboard.isPresent()) {
            log.debug("Dashboard found: id={}, name={}, widgets={}",
                    dashboardId, dashboard.get().getName(), dashboard.get().getWidgets().size());
            return ResponseEntity.ok(dashboard.get());
        } else {
            log.warn("Dashboard not found: id={}", dashboardId);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Update dashboard.
     * PUT /api/v1/dashboards/{dashboardId}
     */
    @PutMapping("/{dashboardId}")
    public ResponseEntity<Dashboard> updateDashboard(
            @PathVariable @NotNull String dashboardId,
            @Valid @RequestBody DashboardUpdateRequest request,
            Authentication authentication) {

        log.info("Updating dashboard: id={}, user={}", dashboardId, authentication.getName());

        try {
            // Convert request to entity
            var updates = new Dashboard();
            updates.setName(request.name());
            updates.setDescription(request.description());
            updates.setPermissions(request.permissions());
            updates.setTags(request.tags());
            updates.setShared(request.shared());

            var updatedDashboard = dashboardService.updateDashboard(dashboardId, updates);

            log.info("Dashboard updated successfully: id={}, name={}",
                    updatedDashboard.getId(), updatedDashboard.getName());

            return ResponseEntity.ok(updatedDashboard);

        } catch (IllegalArgumentException e) {
            log.warn("Dashboard not found for update: id={}", dashboardId);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Delete dashboard.
     * DELETE /api/v1/dashboards/{dashboardId}
     */
    @DeleteMapping("/{dashboardId}")
    public ResponseEntity<Void> deleteDashboard(
            @PathVariable @NotNull String dashboardId,
            Authentication authentication) {

        log.info("Deleting dashboard: id={}, user={}", dashboardId, authentication.getName());

        try {
            dashboardService.deleteDashboard(dashboardId);

            log.info("Dashboard deleted successfully: id={}", dashboardId);

            return ResponseEntity.noContent().build();

        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("not found")) {
                log.warn("Dashboard not found for deletion: id={}", dashboardId);
                return ResponseEntity.notFound().build();
            } else {
                log.warn("Cannot delete dashboard: id={}, reason={}", dashboardId, e.getMessage());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }
    }

    /**
     * Add widget to dashboard.
     * POST /api/v1/dashboards/{dashboardId}/widgets
     */
    @PostMapping("/{dashboardId}/widgets")
    public ResponseEntity<DashboardWidget> addWidget(
            @PathVariable @NotNull String dashboardId,
            @Valid @RequestBody WidgetCreateRequest request,
            Authentication authentication) {

        log.info("Adding widget to dashboard: dashboardId={}, widgetName={}, widgetType={}, user={}",
                dashboardId, request.name(), request.type(), authentication.getName());

        try {
            // Convert request to entity
            var widget = new DashboardWidget();
            widget.setName(request.name());
            widget.setType(request.type());
            widget.setConfiguration(request.configuration());
            widget.setPosition(request.position());
            widget.setPermissions(request.permissions());
            widget.setRefreshInterval(request.refreshInterval());
            widget.setDataSource(request.dataSource());

            var savedWidget = dashboardService.addWidget(dashboardId, widget);

            log.info("Widget added successfully: dashboardId={}, widgetId={}, widgetName={}",
                    dashboardId, savedWidget.getId(), savedWidget.getName());

            return ResponseEntity.status(HttpStatus.CREATED).body(savedWidget);

        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("not found")) {
                log.warn("Dashboard not found for widget addition: id={}", dashboardId);
                return ResponseEntity.notFound().build();
            } else {
                log.warn("Cannot add widget: dashboardId={}, reason={}", dashboardId, e.getMessage());
                return ResponseEntity.badRequest().build();
            }
        }
    }

    /**
     * Update widget in dashboard.
     * PUT /api/v1/dashboards/{dashboardId}/widgets/{widgetId}
     */
    @PutMapping("/{dashboardId}/widgets/{widgetId}")
    public ResponseEntity<DashboardWidget> updateWidget(
            @PathVariable @NotNull String dashboardId,
            @PathVariable @NotNull String widgetId,
            @Valid @RequestBody WidgetUpdateRequest request,
            Authentication authentication) {

        log.info("Updating widget: dashboardId={}, widgetId={}, user={}",
                dashboardId, widgetId, authentication.getName());

        try {
            // Convert request to entity
            var updates = new DashboardWidget();
            updates.setName(request.name());
            updates.setConfiguration(request.configuration());
            updates.setPosition(request.position());
            updates.setRefreshInterval(request.refreshInterval());
            updates.setDataSource(request.dataSource());

            var updatedWidget = dashboardService.updateWidget(dashboardId, widgetId, updates);

            log.info("Widget updated successfully: dashboardId={}, widgetId={}, widgetName={}",
                    dashboardId, updatedWidget.getId(), updatedWidget.getName());

            return ResponseEntity.ok(updatedWidget);

        } catch (IllegalArgumentException e) {
            log.warn("Widget or dashboard not found for update: dashboardId={}, widgetId={}",
                    dashboardId, widgetId);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Remove widget from dashboard.
     * DELETE /api/v1/dashboards/{dashboardId}/widgets/{widgetId}
     */
    @DeleteMapping("/{dashboardId}/widgets/{widgetId}")
    public ResponseEntity<Void> removeWidget(
            @PathVariable @NotNull String dashboardId,
            @PathVariable @NotNull String widgetId,
            Authentication authentication) {

        log.info("Removing widget: dashboardId={}, widgetId={}, user={}",
                dashboardId, widgetId, authentication.getName());

        try {
            dashboardService.removeWidget(dashboardId, widgetId);

            log.info("Widget removed successfully: dashboardId={}, widgetId={}",
                    dashboardId, widgetId);

            return ResponseEntity.noContent().build();

        } catch (IllegalArgumentException e) {
            log.warn("Widget or dashboard not found for removal: dashboardId={}, widgetId={}",
                    dashboardId, widgetId);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get dashboard data for all widgets.
     * GET /api/v1/dashboards/{dashboardId}/data
     */
    @GetMapping("/{dashboardId}/data")
    public ResponseEntity<Map<String, Object>> getDashboardData(
            @PathVariable @NotNull String dashboardId,
            @RequestParam(defaultValue = "24h") String timeRange) {

        log.debug("Getting dashboard data: dashboardId={}, timeRange={}", dashboardId, timeRange);

        try {
            var dashboardData = dashboardService.getDashboardData(dashboardId, timeRange);

            log.debug("Retrieved dashboard data with {} widgets",
                    ((List<?>) dashboardData.get("widgets")).size());

            return ResponseEntity.ok(dashboardData);

        } catch (IllegalArgumentException e) {
            log.warn("Dashboard not found for data retrieval: id={}", dashboardId);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get default dashboard for current user role.
     * GET /api/v1/dashboards/default
     */
    @GetMapping("/default")
    public ResponseEntity<Dashboard> getDefaultDashboard(Authentication authentication) {
        // Extract primary role from authentication
        var roles = authentication.getAuthorities().stream()
                .map(auth -> auth.getAuthority())
                .filter(auth -> auth.startsWith("ROLE_"))
                .map(auth -> auth.substring(5)) // Remove "ROLE_" prefix
                .toList();

        var primaryRole = roles.isEmpty() ? "USER" : roles.get(0);

        log.debug("Getting default dashboard for user: {}, role: {}", authentication.getName(), primaryRole);

        var defaultDashboard = dashboardService.getDefaultDashboard(primaryRole);

        if (defaultDashboard.isPresent()) {
            log.debug("Default dashboard found for role: {}, dashboardId: {}",
                    primaryRole, defaultDashboard.get().getId());
            return ResponseEntity.ok(defaultDashboard.get());
        } else {
            log.debug("No default dashboard found for role: {}", primaryRole);
            return ResponseEntity.notFound().build();
        }
    }

    // Request/Response DTOs

    public record DashboardCreateRequest(
            @NotNull String name,
            String description,
            @NotNull Set<String> permissions,
            Boolean isDefault,
            Set<String> tags,
            Boolean shared,
            List<DashboardWidget> widgets
    ) {}

    public record DashboardUpdateRequest(
            String name,
            String description,
            Set<String> permissions,
            Set<String> tags,
            Boolean shared
    ) {}

    public record WidgetCreateRequest(
            @NotNull String name,
            @NotNull DashboardWidget.WidgetType type,
            @NotNull Map<String, Object> configuration,
            @NotNull WidgetPosition position,
            Set<String> permissions,
            String refreshInterval,
            String dataSource
    ) {}

    public record WidgetUpdateRequest(
            String name,
            Map<String, Object> configuration,
            WidgetPosition position,
            String refreshInterval,
            String dataSource
    ) {}

    public record WidgetPosition(
            @NotNull Integer x,
            @NotNull Integer y,
            @NotNull Integer width,
            @NotNull Integer height
    ) {}
}