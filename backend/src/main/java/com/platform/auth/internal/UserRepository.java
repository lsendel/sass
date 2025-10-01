package com.platform.auth.internal;

import com.platform.auth.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for User entity.
 * Internal to auth module - not accessible by other modules.
 *
 * @since 1.0.0
 */
interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Finds a user by email address.
     *
     * @param email the email address
     * @return optional containing the user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Finds an active user by email address.
     * Active means status is ACTIVE and not soft-deleted.
     *
     * @param email the email address
     * @return optional containing the active user if found
     */
    @Query("SELECT u FROM User u WHERE u.email = :email " +
           "AND u.status = 'ACTIVE' AND u.deletedAt IS NULL")
    Optional<User> findActiveUserByEmail(@Param("email") String email);

    /**
     * Checks if a user with the given email exists.
     *
     * @param email the email address
     * @return true if user exists
     */
    boolean existsByEmail(String email);

    /**
     * Checks if a user with the given email exists and is not soft-deleted.
     *
     * @param email the email address
     * @return true if active user exists
     */
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END " +
           "FROM User u WHERE u.email = :email AND u.deletedAt IS NULL")
    boolean existsActiveUserByEmail(@Param("email") String email);
}
