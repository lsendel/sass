package com.platform.auth.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing OAuth2 user information and provider data synchronization.
 * Handles user information lifecycle, data updates from providers, and privacy controls.
 *
 * This service manages OAuth2UserInfo entities which store provider-specific
 * user information separately from the main User entities in the platform.
 */
@Service
@Transactional
public class OAuth2UserService {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2UserService.class);

    private final OAuth2UserInfoRepository userInfoRepository;
    private final OAuth2ProviderRepository providerRepository;
    private final OAuth2AuditService auditService;

    public OAuth2UserService(OAuth2UserInfoRepository userInfoRepository,
                            OAuth2ProviderRepository providerRepository,
                            OAuth2AuditService auditService) {
        this.userInfoRepository = userInfoRepository;
        this.providerRepository = providerRepository;
        this.auditService = auditService;
    }

    /**
     * Find or create OAuth2 user information from provider data
     */
    public OAuth2UserInfo findOrCreateUserInfo(String providerUserId, String provider, String email,
                                              String name, String givenName, String familyName,
                                              String picture, String locale, Boolean emailVerified,
                                              String rawAttributes) {

        // Validate provider is configured
        OAuth2Provider oauthProvider = providerRepository.findByNameAndEnabledTrue(provider)
            .orElseThrow(() -> new IllegalArgumentException("Provider not configured: " + provider));

        // Try to find existing user info by provider and provider user ID
        Optional<OAuth2UserInfo> existingUserInfo = userInfoRepository
            .findByProviderUserIdAndProvider(providerUserId, provider);

        if (existingUserInfo.isPresent()) {
            // Update existing user info
            OAuth2UserInfo userInfo = existingUserInfo.get();
            updateUserInfo(userInfo, email, name, givenName, familyName, picture, locale, emailVerified, rawAttributes);

            auditService.logUserInfoRetrieved(provider, providerUserId, null);
            return userInfoRepository.save(userInfo);
        } else {
            // Create new user info
            OAuth2UserInfo newUserInfo = new OAuth2UserInfo(providerUserId, provider, email);
            updateUserInfo(newUserInfo, email, name, givenName, familyName, picture, locale, emailVerified, rawAttributes);

            OAuth2UserInfo savedUserInfo = userInfoRepository.save(newUserInfo);

            logger.info("Created new OAuth2 user info: provider={}, providerUserId={}, email={}",
                       provider, providerUserId, email);

            return savedUserInfo;
        }
    }

    /**
     * Get OAuth2 user information by provider and provider user ID
     */
    @Transactional(readOnly = true)
    public Optional<OAuth2UserInfo> getUserInfo(String providerUserId, String provider) {
        return userInfoRepository.findByProviderUserIdAndProvider(providerUserId, provider);
    }

    /**
     * Get OAuth2 user information by email and provider
     */
    @Transactional(readOnly = true)
    public Optional<OAuth2UserInfo> getUserInfoByEmail(String email, String provider) {
        return userInfoRepository.findByEmailAndProvider(email, provider);
    }

    /**
     * Get all OAuth2 user information for a specific email across providers
     */
    @Transactional(readOnly = true)
    public List<OAuth2UserInfo> getAllUserInfoForEmail(String email) {
        return userInfoRepository.findByEmailOrderByCreatedAtDesc(email);
    }

    /**
     * Update user information from provider
     */
    public OAuth2UserInfo updateUserInfoFromProvider(String providerUserId, String provider,
                                                     String email, String name, String givenName,
                                                     String familyName, String picture, String locale,
                                                     Boolean emailVerified, String rawAttributes) {

        OAuth2UserInfo userInfo = userInfoRepository.findByProviderUserIdAndProvider(providerUserId, provider)
            .orElseThrow(() -> new IllegalArgumentException("User info not found for provider: " + provider));

        updateUserInfo(userInfo, email, name, givenName, familyName, picture, locale, emailVerified, rawAttributes);

        OAuth2UserInfo savedUserInfo = userInfoRepository.save(userInfo);

        auditService.logUserInfoRetrieved(provider, providerUserId, null);

        logger.info("Updated OAuth2 user info from provider: provider={}, providerUserId={}",
                   provider, providerUserId);

        return savedUserInfo;
    }

    /**
     * Check if user information needs refresh from provider
     */
    @Transactional(readOnly = true)
    public boolean needsRefreshFromProvider(String providerUserId, String provider) {
        return userInfoRepository.findByProviderUserIdAndProvider(providerUserId, provider)
            .map(OAuth2UserInfo::needsRefreshFromProvider)
            .orElse(true);
    }

    /**
     * Get users that need refresh from providers
     */
    @Transactional(readOnly = true)
    public List<OAuth2UserInfo> getUsersNeedingRefresh(int limit) {
        Instant refreshThreshold = Instant.now().minusSeconds(24 * 60 * 60); // 24 hours ago
        return userInfoRepository.findUsersNeedingRefresh(refreshThreshold, limit);
    }

    /**
     * Delete user information (GDPR compliance)
     */
    public void deleteUserInfo(String providerUserId, String provider, String requestedBy) {
        Optional<OAuth2UserInfo> userInfoOpt = userInfoRepository
            .findByProviderUserIdAndProvider(providerUserId, provider);

        if (userInfoOpt.isPresent()) {
            OAuth2UserInfo userInfo = userInfoOpt.get();

            // Log data deletion for audit (GDPR compliance)
            auditService.logUserInfoDeleted(provider, providerUserId, requestedBy, null);

            userInfoRepository.delete(userInfo);

            logger.info("Deleted OAuth2 user info: provider={}, providerUserId={}, requestedBy={}",
                       provider, providerUserId, requestedBy);
        }
    }

    /**
     * Get user information statistics for monitoring
     */
    @Transactional(readOnly = true)
    public OAuth2UserStats getUserStats() {
        long totalUsers = userInfoRepository.count();
        long verifiedEmailUsers = userInfoRepository.countByEmailVerifiedTrue();
        long usersWithPictures = userInfoRepository.countByPictureIsNotNull();

        return new OAuth2UserStats(totalUsers, verifiedEmailUsers, usersWithPictures);
    }

    /**
     * Get user information statistics by provider
     */
    @Transactional(readOnly = true)
    public List<OAuth2ProviderStats> getProviderStats() {
        return userInfoRepository.findProviderStats();
    }

    /**
     * Validate user email verification status
     */
    @Transactional(readOnly = true)
    public boolean isEmailVerified(String email, String provider) {
        return userInfoRepository.findByEmailAndProvider(email, provider)
            .map(OAuth2UserInfo::isEmailVerified)
            .orElse(false);
    }

    /**
     * Get user's display name with fallback logic
     */
    @Transactional(readOnly = true)
    public String getUserDisplayName(String providerUserId, String provider) {
        return userInfoRepository.findByProviderUserIdAndProvider(providerUserId, provider)
            .map(OAuth2UserInfo::getDisplayName)
            .orElse("Unknown User");
    }

    /**
     * Update user timezone if provided
     */
    public void updateUserTimezone(String providerUserId, String provider, String timezone) {
        userInfoRepository.findByProviderUserIdAndProvider(providerUserId, provider)
            .ifPresent(userInfo -> {
                userInfo.setTimezone(timezone);
                userInfoRepository.save(userInfo);

                logger.debug("Updated timezone for OAuth2 user: provider={}, providerUserId={}, timezone={}",
                           provider, providerUserId, timezone);
            });
    }

    /**
     * Search users by name or email (for admin purposes)
     */
    @Transactional(readOnly = true)
    public List<OAuth2UserInfo> searchUsers(String query, int limit) {
        return userInfoRepository.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            query, query, limit);
    }

    /**
     * Get recently created users for monitoring
     */
    @Transactional(readOnly = true)
    public List<OAuth2UserInfo> getRecentlyCreatedUsers(Instant since, int limit) {
        return userInfoRepository.findByCreatedAtAfterOrderByCreatedAtDesc(since, limit);
    }

    /**
     * Get users with unverified emails
     */
    @Transactional(readOnly = true)
    public List<OAuth2UserInfo> getUsersWithUnverifiedEmails(String provider, int limit) {
        return userInfoRepository.findByProviderAndEmailVerifiedFalseOrEmailVerifiedIsNull(provider, limit);
    }

    // Private helper methods

    private void updateUserInfo(OAuth2UserInfo userInfo, String email, String name, String givenName,
                               String familyName, String picture, String locale, Boolean emailVerified,
                               String rawAttributes) {

        userInfo.updateFromProvider(email, name, givenName, familyName, picture, locale, emailVerified, rawAttributes);

        logger.debug("Updated OAuth2 user info: provider={}, providerUserId={}, email={}",
                    userInfo.getProvider(), userInfo.getProviderUserId(), email);
    }

    /**
     * User statistics record
     */
    public record OAuth2UserStats(
        long totalUsers,
        long verifiedEmailUsers,
        long usersWithPictures
    ) {}

    /**
     * Provider statistics record
     */
    public record OAuth2ProviderStats(
        String provider,
        long userCount,
        long verifiedEmailCount
    ) {}

    /**
     * User information result for API responses
     */
    public record OAuth2UserInfoResult(
        String providerUserId,
        String provider,
        String email,
        String name,
        String displayName,
        String picture,
        String locale,
        boolean emailVerified,
        Instant lastUpdated
    ) {
        public static OAuth2UserInfoResult from(OAuth2UserInfo userInfo) {
            return new OAuth2UserInfoResult(
                userInfo.getProviderUserId(),
                userInfo.getProvider(),
                userInfo.getEmail(),
                userInfo.getName(),
                userInfo.getDisplayName(),
                userInfo.getPicture(),
                userInfo.getLocale(),
                userInfo.isEmailVerified(),
                userInfo.getLastUpdatedFromProvider()
            );
        }
    }

    /**
     * Exception for OAuth2 user service operations
     */
    public static class OAuth2UserServiceException extends RuntimeException {
        public OAuth2UserServiceException(String message) {
            super(message);
        }

        public OAuth2UserServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}