package com.platform.user.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

/**
 * REST controller for user role assignment operations in the RBAC system.
 * Provides endpoints for assigning, removing, and managing user role assignments.
 *
 * Following Spring Modulith principles, this controller only uses the public UserRoleApi
 * and does not directly access internal implementation details.
 */
@RestController
@RequestMapping("/api/organizations/{organizationId}/users")
@CrossOrigin(origins = "${app.cors.allowed-origins:http://localhost:3000}")
public class UserRoleController {

    private final UserRoleApi userRoleApi;
    private final RoleApi roleApi;

    @Autowired
    public UserRoleController(UserRoleApi userRoleApi, RoleApi roleApi) {
        this.userRoleApi = userRoleApi;
        this.roleApi = roleApi;
    }

    /**
     * Get all role assignments for a user in an organization.
     * Shows active, expired, and removed assignments with full details.
     */
    @GetMapping("/{userId}/roles")
    @PreAuthorize("hasAuthority('USERS:READ') and @organizationService.hasAccess(authentication.name, #organizationId)")
    public ResponseEntity<UserRoleAssignmentsResponse> getUserRoleAssignments(
            @PathVariable Long organizationId,
            @PathVariable Long userId) {

        List<UserRoleApi.UserRoleAssignment> assignments =
            userRoleApi.getUserRoleAssignments(userId, organizationId);

        return ResponseEntity.ok(new UserRoleAssignmentsResponse(userId, assignments));
    }

