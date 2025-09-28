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
 * Defines the contract for managing users.
 *
 * <p>This service interface provides a high-level API for all user-related operations, serving as an
 * abstraction layer between the API controllers and the internal implementation details.
 */
public interface UserManagementServiceInterface {

  /**
   * Retrieves the currently authenticated user.
   *
   * @return an {@link Optional} containing the {@link UserResponse} for the current user if
   *     authenticated, otherwise an empty {@link Optional}
   */
  Optional<UserResponse> getCurrentUser();

  /**
   * Finds a user by their ID.
   *
   * @param userId the ID of the user
   * @return an {@link Optional} containing the {@link UserResponse} if found, otherwise an empty
   *     {@link Optional}
   */
  Optional<UserResponse> findById(UUID userId);

  /**
   * Updates a user's profile.
   *
   * @param userId the ID of the user to update
   * @param name the new name for the user
   * @param preferences a map of new preferences for the user
   * @return a {@link UserResponse} representing the updated user
   */
  UserResponse updateProfile(UUID userId, String name, Map<String, Object> preferences);

  /**
   * Updates a user's preferences.
   *
   * @param userId the ID of the user to update
   * @param preferences a map of new preferences for the user
   * @return a {@link UserResponse} representing the updated user
   */
  UserResponse updatePreferences(UUID userId, Map<String, Object> preferences);

  /**
   * Deletes a user (soft delete).
   *
   * @param userId the ID of the user to delete
   */
  void deleteUser(UUID userId);

  /**
   * Searches for users by name.
   *
   * @param name the name to search for
   * @return a list of {@link UserResponse}s matching the search criteria
   */
  List<UserResponse> searchUsersByName(String name);

  /**
   * Retrieves a paginated list of all users.
   *
   * @param page the page number to retrieve
   * @param size the number of users per page
   * @param sortBy the field to sort by
   * @param sortDirection the sort direction ("asc" or "desc")
   * @return a {@link PagedUserResponse} containing the paginated results
   */
  PagedUserResponse findAllUsers(int page, int size, String sortBy, String sortDirection);

  /**
   * Retrieves a list of recently active users.
   *
   * @param since the timestamp from which to consider users as recently active
   * @return a list of recently active {@link UserResponse}s
   */
  List<UserResponse> getRecentlyActiveUsers(Instant since);

  /**
   * Retrieves user statistics.
   *
   * @return a {@link UserStatistics} object containing the statistics
   */
  UserStatistics getUserStatistics();

  /**
   * Counts the total number of active users.
   *
   * @return the count of active users
   */
  long countActiveUsers();

  /**
   * Finds all users who authenticated with a specific provider.
   *
   * @param provider the name of the provider
   * @return a list of {@link UserResponse}s from the specified provider
   */
  List<UserResponse> findUsersByProvider(String provider);

  /**
   * Restores a soft-deleted user.
   *
   * @param userId the ID of the user to restore
   * @return a {@link UserResponse} representing the restored user
   */
  UserResponse restoreUser(UUID userId);

  /**
   * Activates a user's account.
   *
   * @param userId the ID of the user to activate
   */
  void activateUser(UUID userId);

  /**
   * Deactivates a user's account.
   *
   * @param userId the ID of the user to deactivate
   */
  void deactivateUser(UUID userId);

  /**
   * Changes a user's password.
   *
   * @param userId the ID of the user
   * @param currentPassword the user's current password
   * @param newPassword the new password to set
   */
  void changePassword(UUID userId, String currentPassword, String newPassword);
}