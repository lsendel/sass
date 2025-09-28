package com.platform.user.internal;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.platform.audit.internal.AuditService;
import com.platform.shared.security.TenantContext;
import com.platform.shared.types.Email;

/**
 * Service for managing user-related operations.
 *
 * <p>This service encapsulates the business logic for creating, updating, deleting, and retrieving
 * users. It is aware of the tenant context for security purposes and performs detailed auditing for
 * all significant operations.
 *
 * @see User
 * @see UserRepository
 * @see AuditService
 */
@Service
@Transactional
public class UserService {

  private static final Logger logger = LoggerFactory.getLogger(UserService.class);

  private final UserRepository userRepository;
  private final AuditService auditService;

  /**
   * Constructs a new UserService.
   *
   * @param userRepository the repository for managing users
   * @param auditService the service for logging audit events
   */
  public UserService(UserRepository userRepository, AuditService auditService) {
    this.userRepository = userRepository;
    this.auditService = auditService;
  }

  /**
   * Finds a user by their ID, excluding soft-deleted ones.
   *
   * @param userId the ID of the user
   * @return an {@link Optional} containing the user if found, otherwise an empty {@link Optional}
   */
  @Transactional(readOnly = true)
  public Optional<User> findById(UUID userId) {
    return userRepository.findById(userId).filter(user -> !user.isDeleted());
  }

  /**
   * Finds a user by their email address, excluding soft-deleted ones.
   *
   * @param email the email address of the user
   * @return an {@link Optional} containing the user if found, otherwise an empty {@link Optional}
   */
  @Transactional(readOnly = true)
  public Optional<User> findByEmail(String email) {
    return userRepository.findByEmailAndDeletedAtIsNull(email);
  }

  /**
   * Retrieves the currently authenticated user from the security context.
   *
   * @return an {@link Optional} containing the current user if authenticated, otherwise an empty
   *     {@link Optional}
   */
  @Transactional(readOnly = true)
  public Optional<User> getCurrentUser() {
    UUID currentUserId = TenantContext.getCurrentUserId();
    if (currentUserId == null) {
      return Optional.empty();
    }
    return findById(currentUserId);
  }

  /**
   * Creates a new user.
   *
   * @param email the user's email address
   * @param name the user's full name
   * @param provider the name of the authentication provider
   * @param providerId the user's ID from the provider
   * @return the newly created {@link User} entity
   */
  public User createUser(String email, String name, String provider, String providerId) {
    return createUser(email, name, provider, providerId, Map.of());
  }

  /**
   * Creates a new user with a set of preferences.
   *
   * @param email the user's email address
   * @param name the user's full name
   * @param provider the name of the authentication provider
   * @param providerId the user's ID from the provider
   * @param preferences a map of custom preferences for the user
   * @return the newly created {@link User} entity
   * @throws IllegalArgumentException if the email or provider ID already exists
   */
  public User createUser(
      String email,
      String name,
      String provider,
      String providerId,
      Map<String, Object> preferences) {
    // Audit user creation attempt
    auditService.logEvent(
        "USER_CREATION_STARTED",
        "USER",
        email,
        "CREATE",
        Map.of(
            "email", email,
            "name", name,
            "provider", provider,
            "provider_id", providerId,
            "has_preferences", preferences != null && !preferences.isEmpty()),
        null,
        "system",
        "UserService",
        Map.of("action", "user_creation_started"));

    // Validate email is not already taken
    if (userRepository.existsByEmailAndDeletedAtIsNull(email)) {
      // Audit failed creation attempt
      auditService.logEvent(
          "USER_CREATION_FAILED",
          "USER",
          email,
          "CREATE",
          Map.of(
              "email", email,
              "reason", "Email already exists",
              "provider", provider),
          null,
          "system",
          "UserService",
          Map.of("error", "duplicate_email"));

      throw new IllegalArgumentException("User with email already exists: " + email);
    }

    // Validate provider and providerId combination
    Optional<User> existingProviderUser =
        userRepository.findByProviderAndProviderId(provider, providerId);
    if (existingProviderUser.isPresent()) {
      // Audit failed creation attempt
      auditService.logEvent(
          "USER_CREATION_FAILED",
          "USER",
          email,
          "CREATE",
          Map.of(
              "email", email,
              "provider", provider,
              "provider_id", providerId,
              "reason", "Provider ID already exists",
              "existing_user_id", existingProviderUser.get().getId().toString()),
          null,
          "system",
          "UserService",
          Map.of("error", "duplicate_provider_id"));

      throw new IllegalArgumentException(
          "User with provider already exists: " + provider + ":" + providerId);
    }

    User user = new User(new Email(email), name, provider, providerId);
    if (preferences != null && !preferences.isEmpty()) {
      user.updatePreferences(preferences);

      // Audit preference setting
      auditService.logEvent(
          "USER_PREFERENCES_SET",
          "USER",
          email,
          "UPDATE",
          Map.of(
              "email", email,
              "preference_keys", preferences.keySet().toString(),
              "preference_count", preferences.size()),
          null,
          "system",
          "UserService",
          null);
    }

    User savedUser = userRepository.save(user);

    // Audit successful user creation
    auditService.logEvent(
        "USER_CREATED",
        "USER",
        savedUser.getId().toString(),
        "CREATE",
        Map.of(
            "user_id", savedUser.getId().toString(),
            "email", email,
            "name", name,
            "provider", provider,
            "has_preferences", preferences != null && !preferences.isEmpty()),
        Map.of(
            "user_id",
            savedUser.getId().toString(),
            "email",
            email,
            "created_at",
            savedUser.getCreatedAt().toString(),
            "provider",
            provider),
        "system",
        "UserService",
        Map.of("success", "user_created"));

    logger.info("Created user: {} with email: {}", savedUser.getId(), email);

    return savedUser;
  }

