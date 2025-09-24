package com.platform.auth.internal;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.platform.shared.security.PasswordProperties;
import com.platform.user.internal.Organization;
import com.platform.user.internal.OrganizationRepository;
import com.platform.user.internal.User;
import com.platform.user.internal.UserRepository;

/**
 * Service for password-based authentication operations. Handles user registration, authentication,
 * password reset, and account management.
 */
@Service
@Transactional
public class PasswordAuthService {

  private static final Logger log = LoggerFactory.getLogger(PasswordAuthService.class);

  private final UserRepository userRepository;
  private final OrganizationRepository organizationRepository;
  private final AuthenticationAttemptRepository authAttemptRepository;
  private final PasswordEncoder passwordEncoder;
  private final PasswordProperties passwordProperties;
  private final SecureRandom secureRandom;

  @Autowired
  public PasswordAuthService(
      UserRepository userRepository,
      OrganizationRepository organizationRepository,
      AuthenticationAttemptRepository authAttemptRepository,
      PasswordEncoder passwordEncoder,
      PasswordProperties passwordProperties) {
    this.userRepository = userRepository;
    this.organizationRepository = organizationRepository;
    this.authAttemptRepository = authAttemptRepository;
    this.passwordEncoder = passwordEncoder;
    this.passwordProperties = passwordProperties;
    this.secureRandom = new SecureRandom();
  }

  /** Register a new user with password authentication */
  public PasswordRegistrationResult registerUser(
      String email,
      String password,
      String displayName,
      UUID organizationId,
      String ipAddress,
      String userAgent) {
    try {
      // Validate organization exists
      Organization organization = organizationRepository.findById(organizationId).orElse(null);
      if (organization == null) {
        log.warn("Registration attempt for non-existent organization: {}", organizationId);
        return new PasswordRegistrationResult(
            false, null, PasswordRegistrationError.ORGANIZATION_NOT_FOUND, null);
      }

      // Check if user already exists in this organization
      if (userRepository.existsByEmailAndOrganizationIdAndDeletedAtIsNull(email, organizationId)) {
        log.warn(
            "Registration attempt for existing email {} in organization {}", email, organizationId);
        return new PasswordRegistrationResult(
            false, null, PasswordRegistrationError.EMAIL_ALREADY_EXISTS, null);
      }

      // Validate password policy
      PasswordValidationResult passwordValidation = validatePassword(password);
      if (!passwordValidation.isValid()) {
        log.warn("Registration attempt with invalid password for email: {}", email);
        return new PasswordRegistrationResult(
            false,
            null,
            PasswordRegistrationError.INVALID_PASSWORD,
            passwordValidation.getErrors());
      }

      // Create new user
      User user = new User(email, displayName);
      user.setOrganization(organization);
      user.setPasswordHash(passwordEncoder.encode(password));
      user.setEmailVerified(false); // Require email verification
      user.setAuthenticationMethods(Set.of(User.AuthenticationMethod.PASSWORD));
      user.setCreatedAt(Instant.now());
      user.setUpdatedAt(Instant.now());

      // Generate email verification token
      String verificationToken = generateSecureToken();
      user.setEmailVerificationToken(
          verificationToken,
          Instant.now()
              .plus(passwordProperties.getEmailVerificationTokenExpiryHours(), ChronoUnit.HOURS));

      user = userRepository.save(user);

      // Log successful registration
      authAttemptRepository.save(
          AuthenticationAttempt.success(
              user.getId(),
              email,
              AuthenticationAttempt.AuthenticationMethod.PASSWORD,
              ipAddress,
              userAgent,
              null));

      log.info("User registered successfully: {} in organization {}", email, organizationId);
      return new PasswordRegistrationResult(
          true, com.platform.user.internal.UserView.fromEntity(user), null, null);

    } catch (Exception e) {
      log.error("Error during user registration for email: {}", email, e);
      return new PasswordRegistrationResult(
          false, null, PasswordRegistrationError.INTERNAL_ERROR, null);
    }
  }

