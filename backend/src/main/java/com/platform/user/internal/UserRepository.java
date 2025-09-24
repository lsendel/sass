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
}