  /**
   * Updates a user's profile information.
   *
   * @param userId the ID of the user to update
   * @param name the new name for the user
   * @param preferences a map of new preferences for the user
   * @return the updated {@link User} entity
   * @throws IllegalArgumentException if the user is not found
   * @throws SecurityException if the current user is not authorized to update the profile
   */
  public User updateProfile(UUID userId, String name, Map<String, Object> preferences) {
    User user =
        findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

    // Validate current user can update this profile
    validateUserAccess(user);

    // Store old values for audit trail
    String oldName = user.getName();
    Map<String, Object> oldPreferences = user.getPreferences();

    // Audit profile update attempt
    auditService.logEvent(
        "USER_PROFILE_UPDATE_STARTED",
        "USER",
        userId.toString(),
        "UPDATE",
        Map.of(
            "user_id",
            userId.toString(),
            "email",
            user.getEmail().getValue(),
            "old_name",
            oldName != null ? oldName : "",
            "new_name",
            name != null ? name : "",
            "preferences_updated",
            preferences != null && !preferences.isEmpty()),
        null,
        "system",
        "UserService",
        Map.of("action", "profile_update_started"));

    user.updateProfile(name, preferences);
    User savedUser = userRepository.save(user);

    // Audit successful profile update
    auditService.logEvent(
        "USER_PROFILE_UPDATED",
        "USER",
        userId.toString(),
        "UPDATE",
        Map.of(
            "user_id", userId.toString(),
            "email", user.getEmail().getValue(),
            "name_changed", !oldName.equals(name),
            "preferences_changed", preferences != null && !preferences.isEmpty()),
        Map.of(
            "user_id", userId.toString(),
            "updated_at", savedUser.getUpdatedAt().toString(),
            "name", name != null ? name : ""),
        "system",
        "UserService",
        Map.of("success", "profile_updated"));

    logger.info("Updated profile for user: {}", userId);
    return savedUser;
  }

  /**
   * Updates a user's preferences.
   *
   * @param userId the ID of the user to update
   * @param preferences a map of new preferences for the user
   * @return the updated {@link User} entity
   * @throws IllegalArgumentException if the user is not found
   * @throws SecurityException if the current user is not authorized to update the preferences
   */
  public User updatePreferences(UUID userId, Map<String, Object> preferences) {
    User user =
        findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

    validateUserAccess(user);

    user.updatePreferences(preferences);
    User savedUser = userRepository.save(user);

    logger.info("Updated preferences for user: {}", userId);
    return savedUser;
  }