    /**
     * Assign a role to a user with optional expiration.
     * Creates a new role assignment with audit tracking.
     */
    @PostMapping("/{userId}/roles")
    @PreAuthorize("hasAuthority('USERS:WRITE') and @organizationService.hasAccess(authentication.name, #organizationId)")
    public ResponseEntity<RoleAssignmentResponse> assignRoleToUser(
            @PathVariable Long organizationId,
            @PathVariable Long userId,
            @Valid @RequestBody AssignRoleRequest request) {

        try {
            // Get current user ID for audit tracking
            Long assignedBy = getCurrentUserId();

            UserRoleApi.UserRoleAssignment assignment = userRoleApi.assignRoleToUser(
                userId, request.getRoleId(), organizationId, assignedBy, request.getExpiresAt()
            );

            return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RoleAssignmentResponse("Success", "Role assigned successfully", assignment));

        } catch (UserRoleApi.DuplicateRoleAssignmentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new RoleAssignmentResponse("Error", e.getMessage(), null));
        } catch (UserRoleApi.RoleAssignmentLimitExceededException e) {
            return ResponseEntity.badRequest()
                .body(new RoleAssignmentResponse("Error", e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new RoleAssignmentResponse("Error", "Failed to assign role: " + e.getMessage(), null));
        }
    }

    /**
     * Remove a role assignment from a user.
     * Marks the assignment as removed with audit information.
     */
    @DeleteMapping("/{userId}/roles/{roleId}")
    @PreAuthorize("hasAuthority('USERS:WRITE') and @organizationService.hasAccess(authentication.name, #organizationId)")
    public ResponseEntity<RoleRemovalResponse> removeRoleFromUser(
            @PathVariable Long organizationId,
            @PathVariable Long userId,
            @PathVariable Long roleId) {

        try {
            // Get current user ID for audit tracking
            Long removedBy = getCurrentUserId();

            userRoleApi.removeRoleFromUser(userId, roleId, organizationId, removedBy);

            return ResponseEntity.ok(new RoleRemovalResponse("Success", "Role removed successfully"));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new RoleRemovalResponse("Error", "Failed to remove role: " + e.getMessage()));
        }
    }

    /**
     * Extend the expiration of a role assignment.
     * Updates the expiration date for temporary role assignments.
     */
    @PutMapping("/{userId}/roles/{roleId}/extend")
    @PreAuthorize("hasAuthority('USERS:WRITE') and @organizationService.hasAccess(authentication.name, #organizationId)")
    public ResponseEntity<RoleExtensionResponse> extendRoleAssignment(
            @PathVariable Long organizationId,
            @PathVariable Long userId,
            @PathVariable Long roleId,
            @Valid @RequestBody ExtendRoleRequest request) {

        try {
            // Get current user ID for audit tracking
            Long updatedBy = getCurrentUserId();

            userRoleApi.extendRoleAssignment(userId, roleId, organizationId,
                request.getNewExpirationDate(), updatedBy);

            return ResponseEntity.ok(new RoleExtensionResponse("Success",
                "Role assignment extended until " + request.getNewExpirationDate()));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new RoleExtensionResponse("Error", "Failed to extend role assignment: " + e.getMessage()));
        }
    }

    /**
     * Get all users assigned to a specific role.
     * Used for role management and user listing.
     */
    @GetMapping("/by-role/{roleId}")
    @PreAuthorize("hasAuthority('USERS:READ') and @organizationService.hasAccess(authentication.name, #organizationId)")
    public ResponseEntity<RoleAssignmentsResponse> getRoleAssignments(
            @PathVariable Long organizationId,
            @PathVariable Long roleId) {

        // Verify role exists in organization
        var roleDetails = roleApi.getRoleDetails(roleId, organizationId);
        if (roleDetails.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        List<UserRoleApi.UserRoleAssignment> assignments =
            userRoleApi.getRoleAssignments(roleId, organizationId);

        return ResponseEntity.ok(new RoleAssignmentsResponse(roleId, roleDetails.get().name(), assignments));
    }

    /**
     * Check if a user has an active role assignment.
     * Used for quick role membership validation.
     */
    @GetMapping("/{userId}/roles/{roleId}/check")
    @PreAuthorize("hasAuthority('USERS:READ') and @organizationService.hasAccess(authentication.name, #organizationId)")
    public ResponseEntity<RoleCheckResponse> checkRoleAssignment(
            @PathVariable Long organizationId,
            @PathVariable Long userId,
            @PathVariable Long roleId) {

        boolean hasAssignment = userRoleApi.hasActiveRoleAssignment(userId, roleId, organizationId);

        return ResponseEntity.ok(new RoleCheckResponse(userId, roleId, hasAssignment));
    }

    /**
     * Get role assignments expiring within specified days.
     * Used for proactive role expiration management and notifications.
     */
    @GetMapping("/roles/expiring")
    @PreAuthorize("hasAuthority('USERS:READ') and @organizationService.hasAccess(authentication.name, #organizationId)")
    public ResponseEntity<ExpiringAssignmentsResponse> getExpiringAssignments(
            @PathVariable Long organizationId,
            @RequestParam(defaultValue = "7") int daysAhead) {

        List<UserRoleApi.UserRoleAssignment> expiringAssignments =
            userRoleApi.getExpiringAssignments(organizationId, daysAhead);

        return ResponseEntity.ok(new ExpiringAssignmentsResponse(daysAhead, expiringAssignments));
    }

    /**
     * Get user role assignment statistics for an organization.
     * Used for organizational metrics and reporting.
     */
    @GetMapping("/roles/statistics")
    @PreAuthorize("hasAuthority('USERS:READ') and @organizationService.hasAccess(authentication.name, #organizationId)")
    public ResponseEntity<UserRoleStatisticsResponse> getUserRoleStatistics(@PathVariable Long organizationId) {
        UserRoleApi.UserRoleStatistics statistics = userRoleApi.getUserRoleStatistics(organizationId);

        return ResponseEntity.ok(new UserRoleStatisticsResponse(statistics));
    }

    // Helper methods

    private Long getCurrentUserId() {
        // In a real implementation, this would extract user ID from SecurityContext
        return 1L; // TODO: Implement proper user ID extraction from JWT/session
    }

    // Response DTOs

    public record UserRoleAssignmentsResponse(Long userId, List<UserRoleApi.UserRoleAssignment> assignments) {}

    public record RoleAssignmentResponse(String status, String message, UserRoleApi.UserRoleAssignment assignment) {}

    public record RoleRemovalResponse(String status, String message) {}

    public record RoleExtensionResponse(String status, String message) {}

    public record RoleAssignmentsResponse(Long roleId, String roleName, List<UserRoleApi.UserRoleAssignment> assignments) {}

    public record RoleCheckResponse(Long userId, Long roleId, boolean hasActiveAssignment) {}

    public record ExpiringAssignmentsResponse(int daysAhead, List<UserRoleApi.UserRoleAssignment> assignments) {}

    public record UserRoleStatisticsResponse(UserRoleApi.UserRoleStatistics statistics) {}

    // Request DTOs

    public static class AssignRoleRequest {
        @NotNull(message = "Role ID is required")
        private Long roleId;

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        private LocalDateTime expiresAt;

        public AssignRoleRequest() {}

        public AssignRoleRequest(Long roleId, LocalDateTime expiresAt) {
            this.roleId = roleId;
            this.expiresAt = expiresAt;
        }

        // Getters and setters
        public Long getRoleId() { return roleId; }
        public void setRoleId(Long roleId) { this.roleId = roleId; }
        public LocalDateTime getExpiresAt() { return expiresAt; }
        public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    }

    public static class ExtendRoleRequest {
        @NotNull(message = "New expiration date is required")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        private LocalDateTime newExpirationDate;

        public ExtendRoleRequest() {}

        public ExtendRoleRequest(LocalDateTime newExpirationDate) {
            this.newExpirationDate = newExpirationDate;
        }

        // Getters and setters
        public LocalDateTime getNewExpirationDate() { return newExpirationDate; }
        public void setNewExpirationDate(LocalDateTime newExpirationDate) {
            this.newExpirationDate = newExpirationDate;
        }
    }
}