package com.platform.user.internal;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link User} entities.
 *
 * <p>This interface provides methods for querying and managing user records in the database,
 * including support for multi-tenancy, soft deletion, and advanced analytics.
 *
 * @see User
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

  /**
   * Finds a user by their email address and organization ID, excluding soft-deleted users.
   *
   * <p>This query is multi-tenant aware, allowing the same email to exist in different
   * organizations.
   *
   * @param email the email address of the user
   * @param organizationId the ID of the organization
   * @return an {@link Optional} containing the user if found, otherwise an empty {@link Optional}
   */
  @Query(
      "SELECT u FROM User u WHERE u.email.value = :email AND u.organization.id = :organizationId AND u.deletedAt IS NULL")
  Optional<User> findByEmailAndOrganizationIdAndDeletedAtIsNull(
      @Param("email") String email, @Param("organizationId") UUID organizationId);

  /**
   * Finds all non-deleted users with a specific email address across all organizations.
   *
   * <p>This method is intended for administrative purposes.
   *
   * @param email the email address to search for
   * @return a list of users matching the email
   */
  @Query("SELECT u FROM User u WHERE u.email.value = :email AND u.deletedAt IS NULL")
  List<User> findByEmailAcrossOrganizations(@Param("email") String email);

  /**
   * Finds a user by their OAuth2 provider and provider-specific ID.
   *
   * @param provider the name of the OAuth2 provider (e.g., "google")
   * @param providerId the user's ID from the provider
   * @return an {@link Optional} containing the user if found, otherwise an empty {@link Optional}
   */
  Optional<User> findByProviderAndProviderId(String provider, String providerId);

  /**
   * Finds all users with a specific email address, including soft-deleted ones.
   *
   * <p>This method is intended for administrative purposes.
   *
   * @param email the email address to search for
   * @return a list of all users with the given email
   */
  @Query("SELECT u FROM User u WHERE u.email.value = :email")
  List<User> findByEmail(@Param("email") String email);

  /**
   * Finds a user by their email address and organization ID, including soft-deleted ones.
   *
   * @param email the email address of the user
   * @param organizationId the ID of the organization
   * @return an {@link Optional} containing the user if found, otherwise an empty {@link Optional}
   */
  @Query(
      "SELECT u FROM User u WHERE u.email.value = :email AND u.organization.id = :organizationId")
  Optional<User> findByEmailAndOrganizationId(
      @Param("email") String email, @Param("organizationId") UUID organizationId);

  /**
   * Finds all active (not soft-deleted) users, ordered by creation date.
   *
   * @return a list of all active users
   */
  @Query("SELECT u FROM User u WHERE u.deletedAt IS NULL ORDER BY u.createdAt DESC")
  List<User> findAllActive();

  /**
   * Finds all active users with pagination.
   *
   * @param pageable the pagination information
   * @return a {@link Page} of active users
   */
  @Query("SELECT u FROM User u WHERE u.deletedAt IS NULL")
  Page<User> findByDeletedAtIsNull(Pageable pageable);

  /**
   * Finds all active users who authenticated with a specific provider.
   *
   * @param provider the name of the provider
   * @return a list of users from the specified provider
   */
  List<User> findByProviderAndDeletedAtIsNull(String provider);

  /**
   * Finds all active users created within a specific date range.
   *
   * @param startDate the start of the date range
   * @param endDate the end of the date range
   * @return a list of users created within the date range
   */
  @Query(
      "SELECT u FROM User u WHERE u.createdAt BETWEEN :startDate AND :endDate AND u.deletedAt IS NULL")
  List<User> findUsersCreatedBetween(
      @Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

  /**
   * Counts the total number of active users.
   *
   * @return the count of active users
   */
  @Query("SELECT COUNT(u) FROM User u WHERE u.deletedAt IS NULL")
  long countActiveUsers();

  /**
   * Counts the number of active users for a specific provider.
   *
   * @param provider the name of the provider
   * @return the count of users for the provider
   */
  @Query("SELECT COUNT(u) FROM User u WHERE u.provider = :provider AND u.deletedAt IS NULL")
  long countUsersByProvider(@Param("provider") String provider);

  /**
   * Finds all active users with a specific preference key-value pair in their preferences JSON.
   *
   * @param preferenceKey the key of the preference to search for
   * @param value the value of the preference to match
   * @return a list of users with the specified preference
   */
  @Query(
      value =
          "SELECT * FROM users u WHERE (u.preferences::jsonb) ->> :preferenceKey = :value AND u.deleted_at IS NULL",
      nativeQuery = true)
  List<User> findByPreference(
      @Param("preferenceKey") String preferenceKey, @Param("value") String value);

  /**
   * Checks if an email address exists for a user in a specific organization, excluding soft-deleted
   * users.
   *
   * @param email the email address to check
   * @param organizationId the ID of the organization
   * @return {@code true} if the email exists in the organization, {@code false} otherwise
   */
  @Query(
      "SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.email.value = :email AND u.organization.id = :organizationId AND u.deletedAt IS NULL")
  boolean existsByEmailAndOrganizationIdAndDeletedAtIsNull(
      @Param("email") String email, @Param("organizationId") UUID organizationId);

  /**
   * Finds all active users whose names contain a specific pattern, ignoring case.
   *
   * @param namePattern the pattern to search for in user names
   * @return a list of users matching the name pattern
   */
  @Query(
      "SELECT u FROM User u WHERE LOWER(u.name) LIKE LOWER(CONCAT('%', :namePattern, '%')) AND u.deletedAt IS NULL")
  List<User> findByNameContainingIgnoreCase(@Param("namePattern") String namePattern);

  /**
   * Soft-deletes a user by setting their {@code deletedAt} timestamp.
   *
   * @param userId the ID of the user to delete
   * @param deletedAt the timestamp to set as the deletion time
   */
  @Query("UPDATE User u SET u.deletedAt = :deletedAt WHERE u.id = :userId")
  void softDeleteUser(@Param("userId") UUID userId, @Param("deletedAt") Instant deletedAt);

  /**
   * Restores a soft-deleted user by clearing their {@code deletedAt} timestamp.
   *
   * @param userId the ID of the user to restore
   */
  @Query("UPDATE User u SET u.deletedAt = NULL WHERE u.id = :userId")
  void restoreUser(@Param("userId") UUID userId);

  /**
   * Finds all active users who authenticated with any of the specified providers.
   *
   * @param providers a list of provider names
   * @return a list of users from the specified providers
   */
  @Query("SELECT u FROM User u WHERE u.provider IN :providers AND u.deletedAt IS NULL")
  List<User> findByProviderIn(@Param("providers") List<String> providers);

  /**
   * Finds all users who have been recently active, based on their {@code updatedAt} timestamp.
   *
   * @param since the timestamp from which to consider users as recently active
   * @return a list of recently active users
   */
  @Query(
      "SELECT u FROM User u WHERE u.updatedAt >= :since AND u.deletedAt IS NULL ORDER BY u.updatedAt DESC")
  List<User> findRecentlyActive(@Param("since") Instant since);

  /**
   * Finds all users with a specific email address, including soft-deleted ones, for GDPR data
   * export purposes.
   *
   * @param email the email address to search for
   * @return a list of all users with the given email
   */
  @Query("SELECT u FROM User u WHERE u.email.value = :email")
  List<User> findAllByEmailForDataExport(@Param("email") String email);

  // Password authentication specific queries

  /**
   * Finds a user by their password reset token, excluding soft-deleted users.
   *
   * @param token the password reset token
   * @return an {@link Optional} containing the user if found, otherwise an empty {@link Optional}
   */
  @Query("SELECT u FROM User u WHERE u.passwordResetToken = :token AND u.deletedAt IS NULL")
  Optional<User> findByPasswordResetToken(@Param("token") String token);

  /**
   * Finds a user by their email verification token, excluding soft-deleted users.
   *
   * @param token the email verification token
   * @return an {@link Optional} containing the user if found, otherwise an empty {@link Optional}
   */
  @Query("SELECT u FROM User u WHERE u.emailVerificationToken = :token AND u.deletedAt IS NULL")
  Optional<User> findByEmailVerificationToken(@Param("token") String token);

  /**
   * Finds all active users who have password authentication enabled.
   *
   * @return a list of users with password authentication
   */
  @Query(
      "SELECT u FROM User u JOIN u.authenticationMethods am WHERE am = 'PASSWORD' AND u.deletedAt IS NULL")
  List<User> findUsersWithPasswordAuth();

  /**
   * Counts the number of active users whose email addresses have not been verified.
   *
   * @return the count of unverified users
   */
  @Query("SELECT COUNT(u) FROM User u WHERE u.emailVerified = false AND u.deletedAt IS NULL")
  long countUnverifiedUsers();

  /**
   * Finds all active users whose accounts are currently locked.
   *
   * @param currentTime the current time, used to check if the lockout is still active
   * @return a list of locked users
   */
  @Query("SELECT u FROM User u WHERE u.lockoutExpiresAt > :currentTime AND u.deletedAt IS NULL")
  List<User> findLockedUsers(@Param("currentTime") Instant currentTime);

  /**
   * Searches for active users within an organization by name or email, with pagination.
   *
   * @param organizationId the ID of the organization to search within
   * @param searchTerm the term to search for in names and emails
   * @param pageable the pagination information
   * @return a list of users matching the search criteria
   */
  @Query(
      value =
          "SELECT u FROM User u WHERE u.organization.id = :organizationId "
              + "AND (LOWER(u.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) "
              + "OR LOWER(u.email.value) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) "
              + "AND u.deletedAt IS NULL "
              + "ORDER BY u.name")
  List<User> searchUsersInOrganization(
      @Param("organizationId") UUID organizationId,
      @Param("searchTerm") String searchTerm,
      Pageable pageable);

  /**
   * Finds all active users in a specific organization.
   *
   * @param organizationId the ID of the organization
   * @return a list of users in the organization
   */
  @Query(
      "SELECT u FROM User u WHERE u.organization.id = :organizationId AND u.deletedAt IS NULL ORDER BY u.createdAt DESC")
  List<User> findByOrganizationId(@Param("organizationId") UUID organizationId);

  /**
   * Finds a single active user by their email address.
   *
   * <p>This method is provided for legacy compatibility.
   *
   * @param email the email address to search for
   * @return an {@link Optional} containing the user if found, otherwise an empty {@link Optional}
   */
  @Query("SELECT u FROM User u WHERE u.email.value = :email AND u.deletedAt IS NULL")
  Optional<User> findByEmailAndDeletedAtIsNull(@Param("email") String email);

  /**
   * Checks if an email address exists for any active user.
   *
   * <p>This method is provided for legacy compatibility.
   *
   * @param email the email address to check
   * @return {@code true} if the email exists, {@code false} otherwise
   */
  @Query(
      "SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.email.value = :email AND u.deletedAt IS NULL")
  boolean existsByEmailAndDeletedAtIsNull(@Param("email") String email);

  /**
   * Counts the number of users who have been active since a specific date.
   *
   * @param sinceDate the date from which to count active users
   * @return the number of recently active users
   */
  @Query("SELECT COUNT(u) FROM User u WHERE u.updatedAt >= :sinceDate AND u.deletedAt IS NULL")
  long countUsersActiveAfter(@Param("sinceDate") Instant sinceDate);

  /**
   * Finds all users who have been active since a specific date.
   *
   * @param sinceDate the date from which to find active users
   * @return a list of recently active users
   */
  @Query("SELECT u FROM User u WHERE u.updatedAt >= :sinceDate AND u.deletedAt IS NULL ORDER BY u.updatedAt DESC")
  List<User> findUsersActiveAfter(@Param("sinceDate") Instant sinceDate);

  // ===== ADVANCED ANALYTICS QUERIES =====

  /**
   * Provides user growth analytics by counting new users per month within a date range.
   *
   * @param startDate the start of the date range
   * @param endDate the end of the date range
   * @return a list of object arrays, where each array contains the month and the user count
   */
  @Query(value = """
      SELECT
          DATE_TRUNC('month', created_at) as month,
          COUNT(*) as user_count
      FROM users u
      WHERE u.deleted_at IS NULL
          AND u.created_at >= :startDate
          AND u.created_at <= :endDate
      GROUP BY DATE_TRUNC('month', created_at)
      ORDER BY month
      """, nativeQuery = true)
  List<Object[]> getUserGrowthByMonth(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

  /**
   * Provides user retention analytics by comparing active users between two periods.
   *
   * @param currentPeriodStart the start of the current period
   * @param currentPeriodEnd the end of the current period
   * @param previousPeriodStart the start of the previous period
   * @param previousPeriodEnd the end of the previous period
   * @return a list containing a single object array with the count of retained users, total
   *     previous users, and the retention rate
   */
  @Query(value = """
      SELECT
          COUNT(DISTINCT u1.id) as retained_users,
          COUNT(DISTINCT u2.id) as total_previous_users,
          CASE WHEN COUNT(DISTINCT u2.id) > 0 THEN
              ROUND((COUNT(DISTINCT u1.id) * 100.0 / COUNT(DISTINCT u2.id)), 2)
          ELSE 0 END as retention_rate
      FROM users u1
      JOIN users u2 ON u1.id = u2.id
      WHERE u1.updated_at BETWEEN :currentPeriodStart AND :currentPeriodEnd
          AND u2.updated_at BETWEEN :previousPeriodStart AND :previousPeriodEnd
          AND u1.deleted_at IS NULL AND u2.deleted_at IS NULL
      """, nativeQuery = true)
  List<Object[]> getUserRetentionRate(
      @Param("currentPeriodStart") Instant currentPeriodStart,
      @Param("currentPeriodEnd") Instant currentPeriodEnd,
      @Param("previousPeriodStart") Instant previousPeriodStart,
      @Param("previousPeriodEnd") Instant previousPeriodEnd);

  /**
   * Provides analytics on the distribution of authentication methods among users.
   *
   * @return a list of object arrays, where each array contains the provider name, user count, and
   *     percentage of total users
   */
  @Query(value = """
      SELECT
          u.provider,
          COUNT(*) as user_count,
          ROUND((COUNT(*) * 100.0 / SUM(COUNT(*)) OVER()), 2) as percentage
      FROM users u
      WHERE u.deleted_at IS NULL
      GROUP BY u.provider
      ORDER BY user_count DESC
      """, nativeQuery = true)
  List<Object[]> getAuthenticationMethodDistribution();

  /**
   * Provides analytics on organization size and average user age.
   *
   * @param minUsers the minimum number of users an organization must have to be included
   * @return a list of object arrays, where each array contains the organization name, user count,
   *     and average user age in days
   */
  @Query(value = """
      SELECT
          o.name as organization_name,
          COUNT(u.id) as user_count,
          AVG(EXTRACT(EPOCH FROM (NOW() - u.created_at)) / 86400) as avg_user_age_days
      FROM users u
      JOIN organizations o ON u.organization_id = o.id
      WHERE u.deleted_at IS NULL
      GROUP BY o.id, o.name
      HAVING COUNT(u.id) >= :minUsers
      ORDER BY user_count DESC
      """, nativeQuery = true)
  List<Object[]> getOrganizationUserAnalytics(@Param("minUsers") int minUsers);

  /**
   * Provides analytics on user activity patterns by day of the week and hour of the day.
   *
   * @param startDate the start of the date range
   * @param endDate the end of the date range
   * @return a list of object arrays, where each array contains the day of the week, hour of the
   *     day, and activity count
   */
  @Query(value = """
      SELECT
          EXTRACT(DOW FROM updated_at) as day_of_week,
          EXTRACT(HOUR FROM updated_at) as hour_of_day,
          COUNT(*) as activity_count
      FROM users u
      WHERE u.updated_at >= :startDate
          AND u.updated_at <= :endDate
          AND u.deleted_at IS NULL
      GROUP BY EXTRACT(DOW FROM updated_at), EXTRACT(HOUR FROM updated_at)
      ORDER BY day_of_week, hour_of_day
      """, nativeQuery = true)
  List<Object[]> getUserActivityPatterns(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

  /**
   * Finds email domains with a high number of users, which may indicate suspicious activity.
   *
   * @param threshold the minimum number of users a domain must have to be considered suspicious
   * @return a list of object arrays, where each array contains the domain, user count, and a sample
   *     of emails
   */
  @Query(value = """
      SELECT
          SUBSTRING(email FROM '@(.*)$') as domain,
          COUNT(*) as user_count,
          STRING_AGG(DISTINCT email, ', ') as sample_emails
      FROM users
      WHERE deleted_at IS NULL
      GROUP BY SUBSTRING(email FROM '@(.*)$')
      HAVING COUNT(*) >= :threshold
      ORDER BY user_count DESC
      """, nativeQuery = true)
  List<Object[]> findSuspiciousEmailDomains(@Param("threshold") int threshold);

  /**
   * Provides analytics on the user lifecycle funnel, categorizing users by their current status.
   *
   * @param inactiveThreshold the timestamp before which users are considered inactive
   * @return a list of object arrays, where each array contains the user status and the count of
   *     users in that status
   */
  @Query(value = """
      SELECT
          CASE
              WHEN email_verified = false THEN 'unverified'
              WHEN lockout_expires_at > NOW() THEN 'locked'
              WHEN updated_at < :inactiveThreshold THEN 'inactive'
              ELSE 'active'
          END as user_status,
          COUNT(*) as count
      FROM users
      WHERE deleted_at IS NULL
      GROUP BY
          CASE
              WHEN email_verified = false THEN 'unverified'
              WHEN lockout_expires_at > NOW() THEN 'locked'
              WHEN updated_at < :inactiveThreshold THEN 'inactive'
              ELSE 'active'
          END
      ORDER BY count DESC
      """, nativeQuery = true)
  List<Object[]> getUserLifecycleFunnel(@Param("inactiveThreshold") Instant inactiveThreshold);

  /**
   * Provides analytics on the geographic distribution of users, based on their timezone preference.
   *
   * @return a list of object arrays, where each array contains the organization name, timezone, and
   *     user count
   */
  @Query(value = """
      SELECT
          o.name as organization,
          COALESCE(u.preferences::jsonb ->> 'timezone', 'unknown') as timezone,
          COUNT(*) as user_count
      FROM users u
      JOIN organizations o ON u.organization_id = o.id
      WHERE u.deleted_at IS NULL
      GROUP BY o.name, COALESCE(u.preferences::jsonb ->> 'timezone', 'unknown')
      HAVING COUNT(*) > 0
      ORDER BY o.name, user_count DESC
      """, nativeQuery = true)
  List<Object[]> getUserGeographicDistribution();

  /**
   * Provides churn risk analysis by identifying users who have been inactive for a specified
   * period.
   *
   * @param churnThreshold the timestamp before which users are considered at risk of churning
   * @param maxResults the maximum number of at-risk users to return
   * @return a list of object arrays, where each array contains details about an at-risk user
   */
  @Query(value = """
      SELECT
          u.id,
          u.email,
          u.name,
          o.name as organization,
          u.updated_at as last_activity,
          EXTRACT(EPOCH FROM (NOW() - u.updated_at)) / 86400 as days_inactive
      FROM users u
      JOIN organizations o ON u.organization_id = o.id
      WHERE u.updated_at < :churnThreshold
          AND u.deleted_at IS NULL
      ORDER BY u.updated_at ASC
      LIMIT :maxResults
      """, nativeQuery = true)
  List<Object[]> getChurnRiskUsers(@Param("churnThreshold") Instant churnThreshold, @Param("maxResults") int maxResults);

  /**
   * Provides user engagement scoring by categorizing users into activity levels.
   *
   * @param organizationId the ID of the organization
   * @param highlyActiveThreshold the timestamp threshold for 'highly active' users
   * @param moderatelyActiveThreshold the timestamp threshold for 'moderately active' users
   * @param lowActiveThreshold the timestamp threshold for 'low active' users
   * @return a list of object arrays, where each array contains user details and their engagement
   *     level
   */
  @Query(value = """
      SELECT
          u.id,
          u.email,
          u.name,
          CASE
              WHEN u.updated_at >= :highlyActiveThreshold THEN 'highly_active'
              WHEN u.updated_at >= :moderatelyActiveThreshold THEN 'moderately_active'
              WHEN u.updated_at >= :lowActiveThreshold THEN 'low_active'
              ELSE 'inactive'
          END as engagement_level,
          u.updated_at as last_activity
      FROM users u
      WHERE u.organization_id = :organizationId AND u.deleted_at IS NULL
      ORDER BY u.updated_at DESC
      """, nativeQuery = true)
  List<Object[]> getUserEngagementScores(
      @Param("organizationId") UUID organizationId,
      @Param("highlyActiveThreshold") Instant highlyActiveThreshold,
      @Param("moderatelyActiveThreshold") Instant moderatelyActiveThreshold,
      @Param("lowActiveThreshold") Instant lowActiveThreshold);

  /**
   * Provides an advanced search for users with multiple filter criteria and pagination.
   *
   * @param searchTerm a term to search for in user names and emails
   * @param organizationId the ID of the organization to filter by
   * @param provider the name of the authentication provider to filter by
   * @param emailVerified the email verification status to filter by
   * @param createdAfter the start date of the creation period to filter by
   * @param createdBefore the end date of the creation period to filter by
   * @param pageable the pagination information
   * @return a list of users matching the search criteria
   */
  @Query(value = """
      SELECT u.* FROM users u
      JOIN organizations o ON u.organization_id = o.id
      WHERE u.deleted_at IS NULL
          AND (:searchTerm IS NULL OR
               LOWER(u.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR
               LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
          AND (:organizationId IS NULL OR u.organization_id = :organizationId)
          AND (:provider IS NULL OR u.provider = :provider)
          AND (:emailVerified IS NULL OR u.email_verified = :emailVerified)
          AND (:createdAfter IS NULL OR u.created_at >= :createdAfter)
          AND (:createdBefore IS NULL OR u.created_at <= :createdBefore)
      ORDER BY u.created_at DESC
      """, nativeQuery = true)
  List<User> advancedUserSearch(
      @Param("searchTerm") String searchTerm,
      @Param("organizationId") UUID organizationId,
      @Param("provider") String provider,
      @Param("emailVerified") Boolean emailVerified,
      @Param("createdAfter") Instant createdAfter,
      @Param("createdBefore") Instant createdBefore,
      Pageable pageable);
}
