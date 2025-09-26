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
 * Repository interface for User entity operations. Supports both OAuth2 and password authentication
 * with multi-tenant organization isolation.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

  /**
   * Find user by email and organization (excluding soft-deleted users) Multi-tenant aware - same
   * email can exist in different organizations
   */
  @Query(
      "SELECT u FROM User u WHERE u.email.value = :email AND u.organization.id = :organizationId AND u.deletedAt IS NULL")
  Optional<User> findByEmailAndOrganizationIdAndDeletedAtIsNull(
      @Param("email") String email, @Param("organizationId") UUID organizationId);

  /** Find users by email across all organizations (for admin purposes) */
  @Query("SELECT u FROM User u WHERE u.email.value = :email AND u.deletedAt IS NULL")
  List<User> findByEmailAcrossOrganizations(@Param("email") String email);

  /** Find user by OAuth provider and provider ID */
  Optional<User> findByProviderAndProviderId(String provider, String providerId);

  /** Find user by email (including soft-deleted for admin purposes) */
  @Query("SELECT u FROM User u WHERE u.email.value = :email")
  List<User> findByEmail(@Param("email") String email);

  /** Find user by email and organization (including soft-deleted) */
  @Query(
      "SELECT u FROM User u WHERE u.email.value = :email AND u.organization.id = :organizationId")
  Optional<User> findByEmailAndOrganizationId(
      @Param("email") String email, @Param("organizationId") UUID organizationId);

  /** Find all active users (not soft-deleted) */
  @Query("SELECT u FROM User u WHERE u.deletedAt IS NULL ORDER BY u.createdAt DESC")
  List<User> findAllActive();

  /** Find all active users with pagination */
  @Query("SELECT u FROM User u WHERE u.deletedAt IS NULL")
  Page<User> findByDeletedAtIsNull(Pageable pageable);

  /** Find users by provider */
  List<User> findByProviderAndDeletedAtIsNull(String provider);

  /** Find users created within a date range */
  @Query(
      "SELECT u FROM User u WHERE u.createdAt BETWEEN :startDate AND :endDate AND u.deletedAt IS NULL")
  List<User> findUsersCreatedBetween(
      @Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

  /** Count active users */
  @Query("SELECT COUNT(u) FROM User u WHERE u.deletedAt IS NULL")
  long countActiveUsers();

  /** Count users by provider */
  @Query("SELECT COUNT(u) FROM User u WHERE u.provider = :provider AND u.deletedAt IS NULL")
  long countUsersByProvider(@Param("provider") String provider);

  /** Find users with specific preference */
  @Query(
      value =
          "SELECT * FROM users u WHERE (u.preferences::jsonb) ->> :preferenceKey = :value AND u.deleted_at IS NULL",
      nativeQuery = true)
  List<User> findByPreference(
      @Param("preferenceKey") String preferenceKey, @Param("value") String value);

  /** Check if email exists in organization (excluding soft-deleted) */
  @Query(
      "SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.email.value = :email AND u.organization.id = :organizationId AND u.deletedAt IS NULL")
  boolean existsByEmailAndOrganizationIdAndDeletedAtIsNull(
      @Param("email") String email, @Param("organizationId") UUID organizationId);

  /** Find users by display name pattern (case-insensitive) */
  @Query(
      "SELECT u FROM User u WHERE LOWER(u.name) LIKE LOWER(CONCAT('%', :namePattern, '%')) AND u.deletedAt IS NULL")
  List<User> findByNameContainingIgnoreCase(@Param("namePattern") String namePattern);

  /** Soft delete user by setting deletedAt timestamp */
  @Query("UPDATE User u SET u.deletedAt = :deletedAt WHERE u.id = :userId")
  void softDeleteUser(@Param("userId") UUID userId, @Param("deletedAt") Instant deletedAt);

  /** Restore soft-deleted user */
  @Query("UPDATE User u SET u.deletedAt = NULL WHERE u.id = :userId")
  void restoreUser(@Param("userId") UUID userId);

  /** Find users by multiple providers */
  @Query("SELECT u FROM User u WHERE u.provider IN :providers AND u.deletedAt IS NULL")
  List<User> findByProviderIn(@Param("providers") List<String> providers);

  /** Find recently active users (based on updatedAt) */
  @Query(
      "SELECT u FROM User u WHERE u.updatedAt >= :since AND u.deletedAt IS NULL ORDER BY u.updatedAt DESC")
  List<User> findRecentlyActive(@Param("since") Instant since);

  /** Custom method to find users for GDPR data export */
  @Query("SELECT u FROM User u WHERE u.email.value = :email")
  List<User> findAllByEmailForDataExport(@Param("email") String email);

  // Password authentication specific queries

  /** Find user by password reset token */
  @Query("SELECT u FROM User u WHERE u.passwordResetToken = :token AND u.deletedAt IS NULL")
  Optional<User> findByPasswordResetToken(@Param("token") String token);

  /** Find user by email verification token */
  @Query("SELECT u FROM User u WHERE u.emailVerificationToken = :token AND u.deletedAt IS NULL")
  Optional<User> findByEmailVerificationToken(@Param("token") String token);

  /** Find users with password authentication method */
  @Query(
      "SELECT u FROM User u JOIN u.authenticationMethods am WHERE am = 'PASSWORD' AND u.deletedAt IS NULL")
  List<User> findUsersWithPasswordAuth();

  /** Count users with email verification pending */
  @Query("SELECT COUNT(u) FROM User u WHERE u.emailVerified = false AND u.deletedAt IS NULL")
  long countUnverifiedUsers();

  /** Find locked users (account lockout) */
  @Query("SELECT u FROM User u WHERE u.lockoutExpiresAt > :currentTime AND u.deletedAt IS NULL")
  List<User> findLockedUsers(@Param("currentTime") Instant currentTime);

  /** Search users by name or email within organization */
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

  /** Find users by organization */
  @Query(
      "SELECT u FROM User u WHERE u.organization.id = :organizationId AND u.deletedAt IS NULL ORDER BY u.createdAt DESC")
  List<User> findByOrganizationId(@Param("organizationId") UUID organizationId);

  /**
   * Find user by email (single result, excluding soft-deleted) Legacy method compatibility - used
   * by SessionService and UserService
   */
  @Query("SELECT u FROM User u WHERE u.email.value = :email AND u.deletedAt IS NULL")
  Optional<User> findByEmailAndDeletedAtIsNull(@Param("email") String email);

  /** Check if email exists (excluding soft-deleted) - legacy compatibility */
  @Query(
      "SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.email.value = :email AND u.deletedAt IS NULL")
  boolean existsByEmailAndDeletedAtIsNull(@Param("email") String email);

  /** Count users with activity after a specific date */
  @Query("SELECT COUNT(u) FROM User u WHERE u.updatedAt >= :sinceDate AND u.deletedAt IS NULL")
  long countUsersActiveAfter(@Param("sinceDate") Instant sinceDate);

  /** Find users with activity after a specific date */
  @Query("SELECT u FROM User u WHERE u.updatedAt >= :sinceDate AND u.deletedAt IS NULL ORDER BY u.updatedAt DESC")
  List<User> findUsersActiveAfter(@Param("sinceDate") Instant sinceDate);

  // ===== ADVANCED ANALYTICS QUERIES =====

  /** User growth analytics - count users created by month */
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

  /** User retention analytics - find users active in both periods */
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

  /** Authentication method distribution analytics */
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

  /** Organization size analytics */
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

  /** User activity patterns - daily, weekly, monthly */
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

  /** Find users with suspicious email patterns */
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

  /** User lifecycle funnel analytics */
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

  /** Geographic distribution by organization */
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

  /** Churn risk analysis - users inactive for specified period */
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

  /** User engagement scoring */
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

  /** Advanced search with filters */
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
