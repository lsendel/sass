package com.platform.user.internal;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link Invitation} entities.
 *
 * <p>This interface provides methods for querying and managing invitation records in the database.
 * It includes standard finder methods as well as custom queries for more complex scenarios, such as
 * finding expired or pending invitations.
 *
 * @see Invitation
 */
@Repository
public interface InvitationRepository extends JpaRepository<Invitation, UUID> {

  /**
   * Finds an invitation by its unique token.
   *
   * @param token the invitation token
   * @return an {@link Optional} containing the invitation if found, otherwise an empty {@link
   *     Optional}
   */
  Optional<Invitation> findByToken(String token);

  /**
   * Finds all invitations for a specific organization with a given status.
   *
   * @param organizationId the ID of the organization
   * @param status the status of the invitations to find
   * @return a list of invitations matching the criteria
   */
  List<Invitation> findByOrganizationIdAndStatus(UUID organizationId, Invitation.Status status);

  /**
   * Finds an invitation by the recipient's email address and organization ID.
   *
   * @param email the email address of the recipient
   * @param organizationId the ID of the organization
   * @return an {@link Optional} containing the invitation if found, otherwise an empty {@link
   *     Optional}
   */
  Optional<Invitation> findByEmailAndOrganizationId(String email, UUID organizationId);

  /**
   * Finds all invitations sent to a specific email address.
   *
   * @param email the email address to search for
   * @return a list of invitations sent to the specified email
   */
  List<Invitation> findByEmail(String email);

  /**
   * Finds all invitations sent by a specific user.
   *
   * @param invitedBy the ID of the user who sent the invitations
   * @return a list of invitations sent by the specified user
   */
  List<Invitation> findByInvitedBy(UUID invitedBy);

  /**
   * Finds all invitations for a specific organization.
   *
   * @param organizationId the ID of the organization
   * @return a list of all invitations for the specified organization
   */
  List<Invitation> findByOrganizationId(UUID organizationId);

  /**
   * Finds all pending invitations that have expired.
   *
   * @param now the current time, used to determine if an invitation has expired
   * @return a list of expired, pending invitations
   */
  @Query("SELECT i FROM Invitation i WHERE i.expiresAt < :now AND i.status = 'PENDING'")
  List<Invitation> findExpiredInvitations(@Param("now") Instant now);

  /**
   * Checks if an active, pending invitation exists for a specific email and organization.
   *
   * @param email the email address of the recipient
   * @param organizationId the ID of the organization
   * @param now the current time
   * @return {@code true} if an active invitation exists, {@code false} otherwise
   */
  @Query(
      "SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END FROM Invitation i WHERE i.email = :email AND i.organizationId = :organizationId AND i.status = 'PENDING' AND i.expiresAt > :now")
  boolean existsActiveInvitation(
      @Param("email") String email,
      @Param("organizationId") UUID organizationId,
      @Param("now") Instant now);

  /**
   * Counts the number of invitations for an organization with a specific status.
   *
   * @param organizationId the ID of the organization
   * @param status the status of the invitations to count
   * @return the number of invitations matching the criteria
   */
  long countByOrganizationIdAndStatus(UUID organizationId, Invitation.Status status);

  /**
   * Finds all pending invitations that are expiring soon.
   *
   * @param now the current time
   * @param warningTime the time to consider as the warning threshold for expiration
   * @return a list of invitations that are expiring soon
   */
  @Query(
      "SELECT i FROM Invitation i WHERE i.expiresAt BETWEEN :now AND :warningTime AND i.status = 'PENDING'")
  List<Invitation> findInvitationsExpiringSoon(
      @Param("now") Instant now, @Param("warningTime") Instant warningTime);

  /**
   * Deletes all invitations that expired before a given cutoff time.
   *
   * @param cutoffTime the time before which expired invitations should be deleted
   */
  @Query("DELETE FROM Invitation i WHERE i.expiresAt < :cutoffTime")
  void deleteExpiredInvitations(@Param("cutoffTime") Instant cutoffTime);

  /**
   * Finds all invitations with a specific status.
   *
   * @param status the status to search for
   * @return a list of invitations with the specified status
   */
  List<Invitation> findByStatus(Invitation.Status status);

  /**
   * Finds all recent invitations for a specific organization, created since a given time.
   *
   * @param organizationId the ID of the organization
   * @param since the time from which to find recent invitations
   * @return a list of recent invitations
   */
  @Query(
      "SELECT i FROM Invitation i WHERE i.organizationId = :organizationId AND i.createdAt >= :since ORDER BY i.createdAt DESC")
  List<Invitation> findRecentInvitations(
      @Param("organizationId") UUID organizationId, @Param("since") Instant since);

  /**
   * Finds a pending invitation for a specific organization and email address that has not yet
   * expired.
   *
   * @param organizationId the ID of the organization
   * @param email the email address of the recipient
   * @param now the current time
   * @return an {@link Optional} containing the pending invitation if found, otherwise an empty
   *     {@link Optional}
   */
  @Query(
      "SELECT i FROM Invitation i WHERE i.organizationId = :organizationId AND i.email = :email AND i.status = 'PENDING' AND i.expiresAt > :now")
  Optional<Invitation> findPendingInvitation(
      @Param("organizationId") UUID organizationId,
      @Param("email") String email,
      @Param("now") Instant now);

  /**
   * Finds a pending invitation for a specific organization and email address.
   *
   * <p>This is a convenience method that calls {@link #findPendingInvitation(UUID, String,
   * Instant)} with the current time.
   *
   * @param organizationId the ID of the organization
   * @param email the email address of the recipient
   * @return an {@link Optional} containing the pending invitation if found, otherwise an empty
   *     {@link Optional}
   */
  default Optional<Invitation> findPendingInvitation(UUID organizationId, String email) {
    return findPendingInvitation(organizationId, email, Instant.now());
  }

  /**
   * Finds an invitation by its token and status.
   *
   * @param token the invitation token
   * @param status the status of the invitation
   * @return an {@link Optional} containing the invitation if found, otherwise an empty {@link
   *     Optional}
   */
  Optional<Invitation> findByTokenAndStatus(String token, Invitation.Status status);

  /**
   * Finds all pending invitations for a specific organization, ordered by creation date in
   * descending order.
   *
   * @param organizationId the ID of the organization
   * @return a list of pending invitations for the organization
   */
  @Query(
      "SELECT i FROM Invitation i WHERE i.organizationId = :organizationId AND i.status = 'PENDING' ORDER BY i.createdAt DESC")
  List<Invitation> findPendingInvitationsForOrganization(
      @Param("organizationId") UUID organizationId);
}