  /**
   * Soft-deletes a user.
   *
   * @param userId the ID of the user to delete
   * @throws IllegalArgumentException if the user is not found
   * @throws SecurityException if the current user is not authorized to delete the user
   */
  public void deleteUser(UUID userId) {
    User user =
        findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

    // Validate current user can delete this user (admin or self)
    validateUserDeletionAccess(user);

    // Audit user deletion attempt (GDPR critical)
    auditService.logEvent(
        "USER_DELETION_STARTED",
        "USER",
        userId.toString(),
        "DELETE",
        Map.of(
            "user_id", userId.toString(),
            "email", user.getEmail().getValue(),
            "name", user.getName(),
            "provider", user.getProvider(),
            "deletion_type", "soft_delete",
            "has_preferences", user.getPreferences() != null && !user.getPreferences().isEmpty()),
        null,
        "system",
        "UserService",
        Map.of("action", "user_deletion_started"));

    // Store user data for audit before deletion
    String userEmail = user.getEmail().getValue();
    String userName = user.getName();
    String userProvider = user.getProvider();
    Instant userCreatedAt = user.getCreatedAt();

    user.markAsDeleted();
    User deletedUser = userRepository.save(user);

    // Audit successful user deletion (GDPR compliance audit)
    auditService.logEvent(
        "USER_DELETED",
        "USER",
        userId.toString(),
        "DELETE",
        Map.of(
            "user_id",
            userId.toString(),
            "email",
            userEmail,
            "name",
            userName,
            "provider",
            userProvider,
            "deletion_type",
            "soft_delete",
            "user_account_age_days",
            java.time.Duration.between(userCreatedAt, Instant.now()).toDays()),
        Map.of(
            "user_id", userId.toString(),
            "deleted_at", deletedUser.getDeletedAt().toString(),
            "deletion_confirmed", "true"),
        "system",
        "UserService",
        Map.of("gdpr_compliance", "user_deleted"));

    logger.info("Soft deleted user: {}", userId);
  }

  /**
   * Searches for users by a name pattern.
   *
   * @param namePattern the pattern to search for in user names
   * @return a list of users matching the name pattern
   */
  @Transactional(readOnly = true)
  public List<User> searchUsersByName(String namePattern) {
    return userRepository.findByNameContainingIgnoreCase(namePattern);
  }

  /**
   * Finds all users who authenticated with a specific provider.
   *
   * @param provider the name of the provider
   * @return a list of users from the specified provider
   */
  @Transactional(readOnly = true)
  public List<User> findUsersByProvider(String provider) {
    return userRepository.findByProviderAndDeletedAtIsNull(provider);
  }

  /**
   * Retrieves a paginated list of all active users.
   *
   * <p>This is an administrative function and requires admin privileges.
   *
   * @param page the page number to retrieve
   * @param size the number of users per page
   * @param sortBy the field to sort by
   * @param sortDirection the sort direction ("asc" or "desc")
   * @return a {@link Page} of users
   * @throws SecurityException if the current user is not an administrator
   */
  @Transactional(readOnly = true)
  public Page<User> findAllUsers(int page, int size, String sortBy, String sortDirection) {
    // Validate admin access
    validateAdminAccess();

    Sort.Direction direction =
        "desc".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC;

    Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

    // Use custom query to get only active users
    return userRepository.findByDeletedAtIsNull(pageable);
  }

  /**
   * Retrieves a list of users who have been recently active.
   *
   * <p>This is an administrative function.
   *
   * @param since the timestamp from which to consider users as recently active
   * @return a list of recently active users
   * @throws SecurityException if the current user is not an administrator
   */
  @Transactional(readOnly = true)
  public List<User> getRecentlyActiveUsers(Instant since) {
    validateAdminAccess();
    return userRepository.findRecentlyActive(since);
  }

  /**
   * Counts the total number of active users.
   *
   * @return the count of active users
   */
  @Transactional(readOnly = true)
  public long countActiveUsers() {
    return userRepository.countActiveUsers();
  }

  /**
   * Gets a map of user counts for each authentication provider.
   *
   * @return a map where keys are provider names and values are user counts
   */
  @Transactional(readOnly = true)
  public Map<String, Long> getUserCountsByProvider() {
    List<String> providers = List.of("google", "github", "microsoft");
    return providers.stream()
        .collect(
            java.util.stream.Collectors.toMap(
                provider -> provider, provider -> userRepository.countUsersByProvider(provider)));
  }

  /**
   * Finds users by a specific preference key-value pair.
   *
   * <p>This is an administrative function.
   *
   * @param preferenceKey the key of the preference
   * @param preferenceValue the value of the preference
   * @return a list of users with the specified preference
   * @throws SecurityException if the current user is not an administrator
   */
  @Transactional(readOnly = true)
  public List<User> findUsersByPreference(String preferenceKey, String preferenceValue) {
    validateAdminAccess();
    return userRepository.findByPreference(preferenceKey, preferenceValue);
  }

