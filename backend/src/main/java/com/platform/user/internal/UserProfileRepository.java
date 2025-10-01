package com.platform.user.internal;

import com.platform.user.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for UserProfile entity.
 *
 * @since 1.0.0
 */
interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

    /**
     * Finds a user profile by email.
     *
     * @param email the email address
     * @return the user profile if found
     */
    Optional<UserProfile> findByEmail(String email);

    /**
     * Finds an active user profile by email (not deleted).
     *
     * @param email the email address
     * @return the user profile if found and active
     */
    @Query("SELECT u FROM UserProfile u WHERE u.email = :email " +
           "AND u.deletedAt IS NULL")
    Optional<UserProfile> findActiveByEmail(@Param("email") String email);

    /**
     * Finds all users in an organization.
     *
     * @param organizationId the organization ID
     * @return list of users in the organization
     */
    @Query("SELECT u FROM UserProfile u WHERE u.organization.id = :orgId " +
           "AND u.deletedAt IS NULL ORDER BY u.createdAt DESC")
    List<UserProfile> findByOrganizationId(@Param("orgId") UUID organizationId);

    /**
     * Checks if a user exists in an organization.
     *
     * @param email the email address
     * @param organizationId the organization ID
     * @return true if user exists in the organization
     */
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END " +
           "FROM UserProfile u WHERE u.email = :email " +
           "AND u.organization.id = :orgId AND u.deletedAt IS NULL")
    boolean existsByEmailAndOrganizationId(
        @Param("email") String email,
        @Param("orgId") UUID organizationId
    );

    /**
     * Counts active users in an organization.
     *
     * @param organizationId the organization ID
     * @return count of active users
     */
    @Query("SELECT COUNT(u) FROM UserProfile u " +
           "WHERE u.organization.id = :orgId AND u.deletedAt IS NULL")
    long countByOrganizationId(@Param("orgId") UUID organizationId);
}
