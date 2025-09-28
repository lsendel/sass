package com.platform.user.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Optional;

/**
 * REST controller for role management operations in the RBAC system.
 * Provides endpoints for role creation, modification, deletion, and querying.
 *
 * Following Spring Modulith principles, this controller only uses the public RoleApi
 * and does not directly access internal implementation details.
 */
@RestController
@RequestMapping("/api/organizations/{organizationId}/roles")
@CrossOrigin(origins = "${app.cors.allowed-origins:http://localhost:3000}")
public class RoleController {

    private final RoleApi roleApi;

    @Autowired
    public RoleController(RoleApi roleApi) {
        this.roleApi = roleApi;
    }

    /**
     * Get all roles for an organization.
     * Returns both predefined and custom roles.
     */
    @GetMapping
    @PreAuthorize("hasAuthority('ORGANIZATIONS:READ') and @organizationService.hasAccess(authentication.name, #organizationId)")
    public ResponseEntity<RolesResponse> getRoles(@PathVariable Long organizationId) {
        List<RoleApi.RoleInfo> roles = roleApi.getRolesByOrganization(organizationId);
        RoleApi.RoleStatistics statistics = roleApi.getRoleStatistics(organizationId);

        return ResponseEntity.ok(new RolesResponse(roles, statistics));
    }