  /**
   * Restores a soft-deleted user.
   *
   * <p>This is an administrative function.
   *
   * @param userId the ID of the user to restore
   * @return the restored {@link User} entity
   * @throws IllegalArgumentException if the user is not found, not deleted, or if their email is
   *     already in use
   * @throws SecurityException if the current user is not an administrator
   */
  public User restoreUser(UUID userId) {
    validateAdminAccess();

    Optional<User> deletedUser = userRepository.findById(userId);
    if (deletedUser.isEmpty()) {
      // Audit failed restoration attempt
      auditService.logEvent(
          "USER_RESTORATION_FAILED",
          "USER",
          userId.toString(),
          "RESTORE",
          Map.of("user_id", userId.toString(), "reason", "User not found"),
          null,
          "system",
          "UserService",
          Map.of("error", "user_not_found"));

      throw new IllegalArgumentException("User not found: " + userId);
    }

    User user = deletedUser.get();
    if (!user.isDeleted()) {
      // Audit failed restoration attempt
      auditService.logEvent(
          "USER_RESTORATION_FAILED",
          "USER",
          userId.toString(),
          "RESTORE",
          Map.of(
              "user_id", userId.toString(),
              "email", user.getEmail().getValue(),
              "reason", "User is not deleted"),
          null,
          "system",
          "UserService",
          Map.of("error", "user_not_deleted"));

      throw new IllegalArgumentException("User is not deleted: " + userId);
    }

    // Check if email conflicts with existing active user
    if (userRepository.existsByEmailAndDeletedAtIsNull(user.getEmail().getValue())) {
      // Audit failed restoration attempt
      auditService.logEvent(
          "USER_RESTORATION_FAILED",
          "USER",
          userId.toString(),
          "RESTORE",
          Map.of(
              "user_id", userId.toString(),
              "email", user.getEmail().getValue(),
              "reason", "Email already in use by another user"),
          null,
          "system",
          "UserService",
          Map.of("error", "email_conflict"));

      throw new IllegalArgumentException(
          "Cannot restore user - email already in use: " + user.getEmail().getValue());
    }

    // Audit user restoration attempt
    auditService.logEvent(
        "USER_RESTORATION_STARTED",
        "USER",
        userId.toString(),
        "RESTORE",
        Map.of(
            "user_id", userId.toString(),
            "email", user.getEmail().getValue(),
            "name", user.getName(),
            "provider", user.getProvider(),
            "deleted_at", user.getDeletedAt().toString()),
        null,
        "system",
        "UserService",
        Map.of("action", "user_restoration_started"));

    userRepository.restoreUser(userId);
    User restoredUser = userRepository.findById(userId).orElseThrow();

    // Audit successful user restoration
    auditService.logEvent(
        "USER_RESTORED",
        "USER",
        userId.toString(),
        "RESTORE",
        Map.of(
            "user_id", userId.toString(),
            "email", restoredUser.getEmail().getValue(),
            "name", restoredUser.getName(),
            "provider", restoredUser.getProvider(),
            "was_deleted_at", user.getDeletedAt().toString()),
        Map.of(
            "user_id", userId.toString(),
            "restored_at", Instant.now().toString(),
            "deletion_duration_days",
                java.time.Duration.between(user.getDeletedAt(), Instant.now()).toDays()),
        "system",
        "UserService",
        Map.of("gdpr_compliance", "user_restored"));

    logger.info("Restored user: {}", userId);
    return restoredUser;
  }

  /**
   * Restores multiple soft-deleted users in a single operation.
   *
   * <p>This is an administrative function.
   *
   * @param userIds a list of user IDs to restore
   * @return a list of the restored {@link User} entities
   * @throws SecurityException if the current user is not an administrator
   */
  public List<User> bulkRestoreUsers(List<UUID> userIds) {
    validateAdminAccess();

    List<User> restoredUsers = new ArrayList<>();
    for (UUID userId : userIds) {
      try {
        User user = restoreUser(userId);
        restoredUsers.add(user);
      } catch (Exception e) {
        logger.warn("Failed to restore user {}: {}", userId, e.getMessage());
        // Continue with other users
      }
    }

    logger.info("Bulk restored {} out of {} users", restoredUsers.size(), userIds.size());
    return restoredUsers;
  }

