package com.platform.user.api;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.platform.user.api.UserDto.PagedUserResponse;
import com.platform.user.api.UserDto.UserResponse;
import com.platform.user.api.UserDto.UserStatistics;

/**
 * Service interface for user management operations.
 * This interface provides the API layer with access to user functionality
 * without depending on internal implementation details.
 */
public interface UserManagementServiceInterface {

  /**
   * Gets the current authenticated user.
   *
   * @return the current user if authenticated, empty otherwise
   */
  Optional<UserResponse> getCurrentUser();

  /**
   * Finds a user by ID.
   *
   * @param userId the user ID
   * @return the user if found, empty otherwise
   */
  Optional<UserResponse> findById(UUID userId);

  /**
   * Updates a user's profile.
   *
   * @param userId the user ID
   * @param name the new name
   * @param preferences the new preferences
   * @return the updated user
   */
  UserResponse updateProfile(UUID userId, String name, Map<String, Object> preferences);

  /**
   * Updates a user's preferences.
   *
   * @param userId the user ID
   * @param preferences the new preferences
   * @return the updated user
   */
  UserResponse updatePreferences(UUID userId, Map<String, Object> preferences);

  /**
   * Deletes a user (soft delete).
   *
   * @param userId the user ID
   */
  void deleteUser(UUID userId);

  /**
   * Searches users by name.
   *
   * @param name the name to search for
   * @return list of matching users
   */
  List<UserResponse> searchUsersByName(String name);

  /**
   * Finds all users with pagination.
   *
   * @param page the page number
   * @param size the page size
   * @param sortBy the field to sort by
   * @param sortDirection the sort direction
   * @return paginated user results
   */
  PagedUserResponse findAllUsers(int page, int size, String sortBy, String sortDirection);

  /**
   * Gets recently active users.
   *
   * @param since the timestamp to filter from
   * @return list of recently active users
   */
  List<UserResponse> getRecentlyActiveUsers(Instant since);

  /**
   * Gets user statistics.
   *
   * @return user statistics
   */
  UserStatistics getUserStatistics();

  /**
   * Counts active users.
   *
   * @return count of active users
   */
  long countActiveUsers();

  /**
   * Finds users by authentication provider.
   *
   * @param provider the provider name
   * @return list of users using the provider
   */
  List<UserResponse> findUsersByProvider(String provider);

  /**
   * Restores a deleted user.
   *
   * @param userId the user ID
   * @return the restored user
   */
  UserResponse restoreUser(UUID userId);
}