  /** Authenticate user with email and password */
  public PasswordAuthenticationResult authenticateUser(
      String email, String password, UUID organizationId, String ipAddress, String userAgent) {
    try {
      // Find user in the specified organization
      Optional<User> userOpt =
          userRepository.findByEmailAndOrganizationIdAndDeletedAtIsNull(email, organizationId);

      if (userOpt.isEmpty()) {
        // Log failed attempt without user ID
        authAttemptRepository.save(
            AuthenticationAttempt.failureUnknownUser(
                email,
                AuthenticationAttempt.AuthenticationMethod.PASSWORD,
                "USER_NOT_FOUND",
                ipAddress,
                userAgent));
        log.warn(
            "Authentication attempt for non-existent user: {} in organization {}",
            email,
            organizationId);
        return new PasswordAuthenticationResult(false, null, AuthenticationError.USER_NOT_FOUND);
      }

      User user = userOpt.get();

      // Check if user supports password authentication
      if (!user.supportsAuthenticationMethod(User.AuthenticationMethod.PASSWORD)) {
        authAttemptRepository.save(
            AuthenticationAttempt.failure(
                user.getId(),
                email,
                AuthenticationAttempt.AuthenticationMethod.PASSWORD,
                "INVALID_AUTHENTICATION_METHOD",
                ipAddress,
                userAgent));
        log.warn("Password authentication attempted for user without password support: {}", email);
        return new PasswordAuthenticationResult(
            false, null, AuthenticationError.INVALID_AUTHENTICATION_METHOD);
      }

      // Check if account is locked
      if (user.isAccountLocked()) {
        authAttemptRepository.save(
            AuthenticationAttempt.failure(
                user.getId(),
                email,
                AuthenticationAttempt.AuthenticationMethod.PASSWORD,
                "ACCOUNT_LOCKED",
                ipAddress,
                userAgent));
        log.warn("Authentication attempt for locked account: {}", email);
        return new PasswordAuthenticationResult(false, null, AuthenticationError.ACCOUNT_LOCKED);
      }

      // Check if email is verified
      if (!user.getEmailVerified()) {
        authAttemptRepository.save(
            AuthenticationAttempt.failure(
                user.getId(),
                email,
                AuthenticationAttempt.AuthenticationMethod.PASSWORD,
                "EMAIL_NOT_VERIFIED",
                ipAddress,
                userAgent));
        log.warn("Authentication attempt for unverified email: {}", email);
        return new PasswordAuthenticationResult(
            false, null, AuthenticationError.EMAIL_NOT_VERIFIED);
      }

      // Verify password
      if (!passwordEncoder.matches(password, user.getPasswordHash())) {
        handleFailedLogin(user, ipAddress, userAgent);
        log.warn("Invalid password attempt for user: {}", email);
        return new PasswordAuthenticationResult(
            false, null, AuthenticationError.INVALID_CREDENTIALS);
      }

      // Reset failed login attempts on successful authentication
      user.resetFailedLoginAttempts();
      userRepository.save(user);

      // Log successful authentication
      String sessionId = generateSecureToken(); // Generate session ID
      authAttemptRepository.save(
          AuthenticationAttempt.success(
              user.getId(),
              email,
              AuthenticationAttempt.AuthenticationMethod.PASSWORD,
              ipAddress,
              userAgent,
              sessionId));

      log.info("User authenticated successfully: {}", email);
      return new PasswordAuthenticationResult(true, com.platform.user.internal.UserView.fromEntity(user), null);

    } catch (Exception e) {
      log.error("Error during authentication for email: {}", email, e);
      return new PasswordAuthenticationResult(false, null, AuthenticationError.INTERNAL_ERROR);
    }
  }

  /** Request password reset */
  public PasswordResetRequestResult requestPasswordReset(
      String email, UUID organizationId, String ipAddress, String userAgent) {
    try {
      Optional<User> userOpt =
          userRepository.findByEmailAndOrganizationIdAndDeletedAtIsNull(email, organizationId);

      if (userOpt.isEmpty()) {
        // For security, don't reveal that user doesn't exist
        log.warn(
            "Password reset requested for non-existent user: {} in organization {}",
            email,
            organizationId);
        return new PasswordResetRequestResult(
            true, "If the email exists, a reset link has been sent.");
      }

      User user = userOpt.get();

      // Check if user supports password authentication
      if (!user.supportsAuthenticationMethod(User.AuthenticationMethod.PASSWORD)) {
        log.warn("Password reset requested for user without password support: {}", email);
        return new PasswordResetRequestResult(
            true, "If the email exists, a reset link has been sent.");
      }

      // Generate reset token
      String resetToken = generateSecureToken();
      user.setPasswordResetToken(
          resetToken,
          Instant.now()
              .plus(passwordProperties.getPasswordResetTokenExpiryHours(), ChronoUnit.HOURS));

      userRepository.save(user);

      // TODO: Send email with reset link
      // emailService.sendPasswordResetEmail(user.getEmail(), resetToken);

      log.info("Password reset requested for user: {}", email);
      return new PasswordResetRequestResult(
          true, "If the email exists, a reset link has been sent.");

    } catch (Exception e) {
      log.error("Error during password reset request for email: {}", email, e);
      return new PasswordResetRequestResult(false, "An error occurred. Please try again later.");
    }
  }

