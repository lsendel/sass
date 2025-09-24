package com.platform.user.api;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.platform.shared.security.PlatformUserPrincipal;
import com.platform.user.api.UserDto.PagedUserResponse;
import com.platform.user.api.UserDto.UpdatePreferencesRequest;
import com.platform.user.api.UserDto.UpdateProfileRequest;
import com.platform.user.api.UserDto.UserResponse;
import com.platform.user.api.UserDto.UserStatistics;

@RestController
@RequestMapping("/api/v1/users")
@PreAuthorize("isAuthenticated()")
public class UserController {

  private final UserManagementServiceInterface userManagementService;

  public UserController(UserManagementServiceInterface userManagementService) {
    this.userManagementService = userManagementService;
  }

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

  @GetMapping("/{userId}")
  @PreAuthorize("hasRole('ADMIN') || #userId == principal.userId")
  public ResponseEntity<UserResponse> getUserById(
      @AuthenticationPrincipal PlatformUserPrincipal principal, @PathVariable UUID userId) {
    Optional<UserResponse> user = userManagementService.findById(userId);
    return user.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
  }

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

  @DeleteMapping("/me")
  public ResponseEntity<Void> deleteCurrentUser(
      @AuthenticationPrincipal PlatformUserPrincipal principal) {
    if (principal == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    userManagementService.deleteUser(principal.getUserId());
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/search")
  public ResponseEntity<List<UserResponse>> searchUsers(
      @RequestParam String name, @AuthenticationPrincipal PlatformUserPrincipal principal) {
    if (principal == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    List<UserResponse> users = userManagementService.searchUsersByName(name);
    return ResponseEntity.ok(users);
  }

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

  @GetMapping("/statistics")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<UserStatistics> getUserStatistics() {
    UserStatistics stats = userManagementService.getUserStatistics();
    return ResponseEntity.ok(stats);
  }

  @GetMapping("/count")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Long> countActiveUsers() {
    long count = userManagementService.countActiveUsers();
    return ResponseEntity.ok(count);
  }

  @GetMapping("/providers/{provider}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<UserResponse>> getUsersByProvider(@PathVariable String provider) {
    List<UserResponse> users = userManagementService.findUsersByProvider(provider);
    return ResponseEntity.ok(users);
  }

  @PostMapping("/{userId}/restore")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<UserResponse> restoreUser(@PathVariable UUID userId) {
    UserResponse restoredUser = userManagementService.restoreUser(userId);
    return ResponseEntity.ok(restoredUser);
  }
}