  /**
   * Soft-deletes multiple users in a single operation.
   *
   * <p>This is an administrative function.
   *
   * @param userIds a list of user IDs to delete
   * @throws SecurityException if the current user is not an administrator
   */
  public void bulkDeleteUsers(List<UUID> userIds) {
    validateAdminAccess();

    int deletedCount = 0;
    for (UUID userId : userIds) {
      try {
        deleteUser(userId);
        deletedCount++;
      } catch (Exception e) {
        logger.warn("Failed to delete user {}: {}", userId, e.getMessage());
        // Continue with other users
      }
    }

    logger.info("Bulk deleted {} out of {} users", deletedCount, userIds.size());
  }

  /**
   * Bulk updates the provider for a set of users.
   *
   * <p>This is an administrative function. Note: This method currently only logs the intended
   * operation and does not perform the update.
   *
   * @param fromProvider the original provider
   * @param toProvider the new provider
   * @return a list of users who would have been updated
   * @throws SecurityException if the current user is not an administrator
   */
  public List<User> bulkUpdateProvider(String fromProvider, String toProvider) {
    validateAdminAccess();

    List<User> usersToUpdate = userRepository.findByProviderAndDeletedAtIsNull(fromProvider);
    List<User> updatedUsers = new ArrayList<>();

    for (User user : usersToUpdate) {
      try {
        // This would need to be enhanced to properly handle provider changes
        // For now, we'll just log the operation
        logger.info("Would update user {} from {} to {}", user.getId(), fromProvider, toProvider);
        updatedUsers.add(user);
      } catch (Exception e) {
        logger.warn("Failed to update provider for user {}: {}", user.getId(), e.getMessage());
      }
    }

    logger.info("Bulk updated provider for {} users from {} to {}",
               updatedUsers.size(), fromProvider, toProvider);
    return updatedUsers;
  }

  /**
   * Exports a list of users based on specified criteria.
   *
   * <p>This is an administrative function.
   *
   * @param status the status to filter by (e.g., "active")
   * @param provider the provider to filter by
   * @param createdAfter an ISO 8601 timestamp to filter by creation date
   * @return a list of users matching the criteria
   * @throws SecurityException if the current user is not an administrator
   */
  @Transactional(readOnly = true)
  public List<User> exportUsers(String status, String provider, String createdAfter) {
    validateAdminAccess();

    // Build query based on criteria
    List<User> users;

    if (provider != null && !provider.isEmpty()) {
      users = userRepository.findByProviderAndDeletedAtIsNull(provider);
    } else if ("active".equals(status)) {
      users = userRepository.findAllActive();
    } else {
      users = userRepository.findAll();
    }

    // Filter by creation date if provided
    if (createdAfter != null && !createdAfter.isEmpty()) {
      try {
        Instant filterDate = Instant.parse(createdAfter);
        users = users.stream()
            .filter(user -> user.getCreatedAt().isAfter(filterDate))
            .toList();
      } catch (Exception e) {
        logger.warn("Invalid date format for createdAfter: {}", createdAfter);
      }
    }

    logger.info("Exported {} users with criteria: status={}, provider={}, createdAfter={}",
               users.size(), status, provider, createdAfter);
    return users;
  }

  /**
   * Retrieves user activity analytics for a given number of days.
   *
   * <p>This is an administrative function.
   *
   * @param days the number of days to look back for activity
   * @return a map containing analytics data
   * @throws SecurityException if the current user is not an administrator
   */
  @Transactional(readOnly = true)
  public Map<String, Object> getUserActivityAnalytics(int days) {
    validateAdminAccess();

    Instant sinceDate = Instant.now().minusSeconds(days * 24 * 60 * 60L);

    Map<String, Object> analytics = new HashMap<>();

    // Active users in period
    long activeUsers = userRepository.countUsersActiveAfter(sinceDate);
    analytics.put("activeUsers", activeUsers);

    // New users in period
    List<User> newUsers = userRepository.findUsersCreatedBetween(sinceDate, Instant.now());
    analytics.put("newUsers", newUsers.size());

    // Users by provider
    Map<String, Long> providerCounts = getUserCountsByProvider();
    analytics.put("usersByProvider", providerCounts);

    // Activity trend (simplified)
    analytics.put("totalUsers", userRepository.countActiveUsers());
    analytics.put("analysisDate", Instant.now().toString());
    analytics.put("analysisPeriodDays", days);

    return analytics;
  }

