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
 * Implementation of UserManagementServiceInterface that bridges the API and internal layers.
 * This service converts between internal entities and API DTOs.
 */
@Service
public class UserManagementServiceImpl implements UserManagementServiceInterface {

  private final UserService userService;

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
        0L, // activeUsers - not available in internal stats
        internalStats.newUsersThisWeek(),
        internalStats.usersByProvider(),
        0.0 // averageSessionDuration - not available in internal stats
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
   * Maps internal User entity to API UserResponse DTO.
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