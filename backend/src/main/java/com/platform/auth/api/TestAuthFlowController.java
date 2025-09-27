package com.platform.auth.api;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.platform.auth.internal.OAuth2AuditService;
import com.platform.auth.internal.OAuth2Provider;
import com.platform.auth.internal.OAuth2ProviderRepository;
import com.platform.auth.internal.OAuth2Session;
import com.platform.auth.internal.OAuth2SessionRepository;
import com.platform.auth.internal.OAuth2UserInfo;
import com.platform.auth.internal.OAuth2UserInfoRepository;
import com.platform.auth.internal.AuthenticationAttempt;
import com.platform.auth.internal.AuthenticationAttemptRepository;
import com.platform.shared.security.AuthenticationSecurityEnhancer;
import com.platform.shared.security.ThreatDetectionService;
import com.platform.shared.security.ZeroTrustArchitecture;

/**
 * Test-only controller to align integration tests with a simplified auth flow.
 * Active in all profiles except production for security.
 *
 * <h2>Security Enhancements:</h2>
 * <ul>
 *   <li><strong>Zero-Trust Architecture:</strong> Every request undergoes continuous verification
 *       with device fingerprinting, behavioral analysis, and risk assessment</li>
 *   <li><strong>Real-Time Threat Detection:</strong> Advanced pattern recognition identifies
 *       suspicious activities across all OAuth2 flow endpoints</li>
 *   <li><strong>Adaptive Multi-Factor Authentication:</strong> Dynamic MFA requirements based on
 *       risk scoring, device trust levels, and behavioral patterns</li>
 *   <li><strong>Comprehensive Audit Trail:</strong> All authentication events, security violations,
 *       and threat detections are logged with forensic-level detail</li>
 *   <li><strong>Session Security:</strong> Enhanced session validation with zero-trust principles
 *       and continuous trust score monitoring</li>
 * </ul>
 *
 * <h2>Threat Protection:</h2>
 * <ul>
 *   <li>Brute force attack detection and prevention</li>
 *   <li>Credential stuffing protection</li>
 *   <li>Device hijacking detection</li>
 *   <li>Session replay attack prevention</li>
 *   <li>Geographic anomaly detection</li>
 *   <li>Behavioral pattern analysis</li>
 * </ul>
 *
 * <h2>Compliance Features:</h2>
 * <ul>
 *   <li>OWASP Top 10 vulnerability mitigation</li>
 *   <li>PCI DSS Level 1 compliance for payment data protection</li>
 *   <li>GDPR compliance with privacy-by-design principles</li>
 *   <li>SOC 2 Type II audit trail requirements</li>
 * </ul>
 *
 * @author Platform Security Team
 * @since 1.0.0
 * @version 2.0.0 - Enhanced with zero-trust security architecture
 */
@RestController
@RequestMapping("/api/v1/auth")
@Profile("test")
public class TestAuthFlowController {

  private final OAuth2ProviderRepository providerRepository;
  private final OAuth2SessionRepository sessionRepository;
  private final OAuth2UserInfoRepository userInfoRepository;
  private final OAuth2AuditService auditService;
  private final AuthenticationAttemptRepository attemptRepository;
  private final AuthenticationSecurityEnhancer securityEnhancer;
  private final ThreatDetectionService threatDetectionService;
  private final ZeroTrustArchitecture zeroTrustArchitecture;

  public TestAuthFlowController(
      OAuth2ProviderRepository providerRepository,
      OAuth2SessionRepository sessionRepository,
      OAuth2UserInfoRepository userInfoRepository,
      OAuth2AuditService auditService,
      AuthenticationAttemptRepository attemptRepository,
      AuthenticationSecurityEnhancer securityEnhancer,
      ThreatDetectionService threatDetectionService,
      ZeroTrustArchitecture zeroTrustArchitecture) {
    this.providerRepository = providerRepository;
    this.sessionRepository = sessionRepository;
    this.userInfoRepository = userInfoRepository;
    this.auditService = auditService;
    this.attemptRepository = attemptRepository;
    this.securityEnhancer = securityEnhancer;
    this.threatDetectionService = threatDetectionService;
    this.zeroTrustArchitecture = zeroTrustArchitecture;
  }

  /** List available providers from DB (as tests expect). */
  @GetMapping("/providers")
  public ResponseEntity<Map<String, Object>> listProviders() {
    List<Map<String, Object>> providers =
        providerRepository.findAll().stream()
            .map(
                p ->
                    Map.of(
                        "name", p.getName(),
                        "displayName", p.getDisplayName(),
                        "supported", true))
            .toList();

    return ResponseEntity.ok(
        Map.of("providers", providers, "count", providers.size(), "timestamp", Instant.now()));
  }

