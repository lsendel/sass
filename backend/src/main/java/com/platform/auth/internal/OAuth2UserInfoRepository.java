package com.platform.auth.internal;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for OAuth2UserInfo entities. Manages user information retrieved from OAuth2 providers.
 */
@Repository
public interface OAuth2UserInfoRepository extends JpaRepository<OAuth2UserInfo, Long> {

  /** Find user info by provider user ID and provider */
  Optional<OAuth2UserInfo> findByProviderUserIdAndProvider(String providerUserId, String provider);

  /** Find user info by email and provider */
  Optional<OAuth2UserInfo> findByEmailAndProvider(String email, String provider);

  /** Find all user info for a specific email across all providers */
  List<OAuth2UserInfo> findByEmailOrderByCreatedAtDesc(String email);

  /** Check if user exists for a provider */
  boolean existsByProviderUserIdAndProvider(String providerUserId, String provider);

  /** Count users with verified emails */
  long countByEmailVerifiedTrue();

  /** Count users with profile pictures */
  long countByPictureIsNotNull();

  /** Find users that need refresh from provider */
  @Query(
      "SELECT u FROM OAuth2UserInfo u WHERE u.lastUpdatedFromProvider < :threshold ORDER BY u.lastUpdatedFromProvider ASC")
  List<OAuth2UserInfo> findUsersNeedingRefresh(
      @Param("threshold") Instant threshold, Pageable pageable);

  /** Find users that need refresh from provider (limited) */
  default List<OAuth2UserInfo> findUsersNeedingRefresh(Instant threshold, int limit) {
    return findUsersNeedingRefresh(threshold, Pageable.ofSize(limit));
  }

  /** Get provider statistics */
  @Query(
      "SELECT new com.platform.auth.internal.OAuth2UserService$OAuth2ProviderStats("
          + "u.provider, COUNT(u), COUNT(CASE WHEN u.emailVerified = true THEN 1 END)) "
          + "FROM OAuth2UserInfo u GROUP BY u.provider")
  List<OAuth2UserService.OAuth2ProviderStats> findProviderStats();

  /** Search users by name or email */
  @Query(
      "SELECT u FROM OAuth2UserInfo u WHERE "
          + "LOWER(u.name) LIKE LOWER(CONCAT('%', :nameQuery, '%')) OR "
          + "LOWER(u.email) LIKE LOWER(CONCAT('%', :emailQuery, '%')) "
          + "ORDER BY u.createdAt DESC")
  List<OAuth2UserInfo> findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
      @Param("nameQuery") String nameQuery,
      @Param("emailQuery") String emailQuery,
      Pageable pageable);

  /** Search users by name or email (limited) */
  default List<OAuth2UserInfo> findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
      String nameQuery, String emailQuery, int limit) {
    return findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
        nameQuery, emailQuery, Pageable.ofSize(limit));
  }

  /** Find recently created users */
  @Query("SELECT u FROM OAuth2UserInfo u WHERE u.createdAt > :since ORDER BY u.createdAt DESC")
  List<OAuth2UserInfo> findByCreatedAtAfterOrderByCreatedAtDesc(
      @Param("since") Instant since, Pageable pageable);

  /** Find recently created users (limited) */
  default List<OAuth2UserInfo> findByCreatedAtAfterOrderByCreatedAtDesc(Instant since, int limit) {
    return findByCreatedAtAfterOrderByCreatedAtDesc(since, Pageable.ofSize(limit));
  }

  /** Find users with unverified emails for a provider */
  @Query(
      "SELECT u FROM OAuth2UserInfo u WHERE u.provider = :provider AND "
          + "(u.emailVerified = false OR u.emailVerified IS NULL) "
          + "ORDER BY u.createdAt DESC")
  List<OAuth2UserInfo> findByProviderAndEmailVerifiedFalseOrEmailVerifiedIsNull(
      @Param("provider") String provider, Pageable pageable);

  /** Find users with unverified emails for a provider (limited) */
  default List<OAuth2UserInfo> findByProviderAndEmailVerifiedFalseOrEmailVerifiedIsNull(
      String provider, int limit) {
    return findByProviderAndEmailVerifiedFalseOrEmailVerifiedIsNull(
        provider, Pageable.ofSize(limit));
  }

  /** Delete user info older than a certain date (GDPR compliance) */
  void deleteByCreatedAtBefore(Instant date);

  /** Count users by provider */
  long countByProvider(String provider);
}