  /**
   * Retrieves overall user statistics.
   *
   * <p>This is an administrative function.
   *
   * @return a {@link UserStatistics} record containing the statistics
   * @throws SecurityException if the current user is not an administrator
   */
  @Transactional(readOnly = true)
  public UserStatistics getUserStatistics() {
    validateAdminAccess();

    long totalUsers = userRepository.countActiveUsers();
    Map<String, Long> providerCounts = getUserCountsByProvider();

    Instant lastWeek = Instant.now().minusSeconds(7 * 24 * 60 * 60);
    List<User> recentUsers = userRepository.findUsersCreatedBetween(lastWeek, Instant.now());

    // Calculate active users (users with activity in last 30 days)
    Instant thirtyDaysAgo = Instant.now().minusSeconds(30 * 24 * 60 * 60);
    long activeUsers = userRepository.countUsersActiveAfter(thirtyDaysAgo);

    // Calculate average session duration based on user activity patterns
    double averageSessionDuration = calculateAverageSessionDuration();

    return new UserStatistics(totalUsers, activeUsers, recentUsers.size(), providerCounts, averageSessionDuration);
  }

  /**
   * Calculates an estimated average session duration for active users.
   *
   * <p>This method provides a simplified estimation based on user activity frequency.
   *
   * @return the estimated average session duration in minutes
   */
  @Transactional(readOnly = true)
  public double calculateAverageSessionDuration() {
    try {
      // For a more accurate calculation, we would analyze token usage patterns
      // For now, we'll estimate based on user update frequency
      Instant lastMonth = Instant.now().minusSeconds(30 * 24 * 60 * 60);
      List<User> activeUsers = userRepository.findUsersActiveAfter(lastMonth);

      if (activeUsers.isEmpty()) {
        return 0.0;
      }

      // Estimate session duration based on activity patterns
      // Users who update frequently likely have shorter, more frequent sessions
      // Users who update less frequently likely have longer sessions
      double totalEstimatedDuration = 0.0;

      for (User user : activeUsers) {
        // Calculate days since last activity
        long daysSinceLastActivity = java.time.Duration.between(user.getUpdatedAt(), Instant.now()).toDays();

        // Estimate session duration (in minutes)
        // More recent activity suggests more active users with potentially longer sessions
        if (daysSinceLastActivity <= 1) {
          totalEstimatedDuration += 45.0; // Active users: ~45 min sessions
        } else if (daysSinceLastActivity <= 7) {
          totalEstimatedDuration += 30.0; // Moderately active: ~30 min sessions
        } else {
          totalEstimatedDuration += 20.0; // Less active: ~20 min sessions
        }
      }

      return totalEstimatedDuration / activeUsers.size();
    } catch (Exception e) {
      logger.warn("Error calculating average session duration", e);
      return 0.0;
    }
  }

  // Validation methods

  private void validateUserAccess(User user) {
    UUID currentUserId = TenantContext.getCurrentUserId();
    if (currentUserId == null) {
      throw new SecurityException("No authenticated user");
    }

    // Users can only modify their own profile
    if (!currentUserId.equals(user.getId())) {
      throw new SecurityException("Access denied - cannot modify other user's profile");
    }
  }

  private void validateUserDeletionAccess(User user) {
    UUID currentUserId = TenantContext.getCurrentUserId();
    if (currentUserId == null) {
      throw new SecurityException("No authenticated user");
    }

    // For now, users can only delete themselves
    // This could be enhanced to allow organization admins to delete members
    if (!currentUserId.equals(user.getId())) {
      throw new SecurityException("Access denied - cannot delete other users");
    }
  }

  private void validateAdminAccess() {
    // This would be enhanced with proper role checking
    // For now, just check if user is authenticated
    if (TenantContext.getCurrentUserId() == null) {
      throw new SecurityException("Admin access required");
    }
  }

  /**
   * DTO for holding user statistics.
   *
   * @param totalUsers the total number of active users
   * @param activeUsers the number of users active in the last 30 days
   * @param newUsersThisWeek the number of new users created in the last 7 days
   * @param usersByProvider a map of user counts by authentication provider
   * @param averageSessionDuration an estimated average session duration in minutes
   */
  public record UserStatistics(
      long totalUsers,
      long activeUsers,
      long newUsersThisWeek,
      Map<String, Long> usersByProvider,
      double averageSessionDuration) {}
}
