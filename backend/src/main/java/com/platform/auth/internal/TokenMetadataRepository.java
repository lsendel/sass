package com.platform.auth.internal;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Repository interface for TokenMetadata entity operations. */
@Repository
public interface TokenMetadataRepository extends JpaRepository<TokenMetadata, UUID> {

    /** Find token metadata by token hash */
    Optional<TokenMetadata> findByTokenHash(String tokenHash);

    /** Find token metadata by lookup hash */
    Optional<TokenMetadata> findFirstByTokenLookupHashAndExpiresAtAfter(String tokenLookupHash, Instant now);

    /** Find all valid (non-expired) tokens */
    @Query("SELECT tm FROM TokenMetadata tm WHERE tm.expiresAt > :now")
    List<TokenMetadata> findValidTokens(@Param("now") Instant now);

    /** Find valid tokens for a specific user */
    @Query("SELECT tm FROM TokenMetadata tm WHERE tm.userId = :userId AND tm.expiresAt > :now")
    List<TokenMetadata> findValidUserTokens(@Param("userId") UUID userId, @Param("now") Instant now);

    /** Count valid tokens for a user */
    @Query("SELECT COUNT(tm) FROM TokenMetadata tm WHERE tm.userId = :userId AND tm.expiresAt > :now")
    long countValidUserTokens(@Param("userId") UUID userId, @Param("now") Instant now);

    /** Find tokens that will expire soon for renewal notifications */
    @Query(
            "SELECT tm FROM TokenMetadata tm WHERE tm.expiresAt BETWEEN :now AND :threshold AND tm.expiresAt > :now")
    List<TokenMetadata> findTokensExpiringSoon(
            @Param("now") Instant now, @Param("threshold") Instant threshold);

    /** Find stale tokens (not used for a long time) */
    @Query("SELECT tm FROM TokenMetadata tm WHERE tm.lastUsed < :threshold AND tm.expiresAt > :now")
    List<TokenMetadata> findStaleTokens(
            @Param("threshold") Instant threshold, @Param("now") Instant now);

    /** Revoke all tokens for a user by setting expiration to now */
    @Modifying
    @Query(
            "UPDATE TokenMetadata tm SET tm.expiresAt = :now WHERE tm.userId = :userId AND tm.expiresAt > :now")
    int revokeAllUserTokens(@Param("userId") UUID userId, @Param("now") Instant now);

    /** Revoke all tokens for a user (convenience method) */
    default int revokeAllUserTokens(UUID userId) {
        return revokeAllUserTokens(userId, Instant.now());
    }

    /** Delete expired tokens (cleanup) */
    @Modifying
    @Query("DELETE FROM TokenMetadata tm WHERE tm.expiresAt <= :expiredBefore")
    int deleteExpiredTokens(@Param("expiredBefore") Instant expiredBefore);

    /** Find tokens by user and session type */
    @Query(
            value =
                    "SELECT * FROM token_metadata tm WHERE tm.user_id = :userId "
                            + "AND tm.expires_at > :now "
                            + "AND (tm.metadata::jsonb) ->> 'sessionType' = :sessionType",
            nativeQuery = true)
    List<TokenMetadata> findUserTokensBySessionType(
            @Param("userId") UUID userId,
            @Param("sessionType") String sessionType,
            @Param("now") Instant now);

    /** Find tokens by IP address (for security analysis) */
    @Query(
            value =
                    "SELECT * FROM token_metadata tm WHERE tm.expires_at > :now "
                            + "AND (tm.metadata::jsonb) ->> 'ipAddress' = :ipAddress",
            nativeQuery = true)
    List<TokenMetadata> findTokensByIpAddress(
            @Param("ipAddress") String ipAddress, @Param("now") Instant now);

    /** Find all tokens for a user (including expired) */
    List<TokenMetadata> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /** Delete all tokens for a user (GDPR compliance) */
    @Modifying
    void deleteByUserId(UUID userId);

    /** Find tokens created within a time range */
    @Query("SELECT tm FROM TokenMetadata tm WHERE tm.createdAt BETWEEN :startTime AND :endTime")
    List<TokenMetadata> findTokensCreatedBetween(
            @Param("startTime") Instant startTime, @Param("endTime") Instant endTime);

    /** Count tokens by session type */
    @Query(
            value =
                    "SELECT (tm.metadata::jsonb) ->> 'sessionType' AS sessionType, COUNT(*) "
                            + "FROM token_metadata tm WHERE tm.expires_at > :now "
                            + "GROUP BY (tm.metadata::jsonb) ->> 'sessionType'",
            nativeQuery = true)
    List<Object[]> countTokensBySessionType(@Param("now") Instant now);
}