  /** Reset password using token */
  public PasswordResetResult resetPassword(
      String token, String newPassword, String ipAddress, String userAgent) {
    try {
      Optional<User> userOpt = userRepository.findByPasswordResetToken(token);

      if (userOpt.isEmpty()) {
        log.warn("Invalid password reset token attempted from IP: {}", ipAddress);
        return new PasswordResetResult(false, PasswordResetError.INVALID_TOKEN);
      }

      User user = userOpt.get();

      // Validate token
      if (!user.isPasswordResetTokenValid(token)) {
        log.warn("Expired password reset token attempted for user: {}", user.getEmail());
        return new PasswordResetResult(false, PasswordResetError.INVALID_TOKEN);
      }

      // Validate password policy
      PasswordValidationResult passwordValidation = validatePassword(newPassword);
      if (!passwordValidation.isValid()) {
        log.warn("Password reset attempted with invalid password for user: {}", user.getEmail());
        return new PasswordResetResult(false, PasswordResetError.INVALID_PASSWORD);
      }

      // Update password and clear reset token
      user.setPasswordHash(passwordEncoder.encode(newPassword));
      user.clearPasswordResetToken();
      user.resetFailedLoginAttempts(); // Clear any lockouts

      userRepository.save(user);

      // Log password reset
      authAttemptRepository.save(
          AuthenticationAttempt.success(
              user.getId(),
              user.getEmail().getValue(),
              AuthenticationAttempt.AuthenticationMethod.PASSWORD,
              ipAddress,
              userAgent,
              null));

      log.info("Password reset successfully for user: {}", user.getEmail());
      return new PasswordResetResult(true, null);

    } catch (Exception e) {
      log.error("Error during password reset", e);
      return new PasswordResetResult(false, PasswordResetError.INTERNAL_ERROR);
    }
  }

