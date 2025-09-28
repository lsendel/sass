package com.platform.user.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * REST controller for permission operations in the RBAC system.
 * Provides endpoints for permission checking, validation, and system permission management.
 *
 * Following Spring Modulith principles, this controller only uses the public PermissionApi
 * and does not directly access internal implementation details.
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "${app.cors.allowed-origins:http://localhost:3000}")
public class PermissionController {

    private final PermissionApi permissionApi;

    @Autowired
    public PermissionController(PermissionApi permissionApi) {
        this.permissionApi = permissionApi;
    }

    /**
     * Get all available system permissions.
     * Used for permission management interfaces and role creation.
     */
    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('ORGANIZATIONS:READ') or hasAuthority('USERS:READ')")
    public ResponseEntity<PermissionsResponse> getAllPermissions() {
        List<PermissionApi.PermissionInfo> permissions = permissionApi.getAllPermissions();

        return ResponseEntity.ok(new PermissionsResponse(permissions));
    }

    /**
     * Get all unique resources in the permission system.
     * Used for resource-based permission management interfaces.
     */
    @GetMapping("/permissions/resources")
    @PreAuthorize("hasAuthority('ORGANIZATIONS:READ')")
    public ResponseEntity<ResourcesResponse> getAllResources() {
        List<String> resources = permissionApi.getAllResources();

        return ResponseEntity.ok(new ResourcesResponse(resources));
    }

    /**
     * Get all actions available for a specific resource.
     * Used for building dynamic permission management interfaces.
     */
    @GetMapping("/permissions/resources/{resource}/actions")
    @PreAuthorize("hasAuthority('ORGANIZATIONS:READ')")
    public ResponseEntity<ActionsResponse> getActionsByResource(@PathVariable String resource) {
        List<String> actions = permissionApi.getActionsByResource(resource);

        return ResponseEntity.ok(new ActionsResponse(resource, actions));
    }

    /**
     * Check multiple permissions for a user in an organization.
     * Used for batch permission validation in complex authorization scenarios.
     */
    @PostMapping("/organizations/{organizationId}/permissions/check")
    @PreAuthorize("hasAuthority('USERS:READ') and @organizationService.hasAccess(authentication.name, #organizationId)")
    public ResponseEntity<PermissionCheckResponse> checkUserPermissions(
            @PathVariable Long organizationId,
            @Valid @RequestBody PermissionCheckRequest request) {

        // Get current user ID from security context
        // In a real implementation, this would come from the authenticated user
        Long currentUserId = getCurrentUserId();

        List<PermissionApi.PermissionCheckResult> results = permissionApi.checkUserPermissions(
            currentUserId, organizationId, request.getPermissions()
        );

        return ResponseEntity.ok(new PermissionCheckResponse(results));
    }

    /**
     * Check a single permission for a user in an organization.
     * Used for quick permission validation.
     */
    @GetMapping("/organizations/{organizationId}/permissions/check")
    @PreAuthorize("hasAuthority('USERS:READ') and @organizationService.hasAccess(authentication.name, #organizationId)")
    public ResponseEntity<SinglePermissionCheckResponse> checkSinglePermission(
            @PathVariable Long organizationId,
            @RequestParam String resource,
            @RequestParam String action) {

        // Get current user ID from security context
        Long currentUserId = getCurrentUserId();

        boolean hasPermission = permissionApi.hasUserPermission(currentUserId, organizationId, resource, action);

        return ResponseEntity.ok(new SinglePermissionCheckResponse(resource, action, hasPermission));
    }

    /**
     * Get all permissions assigned to the current user in an organization.
     * Used for UI state management and navigation control.
     */
    @GetMapping("/organizations/{organizationId}/permissions/my-permissions")
    @PreAuthorize("@organizationService.hasAccess(authentication.name, #organizationId)")
    public ResponseEntity<UserPermissionsResponse> getMyPermissions(@PathVariable Long organizationId) {
        // Get current user ID from security context
        Long currentUserId = getCurrentUserId();

        var permissionKeys = permissionApi.getUserPermissionKeys(currentUserId, organizationId);

        return ResponseEntity.ok(new UserPermissionsResponse(permissionKeys));
    }

    /**
     * Validate permission format and values.
     * Used for permission validation in forms and APIs.
     */
    @PostMapping("/permissions/validate")
    @PreAuthorize("hasAuthority('ORGANIZATIONS:ADMIN')")
    public ResponseEntity<ValidationResponse> validatePermission(@Valid @RequestBody ValidationRequest request) {
        try {
            permissionApi.validatePermission(request.getResource(), request.getAction());
            return ResponseEntity.ok(new ValidationResponse(true, "Permission is valid"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(new ValidationResponse(false, e.getMessage()));
        }
    }

    // Helper methods

    private Long getCurrentUserId() {
        // In a real implementation, this would extract user ID from SecurityContext
        // For now, returning a placeholder
        return 1L; // TODO: Implement proper user ID extraction from JWT/session
    }

    // Response DTOs

    public record PermissionsResponse(List<PermissionApi.PermissionInfo> permissions) {}

    public record ResourcesResponse(List<String> resources) {}

    public record ActionsResponse(String resource, List<String> actions) {}

    public record PermissionCheckResponse(List<PermissionApi.PermissionCheckResult> results) {}

    public record SinglePermissionCheckResponse(String resource, String action, boolean hasPermission) {}

    public record UserPermissionsResponse(java.util.Set<String> permissions) {}

    public record ValidationResponse(boolean isValid, String message) {}

    // Request DTOs

    public static class PermissionCheckRequest {
        private List<PermissionApi.PermissionCheckRequest> permissions;

        public PermissionCheckRequest() {}

        public PermissionCheckRequest(List<PermissionApi.PermissionCheckRequest> permissions) {
            this.permissions = permissions;
        }

        public List<PermissionApi.PermissionCheckRequest> getPermissions() {
            return permissions;
        }

        public void setPermissions(List<PermissionApi.PermissionCheckRequest> permissions) {
            this.permissions = permissions;
        }
    }

    public static class ValidationRequest {
        private String resource;
        private String action;

        public ValidationRequest() {}

        public ValidationRequest(String resource, String action) {
            this.resource = resource;
            this.action = action;
        }

        public String getResource() {
            return resource;
        }

        public void setResource(String resource) {
            this.resource = resource;
        }

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }
    }
}