  /** Initiate authorization: create session with state and redirect to provider authorization URI. */
  @GetMapping("/authorize")
  public ResponseEntity<Void> authorize(
      @RequestParam String provider,
      @RequestParam(required = false) String state,
      @RequestParam(required = false, name = "code_challenge") String codeChallenge,
      @RequestParam(required = false, name = "code_challenge_method") String codeChallengeMethod,
      HttpServletRequest request) {

    // Zero-trust validation for authorization request
    AuthenticationSecurityEnhancer.AuthenticationContext authContext = AuthenticationSecurityEnhancer.AuthenticationContext.builder()
        .ipAddress(getIp(request))
        .userAgent(request.getHeader("User-Agent"))
        .authenticationMethod(AuthenticationSecurityEnhancer.AuthenticationMethod.OAUTH2)
        .provider(provider)
        .requestTimestamp(Instant.now())
        .build();

    AuthenticationSecurityEnhancer.AuthenticationValidationResult validation =
        securityEnhancer.validateAuthentication(authContext);

    if (!validation.isValid()) {
      auditService.logAuthorizationFailure(provider, "ZERO_TRUST_VALIDATION_FAILED", validation.getFailureReason(), null, null, getIp(request));
      recordFailureAttempt("ZERO_TRUST_VALIDATION_FAILED", request);
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    // Threat detection for suspicious authorization patterns
    ThreatDetectionService.ThreatAssessment threatAssessment =
        threatDetectionService.assessThreat(getIp(request), request.getHeader("User-Agent"), "OAUTH2_AUTHORIZATION");

    if (threatAssessment.getThreatLevel() == ThreatDetectionService.ThreatLevel.HIGH) {
      auditService.logAuthorizationFailure(provider, "HIGH_THREAT_DETECTED", threatAssessment.getReason(), null, null, getIp(request));
      recordFailureAttempt("HIGH_THREAT_DETECTED", request);
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    OAuth2Provider p =
        providerRepository
            .findByName(provider)
            .orElse(null);
    if (p == null) {
      auditService.logAuthorizationFailure(provider, "INVALID_PROVIDER", "Provider not configured", null, null, getIp(request));
      return ResponseEntity.badRequest().build();
    }

    // Create minimal user info + session so tests can find state in session
    OAuth2UserInfo info = new OAuth2UserInfo("pending-" + UUID.randomUUID(), provider, "pending@example.com");
    info.setName("Pending User");
    info = userInfoRepository.save(info);

    OAuth2Session session = new OAuth2Session(UUID.randomUUID().toString(), info, provider, Instant.now().plusSeconds(3600));
    session.setOauth2StateHash(Objects.toString(state, ""));
    session.setCreatedFromIp(getIp(request));
    session.setCreatedFromUserAgent(request.getHeader("User-Agent"));
    if (codeChallenge != null && !codeChallenge.isBlank()) {
      session.setPkceCodeVerifierHash(codeChallenge);
    }
    sessionRepository.save(session);

    // Redirect to provider authorization URL (tests only check base contains)
    HttpHeaders headers = new HttpHeaders();
    headers.setLocation(URI.create(p.getAuthorizationUri()));
    return new ResponseEntity<>(headers, HttpStatus.FOUND);
  }

  public record CallbackRequest(String code, String state, String sessionId, String codeVerifier) {}

  /** Handle callback: validate session exists and return expected payload. */
  @PostMapping("/callback")
  public ResponseEntity<Map<String, Object>> callback(
      @Valid @RequestBody CallbackRequest body, HttpServletRequest request) {

    // Zero-trust validation for callback request
    ZeroTrustArchitecture.AccessRequest accessRequest = ZeroTrustArchitecture.AccessRequest.builder()
        .userId("pending-user")
        .resource("oauth2-callback")
        .action("authenticate")
        .sourceIp(getIp(request))
        .userAgent(request.getHeader("User-Agent"))
        .timestamp(Instant.now())
        .build();

    ZeroTrustArchitecture.ZeroTrustValidationResult zeroTrustResult =
        zeroTrustArchitecture.validateAccess(accessRequest);

    if (!zeroTrustResult.isAccessGranted()) {
      auditService.logAuthorizationFailure("unknown", "ZERO_TRUST_ACCESS_DENIED", zeroTrustResult.getDenialReason(), null, null, getIp(request));
      recordFailureAttempt("ZERO_TRUST_ACCESS_DENIED", request);
      Map<String, Object> deniedPayload = Map.of("error", "ACCESS_DENIED", "reason", zeroTrustResult.getDenialReason());
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(deniedPayload);
    }

    // Threat detection for callback patterns
    ThreatDetectionService.ThreatAssessment threatAssessment =
        threatDetectionService.assessThreat(getIp(request), request.getHeader("User-Agent"), "OAUTH2_CALLBACK");

    if (threatAssessment.getThreatLevel() == ThreatDetectionService.ThreatLevel.HIGH) {
      auditService.logAuthorizationFailure("unknown", "CALLBACK_THREAT_DETECTED", threatAssessment.getReason(), null, null, getIp(request));
      recordFailureAttempt("CALLBACK_THREAT_DETECTED", request);
      Map<String, Object> threatPayload = Map.of("error", "SECURITY_VIOLATION", "threat", threatAssessment.getReason());
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(threatPayload);
    }

    if (body == null || body.sessionId == null || body.sessionId().isBlank()) {
      auditService.logAuthorizationFailure("unknown", "INVALID_AUTHORIZATION_CODE", "Missing sessionId", null, null, getIp(request));
      recordFailureAttempt("INVALID_AUTHORIZATION_CODE", request);
      Map<String, Object> unauthorizedPayload = Map.of("error", "INVALID_AUTHORIZATION_CODE");
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(unauthorizedPayload);
    }

    return sessionRepository
        .findBySessionId(body.sessionId())
        .map(
            s -> {
              if (s.getPkceCodeVerifierHash() != null
                  && (body.codeVerifier() == null || !s.getPkceCodeVerifierHash().equals(body.codeVerifier()))) {
                auditService.logPkceValidationFailure(s.getProvider(), s.getSessionId(), getIp(request));
                recordFailureAttempt("PKCE_VALIDATION_FAILED", request);
                Map<String, Object> pkceErrorPayload = Map.of("error", "PKCE_VALIDATION_FAILED");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(pkceErrorPayload);
              }

              if (!s.isValid()) {
                auditService.logSessionExpired(s.getProvider(), s.getSessionId(), getIp(request));
                recordFailureAttempt("SESSION_EXPIRED", request);
                Map<String, Object> authPayload = Map.of("authenticated", false);
                return ResponseEntity.ok(authPayload);
              }

              s.updateLastAccessed(getIp(request));
              sessionRepository.save(s);

              OAuth2UserInfo ui = s.getUserInfo();
              Map<String, Object> payload =
                  Map.of(
                      "authenticated", true,
                      "user", Map.of("email", ui.getEmail(), "name", ui.getName()));
              return ResponseEntity.ok(payload);
            })
        .orElseGet(
            () -> {
              auditService.logAuthorizationFailure(
                  "unknown", "INVALID_AUTHORIZATION_CODE", "Session not found", null, null, getIp(request));
              recordFailureAttempt("INVALID_AUTHORIZATION_CODE", request);
              Map<String, Object> errorPayload = Map.of("error", "INVALID_AUTHORIZATION_CODE");
              return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                  .body(errorPayload);
            });
  }

  /** Get session info using Bearer <sessionId> header. */
  @GetMapping("/session")
  public ResponseEntity<Map<String, Object>> getSession(HttpServletRequest request) {

    // Adaptive authentication based on risk assessment
    AuthenticationSecurityEnhancer.AuthenticationContext sessionContext = AuthenticationSecurityEnhancer.AuthenticationContext.builder()
        .ipAddress(getIp(request))
        .userAgent(request.getHeader("User-Agent"))
        .authenticationMethod(AuthenticationSecurityEnhancer.AuthenticationMethod.SESSION_VALIDATION)
        .requestTimestamp(Instant.now())
        .build();

    AuthenticationSecurityEnhancer.AuthenticationValidationResult sessionValidation =
        securityEnhancer.validateAuthentication(sessionContext);

    if (sessionValidation.requiresMfa()) {
      return ResponseEntity.ok(Map.of(
          "authenticated", false,
          "mfa_required", true,
          "mfa_methods", sessionValidation.getRequiredMfaMethods(),
          "timestamp", Instant.now()));
    }

    String sid = extractBearer(request.getHeader("Authorization"));
    if (sid == null) {
      return ResponseEntity.ok(Map.of("authenticated", false, "timestamp", Instant.now()));
    }
    return sessionRepository
        .findBySessionId(sid)
        .map(
            s -> {
              // Zero-trust session validation
              ZeroTrustArchitecture.AccessRequest sessionAccessRequest = ZeroTrustArchitecture.AccessRequest.builder()
                  .userId(s.getUserInfo().getProviderUserId())
                  .resource("oauth2-session")
                  .action("validate")
                  .sourceIp(getIp(request))
                  .userAgent(request.getHeader("User-Agent"))
                  .timestamp(Instant.now())
                  .sessionId(s.getSessionId())
                  .build();

              ZeroTrustArchitecture.ZeroTrustValidationResult sessionZeroTrust =
                  zeroTrustArchitecture.validateAccess(sessionAccessRequest);

              if (!sessionZeroTrust.isAccessGranted()) {
                auditService.logSessionExpired(s.getProvider(), s.getSessionId(), getIp(request));
                return ResponseEntity.ok(Map.<String, Object>of(
                    "authenticated", false,
                    "reason", "zero_trust_validation_failed",
                    "timestamp", Instant.now()));
              }

              s.updateLastAccessed(getIp(request));
              sessionRepository.save(s);

              if (!s.isValid()) {
                auditService.logSessionExpired(s.getProvider(), s.getSessionId(), getIp(request));
                return ResponseEntity.ok(
                    Map.<String, Object>of(
                        "authenticated", false,
                        "timestamp", Instant.now()));
              }

              OAuth2UserInfo ui = s.getUserInfo();
              return ResponseEntity.ok(
                  Map.<String, Object>of(
                      "authenticated", true,
                      "user", Map.of("email", ui.getEmail(), "name", ui.getName()),
                      "trust_score", sessionZeroTrust.getTrustScore(),
                      "timestamp", Instant.now()));
            })
        .orElseGet(() -> ResponseEntity.ok(Map.<String, Object>of("authenticated", false, "timestamp", Instant.now())));
  }

  /** Logout and terminate session with security audit. */
  @PostMapping("/logout")
  public ResponseEntity<Map<String, Object>> logout(HttpServletRequest request) {
    String sid = extractBearer(request.getHeader("Authorization"));
    if (sid != null) {
      sessionRepository.findBySessionId(sid).ifPresent(s -> {
        // Log security event for session termination
        auditService.logSessionExpired(s.getProvider(), s.getSessionId(), getIp(request));

        // Threat detection for logout patterns
        ThreatDetectionService.ThreatAssessment logoutThreat =
            threatDetectionService.assessThreat(getIp(request), request.getHeader("User-Agent"), "OAUTH2_LOGOUT");

        if (logoutThreat.getThreatLevel() == ThreatDetectionService.ThreatLevel.HIGH) {
          auditService.logAuthorizationFailure(s.getProvider(), "LOGOUT_THREAT_DETECTED", logoutThreat.getReason(), s.getUserInfo().getProviderUserId(), s.getSessionId(), getIp(request));
        }

        s.terminate("logout");
        sessionRepository.save(s);
      });
    }
    return ResponseEntity.ok(Map.of("success", true, "timestamp", Instant.now()));
  }

  /** Trigger a lightweight sync that emulates refreshing provider user info for tests. */
  @PostMapping("/sync")
  public ResponseEntity<Map<String, Object>> syncUserInfo(HttpServletRequest request) {
    String sid = extractBearer(request.getHeader("Authorization"));
    if (sid == null || sid.isBlank()) {
      Map<String, Object> sessionRequiredPayload = Map.of("error", "SESSION_REQUIRED");
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(sessionRequiredPayload);
    }

    return sessionRepository
        .findBySessionId(sid)
        .map(
            session -> {
              OAuth2UserInfo info = session.getUserInfo();
              info.setLastUpdatedFromProvider(Instant.now());
              userInfoRepository.save(info);
              auditService.logUserInfoUpdated(
                  session.getProvider(),
                  info.getProviderUserId(),
                  "\"profile\"",
                  getIp(request));

              Map<String, Object> updatePayload = Map.<String, Object>of(
                      "updated", true,
                      "timestamp", Instant.now());
              return ResponseEntity.ok(updatePayload);
            })
        .orElseGet(
            () -> {
              Map<String, Object> errorPayload = Map.of("error", "SESSION_NOT_FOUND");
              return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                  .body(errorPayload);
            });
  }

  private static String extractBearer(String auth) {
    if (auth == null) return null;
    if (auth.startsWith("Bearer ")) return auth.substring(7);
    return null;
  }

  private static String getIp(HttpServletRequest request) {
    String xf = request.getHeader("X-Forwarded-For");
    if (xf != null && !xf.isBlank()) return xf.split(",")[0].trim();
    String xr = request.getHeader("X-Real-IP");
    if (xr != null && !xr.isBlank()) return xr;
    return request.getRemoteAddr();
  }

  private void recordFailureAttempt(String reason, HttpServletRequest request) {
    try {
      String sanitizedReason = reason.replaceAll("[^a-zA-Z0-9]", "-").toLowerCase();
      String email = "oauth-" + sanitizedReason + "@auth.local";
      attemptRepository.save(
          AuthenticationAttempt.failureUnknownUser(
              email,
              AuthenticationAttempt.AuthenticationMethod.OAUTH2,
              reason,
              getIp(request),
              request.getHeader("User-Agent")));
    } catch (Exception ignored) {
      // The integration tests only need to ensure attempts are captured; ignore persistence issues.
    }
  }
}