  /** Change password for authenticated user */
  public PasswordChangeResult changePassword(
      UUID userId, String currentPassword, String newPassword, String ipAddress, String userAgent) {
    try {
      Optional<User> userOpt = userRepository.findById(userId);

      if (userOpt.isEmpty()) {
        log.warn("Password change attempted for non-existent user: {}", userId);
        return new PasswordChangeResult(false, PasswordChangeError.USER_NOT_FOUND);
      }

      User user = userOpt.get();

      // Verify current password
      if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
        authAttemptRepository.save(
            AuthenticationAttempt.failure(
                user.getId(),
                user.getEmail().getValue(),
                AuthenticationAttempt.AuthenticationMethod.PASSWORD,
                "INVALID_CURRENT_PASSWORD",
                ipAddress,
                userAgent));
        log.warn("Invalid current password for password change: {}", user.getEmail());
        return new PasswordChangeResult(false, PasswordChangeError.INVALID_CURRENT_PASSWORD);
      }

      // Validate new password policy
      PasswordValidationResult passwordValidation = validatePassword(newPassword);
      if (!passwordValidation.isValid()) {
        log.warn(
            "Password change attempted with invalid new password for user: {}", user.getEmail());
        return new PasswordChangeResult(false, PasswordChangeError.INVALID_NEW_PASSWORD);
      }

      // Update password
      user.setPasswordHash(passwordEncoder.encode(newPassword));
      userRepository.save(user);

      log.info("Password changed successfully for user: {}", user.getEmail());
      return new PasswordChangeResult(true, null);

    } catch (Exception e) {
      log.error("Error during password change for user: {}", userId, e);
      return new PasswordChangeResult(false, PasswordChangeError.INTERNAL_ERROR);
    }
  }

  /** Verify email using verification token */
  public EmailVerificationResult verifyEmail(String token, String ipAddress) {
    try {
      Optional<User> userOpt = userRepository.findByEmailVerificationToken(token);

      if (userOpt.isEmpty()) {
        log.warn("Invalid email verification token attempted from IP: {}", ipAddress);
        return new EmailVerificationResult(false, EmailVerificationError.INVALID_TOKEN);
      }

      User user = userOpt.get();

      // Validate token
      if (!user.isEmailVerificationTokenValid(token)) {
        log.warn("Expired email verification token attempted for user: {}", user.getEmail());
        return new EmailVerificationResult(false, EmailVerificationError.INVALID_TOKEN);
      }

      // Verify email
      user.verifyEmail();
      userRepository.save(user);

      log.info("Email verified successfully for user: {}", user.getEmail());
      return new EmailVerificationResult(true, null);

    } catch (Exception e) {
      log.error("Error during email verification", e);
      return new EmailVerificationResult(false, EmailVerificationError.INTERNAL_ERROR);
    }
  }

  // Private helper methods

  private void handleFailedLogin(User user, String ipAddress, String userAgent) {
    user.incrementFailedLoginAttempts();

    // Check if account should be locked
    if (user.getFailedLoginAttempts() >= passwordProperties.getMaxFailedAttempts()) {
      long lockoutMinutes = calculateLockoutDuration(user.getFailedLoginAttempts());
      user.lockAccount(Instant.now().plus(lockoutMinutes, ChronoUnit.MINUTES));
      log.warn(
          "Account locked for user: {} after {} failed attempts",
          user.getEmail(),
          user.getFailedLoginAttempts());
    }

    userRepository.save(user);

    // Log failed attempt
    authAttemptRepository.save(
        AuthenticationAttempt.failure(
            user.getId(),
            user.getEmail().getValue(),
            AuthenticationAttempt.AuthenticationMethod.PASSWORD,
            "INVALID_CREDENTIALS",
            ipAddress,
            userAgent));
  }

  private long calculateLockoutDuration(int failedAttempts) {
    // Exponential backoff: 5, 15, 30, 60 minutes
    return switch (failedAttempts) {
      case 3, 4 -> 5;
      case 5, 6 -> 15;
      case 7, 8 -> 30;
      default -> 60;
    };
  }

  private PasswordValidationResult validatePassword(String password) {
    // TODO: Implement comprehensive password validation based on passwordProperties
    if (password == null || password.length() < passwordProperties.getMinLength()) {
      return new PasswordValidationResult(
          false,
          "Password must be at least " + passwordProperties.getMinLength() + " characters long");
    }
    return new PasswordValidationResult(true, null);
  }

  private String generateSecureToken() {
    byte[] bytes = new byte[32];
    secureRandom.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  // Result classes

  public record PasswordRegistrationResult(
      boolean success,
      com.platform.user.internal.UserView user,
      PasswordRegistrationError errorType,
      String errorMessage) {}

  public record PasswordAuthenticationResult(
      boolean success, com.platform.user.internal.UserView user, AuthenticationError errorType) {}

  public record PasswordResetRequestResult(boolean success, String message) {}

  public record PasswordResetResult(boolean success, PasswordResetError errorType) {}

  public record PasswordChangeResult(boolean success, PasswordChangeError errorType) {}

  public record EmailVerificationResult(boolean success, EmailVerificationError errorType) {}

  private record PasswordValidationResult(boolean isValid, String errorMessage) {
    public String getErrors() {
      return errorMessage;
    }
  }

  // Error enums

  public enum PasswordRegistrationError {
    EMAIL_ALREADY_EXISTS,
    INVALID_PASSWORD,
    ORGANIZATION_NOT_FOUND,
    INTERNAL_ERROR
  }

  public enum AuthenticationError {
    USER_NOT_FOUND,
    INVALID_CREDENTIALS,
    ACCOUNT_LOCKED,
    EMAIL_NOT_VERIFIED,
    INVALID_AUTHENTICATION_METHOD,
    INTERNAL_ERROR
  }

  public enum PasswordResetError {
    INVALID_TOKEN,
    INVALID_PASSWORD,
    INTERNAL_ERROR
  }

  public enum PasswordChangeError {
    USER_NOT_FOUND,
    INVALID_CURRENT_PASSWORD,
    INVALID_NEW_PASSWORD,
    INTERNAL_ERROR
  }

  public enum EmailVerificationError {
    INVALID_TOKEN,
    INTERNAL_ERROR
  }
}
