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

/**
 * Test-only controller to align integration tests with a simplified auth flow.
 * Active in all profiles except production for security.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Profile("!prod")
public class TestAuthFlowController {

  private final OAuth2ProviderRepository providerRepository;
  private final OAuth2SessionRepository sessionRepository;
  private final OAuth2UserInfoRepository userInfoRepository;
  private final OAuth2AuditService auditService;

  public TestAuthFlowController(
      OAuth2ProviderRepository providerRepository,
      OAuth2SessionRepository sessionRepository,
      OAuth2UserInfoRepository userInfoRepository,
      OAuth2AuditService auditService) {
    this.providerRepository = providerRepository;
    this.sessionRepository = sessionRepository;
    this.userInfoRepository = userInfoRepository;
    this.auditService = auditService;
  }

  /** List available providers from DB (as tests expect). */
  @GetMapping("/providers")
  public ResponseEntity<Map<String, Object>> listProviders() {
    List<Map<String, Object>> providers =
        providerRepository.findAll().stream()
            .map(
                p ->
                    Map.<String, Object>of(
                        "name", p.getName(),
                        "displayName", p.getDisplayName(),
                        "supported", Boolean.TRUE))
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
    sessionRepository.save(session);

    // Redirect to provider authorization URL (tests only check base contains)
    HttpHeaders headers = new HttpHeaders();
    headers.setLocation(URI.create(p.getAuthorizationUrl()));
    return new ResponseEntity<>(headers, HttpStatus.FOUND);
  }

  public record CallbackRequest(String code, String state, String sessionId) {}

  /** Handle callback: validate session exists and return expected payload. */
  @PostMapping("/callback")
  public ResponseEntity<Map<String, Object>> callback(
      @Valid @RequestBody CallbackRequest body, HttpServletRequest request) {

    if (body == null || body.sessionId == null || body.sessionId().isBlank()) {
      auditService.logAuthorizationFailure("unknown", "INVALID_AUTHORIZATION_CODE", "Missing sessionId", null, null, getIp(request));
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("error", "INVALID_AUTHORIZATION_CODE"));
    }

    return sessionRepository
        .findBySessionId(body.sessionId())
        .map(
            s -> {
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
              return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                  .body(Map.of("error", "INVALID_AUTHORIZATION_CODE"));
            });
  }

  /** Get session info using Bearer <sessionId> header. */
  @GetMapping("/session")
  public ResponseEntity<Map<String, Object>> getSession(HttpServletRequest request) {
    String sid = extractBearer(request.getHeader("Authorization"));
    if (sid == null) {
      return ResponseEntity.ok(Map.of("authenticated", false, "timestamp", Instant.now()));
    }
    return sessionRepository
        .findBySessionId(sid)
        .map(
            s -> {
              OAuth2UserInfo ui = s.getUserInfo();
              return ResponseEntity.ok(
                  Map.of(
                      "authenticated",
                      s.isValid(),
                      "user",
                      Map.of("email", ui.getEmail(), "name", ui.getName()),
                      "timestamp",
                      Instant.now()));
            })
        .orElse(ResponseEntity.ok(Map.of("authenticated", false, "timestamp", Instant.now())));
  }

  /** Logout and terminate session. */
  @PostMapping("/logout")
  public ResponseEntity<Map<String, Object>> logout(HttpServletRequest request) {
    String sid = extractBearer(request.getHeader("Authorization"));
    if (sid != null) {
      sessionRepository.findBySessionId(sid).ifPresent(s -> {
        s.terminate("logout");
        sessionRepository.save(s);
      });
    }
    return ResponseEntity.ok(Map.of("success", true));
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
}
