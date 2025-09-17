package com.platform.auth.internal;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Repository for OAuth2Session entities. Manages OAuth2 authentication sessions. */
@Repository
public interface OAuth2SessionRepository extends JpaRepository<OAuth2Session, Long> {

    /** Find session by session ID */
    Optional<OAuth2Session> findBySessionId(String sessionId);

    /** Find active session by session ID */
    Optional<OAuth2Session> findBySessionIdAndIsActiveTrue(String sessionId);

    /** Find all sessions for a user */
    List<OAuth2Session> findByUserInfo(OAuth2UserInfo userInfo);

    /** Find active sessions for a user */
    List<OAuth2Session> findByUserInfoAndIsActiveTrueAndTerminatedAtIsNull(OAuth2UserInfo userInfo);

    /** Find expired sessions */
    @Query("SELECT s FROM OAuth2Session s WHERE s.expiresAt < :now AND s.isActive = true")
    List<OAuth2Session> findExpiredSessions(@Param("now") Instant now);

    /** Count active sessions */
    long countByIsActiveTrueAndExpiresAtAfter(Instant now);

    /** Count active sessions by provider */
    long countByProviderAndIsActiveTrueAndExpiresAtAfter(String provider, Instant now);

    /** Find sessions by provider */
    List<OAuth2Session> findByProvider(String provider);

    /** Find sessions that need refresh */
    @Query(
            "SELECT s FROM OAuth2Session s WHERE s.isActive = true AND "
                    + "s.expiresAt > :now AND s.expiresAt < :refreshThreshold")
    List<OAuth2Session> findSessionsNeedingRefresh(
            @Param("now") Instant now, @Param("refreshThreshold") Instant refreshThreshold);

    /** Find sessions by IP address */
    List<OAuth2Session> findByCreatedFromIp(String ipAddress);

    /** Find sessions created within a time range */
    List<OAuth2Session> findByCreatedAtBetween(Instant start, Instant end);

    /** Delete terminated sessions older than a certain date */
    void deleteByTerminatedAtBeforeAndIsActiveFalse(Instant date);

    /** Check if session exists and is active */
    boolean existsBySessionIdAndIsActiveTrue(String sessionId);

    /** Find sessions by user agent pattern */
    @Query("SELECT s FROM OAuth2Session s WHERE s.createdFromUserAgent LIKE %:pattern%")
    List<OAuth2Session> findByUserAgentPattern(@Param("pattern") String pattern);

    /** Get session statistics by provider */
    @Query(
            "SELECT s.provider, COUNT(s), "
                    + "COUNT(CASE WHEN s.isActive = true THEN 1 END), "
                    + "COUNT(CASE WHEN s.terminatedAt IS NOT NULL THEN 1 END) "
                    + "FROM OAuth2Session s GROUP BY s.provider")
    List<Object[]> getSessionStatsByProvider();

    /** Find long-running sessions */
    @Query(
            "SELECT s FROM OAuth2Session s WHERE s.isActive = true AND "
                    + "s.createdAt < :threshold ORDER BY s.createdAt ASC")
    List<OAuth2Session> findLongRunningSessions(@Param("threshold") Instant threshold);

    /** Update last accessed timestamp for a session */
    @Query(
            "UPDATE OAuth2Session s SET s.lastAccessedAt = :now, s.lastAccessedFromIp = :ip "
                    + "WHERE s.sessionId = :sessionId")
    void updateLastAccessed(
            @Param("sessionId") String sessionId, @Param("now") Instant now, @Param("ip") String ip);
}
