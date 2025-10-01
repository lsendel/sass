package com.platform.auth.internal;

import com.platform.auth.User;
import com.platform.auth.events.UserAuthenticatedEvent;
import com.platform.shared.exceptions.ValidationException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Service for user authentication operations.
 * Handles password-based authentication and token generation.
 *
 * @since 1.0.0
 */
@Service
public final class AuthenticationService {

        private static final int MAX_LOGIN_ATTEMPTS = 5;
        private static final long LOCK_DURATION_MINUTES = 30;

        private final UserRepository userRepository;
        private final PasswordEncoder passwordEncoder;
        private final OpaqueTokenService tokenService;
        private final ApplicationEventPublisher eventPublisher;

        /**
         * Constructor with dependency injection.
         */
        public AuthenticationService(
                final UserRepository userRepository,
                final PasswordEncoder passwordEncoder,
                final OpaqueTokenService tokenService,
                final ApplicationEventPublisher eventPublisher) {
                this.userRepository = userRepository;
                this.passwordEncoder = passwordEncoder;
                this.tokenService = tokenService;
                this.eventPublisher = eventPublisher;
        }

        /**
         * Authenticates a user with email and password.
         * Generates an opaque token on successful authentication.
         *
         * @param email the user's email
         * @param password the user's password (plain text)
         * @return an opaque authentication token
         * @throws ValidationException if credentials are invalid or account is locked
         */
        @Transactional
        public String authenticate(final String email, final String password) {
                final User user = userRepository.findByEmail(email)
                        .orElseThrow(() -> new ValidationException("Invalid credentials"));

                // Check if account is locked
                if (user.isLocked()) {
                        throw new ValidationException(
                                "Account is temporarily locked. Please try again later."
                        );
                }

                // Check if account is active
                if (!user.isActive() && user.getStatus() != User.UserStatus.PENDING_VERIFICATION) {
                        throw new ValidationException("Account is not active");
                }

                // Verify password
                if (!passwordEncoder.matches(password, user.getPasswordHash())) {
                        handleFailedLogin(user);
                        throw new ValidationException("Invalid credentials");
                }

                // Auto-verify on first successful login if pending
                if (user.getStatus() == User.UserStatus.PENDING_VERIFICATION) {
                        user.verify();
                }

                // Reset failed attempts on successful login
                resetFailedAttempts(user);

                // Generate token
                final String token = tokenService.generateToken(user);

                // Publish authentication event for audit logging
                eventPublisher.publishEvent(new UserAuthenticatedEvent(
                        user.getId(),
                        user.getEmail(),
                        Instant.now()
                ));

                return token;
        }

        /**
         * Revokes a user's authentication token (logout).
         *
         * @param token the token to revoke
         */
        @Transactional(readOnly = true)
        public void revokeToken(final String token) {
                tokenService.revokeToken(token);
        }

        /**
         * Handles a failed login attempt by incrementing counter and locking if necessary.
         *
         * @param user the user who failed to authenticate
         */
        private void handleFailedLogin(final User user) {
                user.recordFailedLogin(MAX_LOGIN_ATTEMPTS, LOCK_DURATION_MINUTES);
                userRepository.save(user);
        }

        /**
         * Resets failed login attempts after successful authentication.
         *
         * @param user the user who successfully authenticated
         */
        private void resetFailedAttempts(final User user) {
                if (user.getFailedLoginAttempts() > 0) {
                        user.resetFailedAttempts();
                        userRepository.save(user);
                }
        }
}
