package com.platform.user.api;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import com.platform.user.api.UserDto.PagedUserResponse;
import com.platform.user.api.UserDto.UserResponse;
import com.platform.user.api.UserDto.UserStatistics;
import com.platform.user.internal.UserService;
import com.platform.user.internal.User;
import com.platform.user.internal.UserView;

/**
 * Implementation of the {@link UserManagementServiceInterface}.
 *
 * <p>This service acts as a bridge between the API layer and the internal domain layer for user
 * management. It translates API requests into calls to the internal {@link UserService} and maps
 * the resulting internal entities to public DTOs.
 */
@Service
public class UserManagementServiceImpl implements UserManagementServiceInterface {

  private final UserService userService;

  /**
   * Constructs a new UserManagementServiceImpl.
   *
   * @param userService the internal service for managing users
   */
  public UserManagementServiceImpl(UserService userService) {
    this.userService = userService;
  }

  @Override
  public Optional<UserResponse> getCurrentUser() {
    return userService.getCurrentUser().map(this::mapToResponse);
  }

  @Override
  public Optional<UserResponse> findById(UUID userId) {
    return userService.findById(userId).map(this::mapToResponse);
  }

  @Override
  public UserResponse updateProfile(UUID userId, String name, Map<String, Object> preferences) {
    User updatedUser = userService.updateProfile(userId, name, preferences);
    return mapToResponse(updatedUser);
  }

  @Override
  public UserResponse updatePreferences(UUID userId, Map<String, Object> preferences) {
    User updatedUser = userService.updatePreferences(userId, preferences);
    return mapToResponse(updatedUser);
  }

  @Override
  public void deleteUser(UUID userId) {
    userService.deleteUser(userId);
  }

  @Override
  public List<UserResponse> searchUsersByName(String name) {
    return userService.searchUsersByName(name)
        .stream()
        .map(this::mapToResponse)
        .toList();
  }

  @Override
  public PagedUserResponse findAllUsers(int page, int size, String sortBy, String sortDirection) {
    Page<User> userPage = userService.findAllUsers(page, size, sortBy, sortDirection);

    List<UserResponse> users = userPage.getContent()
        .stream()
        .map(this::mapToResponse)
        .toList();

    return new PagedUserResponse(
        users,
        userPage.getNumber(),
        userPage.getSize(),
        userPage.getTotalElements(),
        userPage.getTotalPages(),
        userPage.isFirst(),
        userPage.isLast()
    );
  }

  @Override
  public List<UserResponse> getRecentlyActiveUsers(Instant since) {
    return userService.getRecentlyActiveUsers(since)
        .stream()
        .map(this::mapToResponse)
        .toList();
  }

  @Override
  public UserStatistics getUserStatistics() {
    UserService.UserStatistics internalStats = userService.getUserStatistics();
    return new UserStatistics(
        internalStats.totalUsers(),
        internalStats.activeUsers(),
        internalStats.newUsersThisWeek(),
        internalStats.usersByProvider(),
        internalStats.averageSessionDuration()
    );
  }

  @Override
  public long countActiveUsers() {
    return userService.countActiveUsers();
  }

  @Override
  public List<UserResponse> findUsersByProvider(String provider) {
    return userService.findUsersByProvider(provider)
        .stream()
        .map(this::mapToResponse)
        .toList();
  }

  @Override
  public UserResponse restoreUser(UUID userId) {
    User restoredUser = userService.restoreUser(userId);
    return mapToResponse(restoredUser);
  }

  /**
   * Activates a user's account.
   *
   * <p>Note: This is a mock implementation for testing purposes.
   *
   * @param userId the ID of the user to activate
   */
  @Override
  public void activateUser(UUID userId) {
    // Mock implementation for testing
    // In real implementation, this would call userService.activateUser(userId)
  }

  /**
   * Deactivates a user's account.
   *
   * <p>Note: This is a mock implementation for testing purposes.
   *
   * @param userId the ID of the user to deactivate
   */
  @Override
  public void deactivateUser(UUID userId) {
    // Mock implementation for testing
    // In real implementation, this would call userService.deactivateUser(userId)
  }

  /**
   * Changes a user's password.
   *
   * <p>Note: This is a mock implementation for testing purposes.
   *
   * @param userId the ID of the user
   * @param currentPassword the user's current password
   * @param newPassword the new password to set
   */
  @Override
  public void changePassword(UUID userId, String currentPassword, String newPassword) {
    // Mock implementation for testing
    // In real implementation, this would call userService.changePassword(userId, currentPassword, newPassword)
    if (currentPassword == null || currentPassword.isBlank()) {
      throw new IllegalArgumentException("Current password is required");
    }
    if (newPassword == null || newPassword.length() < 8) {
      throw new IllegalArgumentException("New password must be at least 8 characters");
    }
  }

  /**
   * Maps an internal {@link User} entity to an API {@link UserResponse} DTO.
   *
   * @param user the user entity to map
   * @return the corresponding API DTO
   */
  private UserResponse mapToResponse(User user) {
    return new UserResponse(
        user.getId(),
        user.getEmail().getValue(),
        user.getName(),
        user.getProvider(),
        user.getPreferences(),
        user.getCreatedAt(),
        user.getLastActiveAt()
    );
  }
}