package com.platform.user.internal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for User entity operations.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Find user by email (excluding soft-deleted users)
     */
    @Query("SELECT u FROM User u WHERE u.email.value = :email AND u.deletedAt IS NULL")
    Optional<User> findByEmailAndDeletedAtIsNull(@Param("email") String email);

    /**
     * Find user by OAuth provider and provider ID
     */
    Optional<User> findByProviderAndProviderId(String provider, String providerId);

    /**
     * Find user by email (including soft-deleted for admin purposes)
     */
    @Query("SELECT u FROM User u WHERE u.email.value = :email")
    Optional<User> findByEmail(@Param("email") String email);

    /**
     * Find all active users (not soft-deleted)
     */
    @Query("SELECT u FROM User u WHERE u.deletedAt IS NULL ORDER BY u.createdAt DESC")
    List<User> findAllActive();

    /**
     * Find all active users with pagination
     */
    @Query("SELECT u FROM User u WHERE u.deletedAt IS NULL")
    Page<User> findByDeletedAtIsNull(Pageable pageable);

    /**
     * Find users by provider
     */
    List<User> findByProviderAndDeletedAtIsNull(String provider);

    /**
     * Find users created within a date range
     */
    @Query("SELECT u FROM User u WHERE u.createdAt BETWEEN :startDate AND :endDate AND u.deletedAt IS NULL")
    List<User> findUsersCreatedBetween(@Param("startDate") Instant startDate,
                                     @Param("endDate") Instant endDate);

    /**
     * Count active users
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.deletedAt IS NULL")
    long countActiveUsers();

    /**
     * Count users by provider
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.provider = :provider AND u.deletedAt IS NULL")
    long countUsersByProvider(@Param("provider") String provider);

    /**
     * Find users with specific preference
     */
    @Query("SELECT u FROM User u WHERE JSON_EXTRACT(u.preferences, :jsonPath) = :value AND u.deletedAt IS NULL")
    List<User> findByPreference(@Param("jsonPath") String jsonPath, @Param("value") String value);

    /**
     * Check if email exists (excluding soft-deleted)
     */
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.email.value = :email AND u.deletedAt IS NULL")
    boolean existsByEmailAndDeletedAtIsNull(@Param("email") String email);

    /**
     * Find users by name pattern (case-insensitive)
     */
    @Query("SELECT u FROM User u WHERE LOWER(u.name) LIKE LOWER(CONCAT('%', :namePattern, '%')) AND u.deletedAt IS NULL")
    List<User> findByNameContainingIgnoreCase(@Param("namePattern") String namePattern);

    /**
     * Soft delete user by setting deletedAt timestamp
     */
    @Query("UPDATE User u SET u.deletedAt = :deletedAt WHERE u.id = :userId")
    void softDeleteUser(@Param("userId") UUID userId, @Param("deletedAt") Instant deletedAt);

    /**
     * Restore soft-deleted user
     */
    @Query("UPDATE User u SET u.deletedAt = NULL WHERE u.id = :userId")
    void restoreUser(@Param("userId") UUID userId);

    /**
     * Find users by multiple providers
     */
    @Query("SELECT u FROM User u WHERE u.provider IN :providers AND u.deletedAt IS NULL")
    List<User> findByProviderIn(@Param("providers") List<String> providers);

    /**
     * Find recently active users (based on updatedAt)
     */
    @Query("SELECT u FROM User u WHERE u.updatedAt >= :since AND u.deletedAt IS NULL ORDER BY u.updatedAt DESC")
    List<User> findRecentlyActive(@Param("since") Instant since);

    /**
     * Custom method to find users for GDPR data export
     */
    @Query("SELECT u FROM User u WHERE u.email.value = :email")
    List<User> findAllByEmailForDataExport(@Param("email") String email);
}