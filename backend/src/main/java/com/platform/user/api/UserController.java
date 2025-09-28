package com.platform.user.api;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.platform.shared.security.PlatformUserPrincipal;
import com.platform.user.api.UserDto.ChangePasswordRequest;
import com.platform.user.api.UserDto.CreateUserRequest;
import com.platform.user.api.UserDto.OrganizationResponse;
import com.platform.user.api.UserDto.PagedUserResponse;
import com.platform.user.api.UserDto.UpdatePreferencesRequest;
import com.platform.user.api.UserDto.UpdateProfileRequest;
import com.platform.user.api.UserDto.UserResponse;
import com.platform.user.api.UserDto.UserStatistics;

/**
 * REST controller for managing users.
 *
 * <p>This controller provides endpoints for retrieving and managing user information, including the
 * current user's profile, as well as administrative endpoints for managing all users in the system.
 */
@RestController
@RequestMapping("/api/v1/users")
@PreAuthorize("isAuthenticated()")
public class UserController {

  private final UserManagementServiceInterface userManagementService;

  /**
   * Constructs a new UserController.
   *
   * @param userManagementService the service for managing users
   */
  public UserController(UserManagementServiceInterface userManagementService) {
    this.userManagementService = userManagementService;
  }

  /**
   * Retrieves the details of the currently authenticated user.
   *
   * @param principal the authenticated user principal
   * @return a {@link ResponseEntity} containing the {@link UserResponse} for the current user
   */
  @GetMapping("/me")
  public ResponseEntity<UserResponse> getCurrentUser(
      @AuthenticationPrincipal PlatformUserPrincipal principal) {
    if (principal == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    Optional<UserResponse> user = userManagementService.getCurrentUser();
    return user.map(ResponseEntity::ok)
        .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
  }

  /**
   * Retrieves a user by their ID.
   *
   * @param principal the authenticated user principal
   * @param userId the ID of the user to retrieve
   * @return a {@link ResponseEntity} containing the {@link UserResponse} if found, otherwise a 404
   *     Not Found response
   */
  @GetMapping("/{userId}")
  @PreAuthorize("hasRole('ADMIN') || #userId == principal.userId")
  public ResponseEntity<UserResponse> getUserById(
      @AuthenticationPrincipal PlatformUserPrincipal principal, @PathVariable UUID userId) {
    Optional<UserResponse> user = userManagementService.findById(userId);
    return user.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
  }

  /**
   * Updates the profile of the currently authenticated user.
   *
   * @param principal the authenticated user principal
   * @param request the request body containing the updated profile information
   * @return a {@link ResponseEntity} with the updated {@link UserResponse}
   */
  @PutMapping("/me/profile")
  public ResponseEntity<UserResponse> updateProfile(
      @AuthenticationPrincipal PlatformUserPrincipal principal,
      @Valid @RequestBody UpdateProfileRequest request) {
    if (principal == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    UserResponse updatedUser =
        userManagementService.updateProfile(principal.getUserId(), request.name(), request.preferences());

    return ResponseEntity.ok(updatedUser);
  }

  /**
   * Updates the preferences of the currently authenticated user.
   *
   * @param principal the authenticated user principal
   * @param request the request body containing the updated preferences
   * @return a {@link ResponseEntity} with the updated {@link UserResponse}
   */
  @PutMapping("/me/preferences")
  public ResponseEntity<UserResponse> updatePreferences(
      @AuthenticationPrincipal PlatformUserPrincipal principal,
      @Valid @RequestBody UpdatePreferencesRequest request) {
    if (principal == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    UserResponse updatedUser =
        userManagementService.updatePreferences(principal.getUserId(), request.preferences());
    return ResponseEntity.ok(updatedUser);
  }

  /**
   * Deletes the currently authenticated user.
   *
   * @param principal the authenticated user principal
   * @return a {@link ResponseEntity} with no content
   */
  @DeleteMapping("/me")
  public ResponseEntity<Void> deleteCurrentUser(
      @AuthenticationPrincipal PlatformUserPrincipal principal) {
    if (principal == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    userManagementService.deleteUser(principal.getUserId());
    return ResponseEntity.noContent().build();
  }

  /**
   * Searches for users by name.
   *
   * @param name the name to search for
   * @param principal the authenticated user principal
   * @return a {@link ResponseEntity} containing a list of matching {@link UserResponse}s
   */
  @GetMapping("/search")
  public ResponseEntity<List<UserResponse>> searchUsers(
      @RequestParam String name, @AuthenticationPrincipal PlatformUserPrincipal principal) {
    if (principal == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    List<UserResponse> users = userManagementService.searchUsersByName(name);
    return ResponseEntity.ok(users);
  }

  /**
   * Retrieves a paginated list of all users.
   *
   * <p>This is an administrative endpoint.
   *
   * @param page the page number to retrieve
   * @param size the number of users per page
   * @param sortBy the field to sort by
   * @param sortDirection the sort direction ("asc" or "desc")
   * @return a {@link ResponseEntity} containing a {@link PagedUserResponse}
   */
  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<PagedUserResponse> getAllUsers(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "createdAt") String sortBy,
      @RequestParam(defaultValue = "desc") String sortDirection) {

    PagedUserResponse response =
        userManagementService.findAllUsers(page, size, sortBy, sortDirection);
    return ResponseEntity.ok(response);
  }

  /**
   * Retrieves a list of recently active users.
   *
   * <p>This is an administrative endpoint.
   *
   * @param since an ISO 8601 timestamp to specify the start time for recent activity
   * @return a {@link ResponseEntity} containing a list of recently active {@link UserResponse}s
   */
  @GetMapping("/recent")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<UserResponse>> getRecentUsers(@RequestParam String since) {
    try {
      Instant sinceInstant = Instant.parse(since);
      List<UserResponse> users = userManagementService.getRecentlyActiveUsers(sinceInstant);
      return ResponseEntity.ok(users);
    } catch (Exception e) {
      return ResponseEntity.badRequest().build();
    }
  }

  /**
   * Retrieves user statistics.
   *
   * <p>This is an administrative endpoint.
   *
   * @return a {@link ResponseEntity} containing {@link UserStatistics}
   */
  @GetMapping("/statistics")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<UserStatistics> getUserStatistics() {
    UserStatistics stats = userManagementService.getUserStatistics();
    return ResponseEntity.ok(stats);
  }

  /**
   * Counts the total number of active users.
   *
   * <p>This is an administrative endpoint.
   *
   * @return a {@link ResponseEntity} containing the count of active users
   */
  @GetMapping("/count")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Long> countActiveUsers() {
    long count = userManagementService.countActiveUsers();
    return ResponseEntity.ok(count);
  }

  /**
   * Retrieves all users for a specific authentication provider.
   *
   * <p>This is an administrative endpoint.
   *
   * @param provider the name of the provider
   * @return a {@link ResponseEntity} containing a list of {@link UserResponse}s
   */
  @GetMapping("/providers/{provider}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<UserResponse>> getUsersByProvider(@PathVariable String provider) {
    List<UserResponse> users = userManagementService.findUsersByProvider(provider);
    return ResponseEntity.ok(users);
  }

  /**
   * Restores a soft-deleted user.
   *
   * <p>This is an administrative endpoint.
   *
   * @param userId the ID of the user to restore
   * @return a {@link ResponseEntity} with the restored {@link UserResponse}
   */
  @PostMapping("/{userId}/restore")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<UserResponse> restoreUser(@PathVariable UUID userId) {
    UserResponse restoredUser = userManagementService.restoreUser(userId);
    return ResponseEntity.ok(restoredUser);
  }

  /**
   * Creates a new user.
   *
   * <p>This is an administrative endpoint and uses a mock implementation for testing.
   *
   * @param request the request body containing the new user's details
   * @return a {@link ResponseEntity} with the created {@link UserResponse}
   */
  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
    // Mock user creation for testing
    UUID newUserId = UUID.randomUUID();
    UserResponse mockUser = new UserResponse(
        newUserId,
        request.email(),
        request.name(),
        "oauth2", // provider
        null, // preferences
        Instant.now(), // createdAt
        Instant.now() // lastActiveAt
    );
    return ResponseEntity.status(HttpStatus.CREATED).body(mockUser);
  }

  /**
   * Updates the profile of the currently authenticated user.
   *
   * @param principal the authenticated user principal
   * @param request the request body containing the updated profile information
   * @return a {@link ResponseEntity} with the updated {@link UserResponse}
   */
  @PutMapping("/me")
  public ResponseEntity<UserResponse> updateCurrentUser(
      @AuthenticationPrincipal PlatformUserPrincipal principal,
      @Valid @RequestBody UpdateProfileRequest request) {
    if (principal == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    UserResponse updatedUser =
        userManagementService.updateProfile(principal.getUserId(), request.name(), request.preferences());

    return ResponseEntity.ok(updatedUser);
  }

  /**
   * Activates a user's account.
   *
   * <p>This is an administrative endpoint and uses a mock implementation for testing.
   *
   * @param userId the ID of the user to activate
   * @return a {@link ResponseEntity} with no content
   */
  @PostMapping("/{userId}/activate")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> activateUser(@PathVariable UUID userId) {
    // Mock user activation for testing
    userManagementService.activateUser(userId);
    return ResponseEntity.ok().build();
  }

  /**
   * Deactivates a user's account.
   *
   * <p>This is an administrative endpoint and uses a mock implementation for testing.
   *
   * @param userId the ID of the user to deactivate
   * @return a {@link ResponseEntity} with no content
   */
  @PostMapping("/{userId}/deactivate")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> deactivateUser(@PathVariable UUID userId) {
    // Mock user deactivation for testing
    userManagementService.deactivateUser(userId);
    return ResponseEntity.ok().build();
  }

  /**
   * Retrieves the organizations for the currently authenticated user.
   *
   * <p>This endpoint uses a mock implementation for testing.
   *
   * @param principal the authenticated user principal
   * @return a {@link ResponseEntity} containing a list of {@link OrganizationResponse}s
   */
  @GetMapping("/me/organizations")
  public ResponseEntity<List<OrganizationResponse>> getCurrentUserOrganizations(
      @AuthenticationPrincipal PlatformUserPrincipal principal) {
    if (principal == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    // Mock organization data for testing
    if (principal.getOrganizationId() != null) {
      OrganizationResponse mockOrg = new OrganizationResponse(
          principal.getOrganizationId(),
          "Test Organization",
          principal.getOrganizationSlug() != null ? principal.getOrganizationSlug() : "test-org",
          principal.getUserId(), // ownerId
          Map.of(), // settings
          Instant.now(), // createdAt
          Instant.now() // updatedAt
      );
      return ResponseEntity.ok(List.of(mockOrg));
    }
    return ResponseEntity.ok(List.of());
  }

  /**
   * Changes the password for the currently authenticated user.
   *
   * <p>This endpoint uses a mock implementation for testing.
   *
   * @param principal the authenticated user principal
   * @param request the request body containing the current and new passwords
   * @return a {@link ResponseEntity} with no content
   */
  @PostMapping("/me/password")
  public ResponseEntity<Void> changePassword(
      @AuthenticationPrincipal PlatformUserPrincipal principal,
      @Valid @RequestBody ChangePasswordRequest request) {
    if (principal == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    // Validate current password
    if (request.currentPassword() == null || request.currentPassword().isBlank()) {
      return ResponseEntity.badRequest().build();
    }
    // Mock password change for testing
    userManagementService.changePassword(principal.getUserId(), request.currentPassword(), request.newPassword());
    return ResponseEntity.ok().build();
  }

}