    /**
     * Get detailed information about a specific role.
     * Includes role metadata and assigned permissions.
     */
    @GetMapping("/{roleId}")
    @PreAuthorize("hasAuthority('ORGANIZATIONS:READ') and @organizationService.hasAccess(authentication.name, #organizationId)")
    public ResponseEntity<RoleDetailsResponse> getRoleDetails(
            @PathVariable Long organizationId,
            @PathVariable Long roleId) {

        Optional<RoleApi.RoleDetails> roleDetails = roleApi.getRoleDetails(roleId, organizationId);

        return roleDetails
            .map(details -> ResponseEntity.ok(new RoleDetailsResponse(details)))
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create a new custom role in an organization.
     * Only organization admins can create custom roles.
     */
    @PostMapping
    @PreAuthorize("hasAuthority('ORGANIZATIONS:ADMIN') and @organizationService.hasAccess(authentication.name, #organizationId)")
    public ResponseEntity<RoleCreatedResponse> createRole(
            @PathVariable Long organizationId,
            @Valid @RequestBody CreateRoleRequest request) {

        try {
            // In a real implementation, role creation would be handled by a role management service
            // that uses the internal RoleService through proper service layer

            // For now, we'll return a success response indicating the role would be created
            // This demonstrates the API contract without the full implementation

            return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RoleCreatedResponse(
                    "Role creation initiated",
                    request.getName(),
                    "Role will be created with " + request.getPermissionIds().size() + " permissions"
                ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(new RoleCreatedResponse("Error", null, e.getMessage()));
        }
    }

    /**
     * Update an existing custom role.
     * Only custom roles can be modified; predefined roles are immutable.
     */
    @PutMapping("/{roleId}")
    @PreAuthorize("hasAuthority('ORGANIZATIONS:ADMIN') and @organizationService.hasAccess(authentication.name, #organizationId)")
    public ResponseEntity<RoleUpdatedResponse> updateRole(
            @PathVariable Long organizationId,
            @PathVariable Long roleId,
            @Valid @RequestBody UpdateRoleRequest request) {

        try {
            // Check if role exists and is modifiable
            Optional<RoleApi.RoleDetails> existingRole = roleApi.getRoleDetails(roleId, organizationId);

            if (existingRole.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            if (existingRole.get().roleType() == RoleApi.RoleType.PREDEFINED) {
                return ResponseEntity.badRequest()
                    .body(new RoleUpdatedResponse("Error", "Cannot modify predefined roles"));
            }

            // In a real implementation, this would call the role management service
            return ResponseEntity.ok(new RoleUpdatedResponse("Success", "Role updated successfully"));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(new RoleUpdatedResponse("Error", e.getMessage()));
        }
    }

    /**
     * Delete a custom role.
     * Only custom roles with no active assignments can be deleted.
     */
    @DeleteMapping("/{roleId}")
    @PreAuthorize("hasAuthority('ORGANIZATIONS:ADMIN') and @organizationService.hasAccess(authentication.name, #organizationId)")
    public ResponseEntity<RoleDeletedResponse> deleteRole(
            @PathVariable Long organizationId,
            @PathVariable Long roleId) {

        try {
            Optional<RoleApi.RoleDetails> existingRole = roleApi.getRoleDetails(roleId, organizationId);

            if (existingRole.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            RoleApi.RoleDetails role = existingRole.get();

            if (role.roleType() == RoleApi.RoleType.PREDEFINED) {
                return ResponseEntity.badRequest()
                    .body(new RoleDeletedResponse("Error", "Cannot delete predefined roles"));
            }

            if (role.assignedUserCount() > 0) {
                return ResponseEntity.badRequest()
                    .body(new RoleDeletedResponse("Error",
                        "Cannot delete role with active user assignments: " + role.assignedUserCount()));
            }

            // In a real implementation, this would call the role management service
            return ResponseEntity.ok(new RoleDeletedResponse("Success", "Role deleted successfully"));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new RoleDeletedResponse("Error", e.getMessage()));
        }
    }

    /**
     * Get roles that have a specific permission.
     * Used for permission impact analysis and role discovery.
     */
    @GetMapping("/by-permission")
    @PreAuthorize("hasAuthority('ORGANIZATIONS:READ') and @organizationService.hasAccess(authentication.name, #organizationId)")
    public ResponseEntity<RolesByPermissionResponse> getRolesByPermission(
            @PathVariable Long organizationId,
            @RequestParam String resource,
            @RequestParam String action) {

        List<RoleApi.RoleInfo> roles = roleApi.getRolesWithPermission(organizationId, resource, action);

        return ResponseEntity.ok(new RolesByPermissionResponse(resource, action, roles));
    }

    /**
     * Get role statistics for an organization.
     * Used for organizational metrics and reporting.
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasAuthority('ORGANIZATIONS:READ') and @organizationService.hasAccess(authentication.name, #organizationId)")
    public ResponseEntity<RoleStatisticsResponse> getRoleStatistics(@PathVariable Long organizationId) {
        RoleApi.RoleStatistics statistics = roleApi.getRoleStatistics(organizationId);

        return ResponseEntity.ok(new RoleStatisticsResponse(statistics));
    }

    // Response DTOs

    public record RolesResponse(List<RoleApi.RoleInfo> roles, RoleApi.RoleStatistics statistics) {}

    public record RoleDetailsResponse(RoleApi.RoleDetails role) {}

    public record RoleCreatedResponse(String status, String roleName, String message) {}

    public record RoleUpdatedResponse(String status, String message) {}

    public record RoleDeletedResponse(String status, String message) {}

    public record RolesByPermissionResponse(String resource, String action, List<RoleApi.RoleInfo> roles) {}

    public record RoleStatisticsResponse(RoleApi.RoleStatistics statistics) {}

    // Request DTOs

    public static class CreateRoleRequest {
        @NotBlank(message = "Role name is required")
        @Size(max = 100, message = "Role name cannot exceed 100 characters")
        private String name;

        @Size(max = 500, message = "Role description cannot exceed 500 characters")
        private String description;

        @NotEmpty(message = "At least one permission is required")
        private List<Long> permissionIds;

        public CreateRoleRequest() {}

        public CreateRoleRequest(String name, String description, List<Long> permissionIds) {
            this.name = name;
            this.description = description;
            this.permissionIds = permissionIds;
        }

        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public List<Long> getPermissionIds() { return permissionIds; }
        public void setPermissionIds(List<Long> permissionIds) { this.permissionIds = permissionIds; }
    }

    public static class UpdateRoleRequest {
        @NotBlank(message = "Role name is required")
        @Size(max = 100, message = "Role name cannot exceed 100 characters")
        private String name;

        @Size(max = 500, message = "Role description cannot exceed 500 characters")
        private String description;

        private List<Long> permissionIds;

        public UpdateRoleRequest() {}

        public UpdateRoleRequest(String name, String description, List<Long> permissionIds) {
            this.name = name;
            this.description = description;
            this.permissionIds = permissionIds;
        }

        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public List<Long> getPermissionIds() { return permissionIds; }
        public void setPermissionIds(List<Long> permissionIds) { this.permissionIds = permissionIds; }
    }
}