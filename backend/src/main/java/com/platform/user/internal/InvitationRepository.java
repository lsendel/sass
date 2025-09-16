package com.platform.user.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Invitation entity operations.
 */
@Repository
public interface InvitationRepository extends JpaRepository<Invitation, UUID> {

    /**
     * Find invitation by token
     */
    Optional<Invitation> findByToken(String token);

    /**
     * Find pending invitations for organization
     */
    List<Invitation> findByOrganizationIdAndStatus(UUID organizationId, Invitation.Status status);

    /**
     * Find invitation by email and organization
     */
    Optional<Invitation> findByEmailAndOrganizationId(String email, UUID organizationId);

    /**
     * Find invitations by email
     */
    List<Invitation> findByEmail(String email);

    /**
     * Find invitations by inviter
     */
    List<Invitation> findByInvitedBy(UUID invitedBy);

    /**
     * Find invitations by organization
     */
    List<Invitation> findByOrganizationId(UUID organizationId);

    /**
     * Find expired invitations
     */
    @Query("SELECT i FROM Invitation i WHERE i.expiresAt < :now AND i.status = 'PENDING'")
    List<Invitation> findExpiredInvitations(@Param("now") Instant now);

    /**
     * Check if active invitation exists for email and organization
     */
    @Query("SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END FROM Invitation i WHERE i.email = :email AND i.organizationId = :organizationId AND i.status = 'PENDING' AND i.expiresAt > :now")
    boolean existsActiveInvitation(@Param("email") String email, @Param("organizationId") UUID organizationId, @Param("now") Instant now);

    /**
     * Count pending invitations for organization
     */
    long countByOrganizationIdAndStatus(UUID organizationId, Invitation.Status status);

    /**
     * Find invitations expiring soon
     */
    @Query("SELECT i FROM Invitation i WHERE i.expiresAt BETWEEN :now AND :warningTime AND i.status = 'PENDING'")
    List<Invitation> findInvitationsExpiringSoon(@Param("now") Instant now, @Param("warningTime") Instant warningTime);

    /**
     * Delete expired invitations
     */
    @Query("DELETE FROM Invitation i WHERE i.expiresAt < :cutoffTime")
    void deleteExpiredInvitations(@Param("cutoffTime") Instant cutoffTime);

    /**
     * Find invitations by status
     */
    List<Invitation> findByStatus(Invitation.Status status);

    /**
     * Find recent invitations for organization
     */
    @Query("SELECT i FROM Invitation i WHERE i.organizationId = :organizationId AND i.createdAt >= :since ORDER BY i.createdAt DESC")
    List<Invitation> findRecentInvitations(@Param("organizationId") UUID organizationId, @Param("since") Instant since);

    /**
     * Find pending invitation for organization and email
     */
    @Query("SELECT i FROM Invitation i WHERE i.organizationId = :organizationId AND i.email = :email AND i.status = 'PENDING' AND i.expiresAt > :now")
    Optional<Invitation> findPendingInvitation(@Param("organizationId") UUID organizationId, @Param("email") String email, @Param("now") Instant now);

    /**
     * Find pending invitation without time check (for service method compatibility)
     */
    default Optional<Invitation> findPendingInvitation(UUID organizationId, String email) {
        return findPendingInvitation(organizationId, email, Instant.now());
    }

    /**
     * Find invitation by token and status
     */
    Optional<Invitation> findByTokenAndStatus(String token, Invitation.Status status);

    /**
     * Find pending invitations for organization
     */
    @Query("SELECT i FROM Invitation i WHERE i.organizationId = :organizationId AND i.status = 'PENDING' ORDER BY i.createdAt DESC")
    List<Invitation> findPendingInvitationsForOrganization(@Param("organizationId") UUID organizationId);
}