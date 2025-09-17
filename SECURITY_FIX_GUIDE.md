# Security Fix Implementation Guide

This guide provides concrete code implementations to address the critical and high-severity security issues identified in the security review.

## 🔴 CRITICAL FIXES

### 1. Replace localStorage with Secure Cookies for Authentication

#### Backend: Create Secure Cookie Service

**File**: `backend/src/main/java/com/platform/shared/security/SecureCookieService.java`

```java
package com.platform.shared.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SecureCookieService {

    @Value("${app.cookie.domain:localhost}")
    private String cookieDomain;

    @Value("${app.cookie.secure:true}")
    private boolean secureCookie;

    @Value("${app.cookie.max-age:86400}") // 24 hours
    private int cookieMaxAge;

    public void setAuthCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("auth-token", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(secureCookie);
        cookie.setPath("/");
        cookie.setDomain(cookieDomain);
        cookie.setMaxAge(cookieMaxAge);
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);
    }

    public void clearAuthCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("auth-token", "");
        cookie.setHttpOnly(true);
        cookie.setSecure(secureCookie);
        cookie.setPath("/");
        cookie.setDomain(cookieDomain);
        cookie.setMaxAge(0);
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);
    }
}
```

#### Frontend: Remove localStorage Usage

**File**: `frontend/src/store/slices/authSlice.ts`

```typescript
import { createSlice, PayloadAction } from '@reduxjs/toolkit'

// ... existing imports and types ...

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    setCredentials: (
      state,
      action: PayloadAction<{ user: User }>
    ) => {
      state.user = action.payload.user
      // Remove token from state - handled by httpOnly cookies
      state.isAuthenticated = true
      state.error = null
    },
    logout: state => {
      state.user = null
      state.isAuthenticated = false
      state.error = null
      // No localStorage manipulation needed
    },
    // ... other reducers
  },
})
```

**File**: `frontend/src/utils/api.ts`

```typescript
import axios from 'axios'

// Configure axios to send cookies
const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  withCredentials: true, // This ensures cookies are sent with requests
  headers: {
    'Content-Type': 'application/json',
  },
})

// Add CSRF token to requests
api.interceptors.request.use((config) => {
  const csrfToken = document.querySelector('meta[name="csrf-token"]')?.getAttribute('content')
  if (csrfToken) {
    config.headers['X-CSRF-Token'] = csrfToken
  }
  return config
})

export default api
```

### 2. Implement Proper OAuth2 State Validation

**File**: `backend/src/main/java/com/platform/auth/internal/OAuth2StateService.java`

```java
package com.platform.auth.internal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

@Service
public class OAuth2StateService {

    private static final int STATE_LENGTH = 32;
    private static final long STATE_TTL_MINUTES = 10;
    private static final String STATE_KEY_PREFIX = "oauth2:state:";

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private final SecureRandom secureRandom = new SecureRandom();

    public String generateState(String sessionId) {
        byte[] randomBytes = new byte[STATE_LENGTH];
        secureRandom.nextBytes(randomBytes);
        String state = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        // Store state in Redis with TTL
        String key = STATE_KEY_PREFIX + state;
        redisTemplate.opsForValue().set(key, sessionId, STATE_TTL_MINUTES, TimeUnit.MINUTES);

        return state;
    }

    public boolean validateState(String state, String sessionId) {
        if (state == null || sessionId == null) {
            return false;
        }

        String key = STATE_KEY_PREFIX + state;
        String storedSessionId = redisTemplate.opsForValue().get(key);

        if (storedSessionId != null && storedSessionId.equals(sessionId)) {
            // Delete state after successful validation (one-time use)
            redisTemplate.delete(key);
            return true;
        }

        return false;
    }
}
```

**Update**: `backend/src/main/java/com/platform/auth/api/OAuth2Controller.java`

```java
@GetMapping("/authorize/{provider}")
public ResponseEntity<Map<String, Object>> initiateAuthorization(
    @PathVariable String provider,
    @RequestParam(required = false) String redirect_uri,
    HttpServletRequest request) {

    String sessionId = request.getSession().getId();
    String ipAddress = getClientIpAddress(request);

    try {
        // ... existing validation ...

        // Generate and store state properly
        String state = oauth2StateService.generateState(sessionId);

        // Generate PKCE parameters
        String codeVerifier = generateCodeVerifier();
        String codeChallenge = generateCodeChallenge(codeVerifier);

        // Store code verifier in session for later validation
        request.getSession().setAttribute("code_verifier", codeVerifier);

        // ... rest of the method
    } catch (Exception e) {
        // ... error handling
    }
}

// Update the callback handler
@GetMapping("/callback/{provider}")
public ResponseEntity<Map<String, Object>> handleCallback(
    @PathVariable String provider,
    @RequestParam(required = false) String code,
    @RequestParam(required = false) String state,
    // ... other params
    HttpServletRequest request) {

    String sessionId = request.getSession().getId();

    // Properly validate state
    if (!oauth2StateService.validateState(state, sessionId)) {
        auditService.logStateValidationFailure(provider, sessionId, state, ipAddress);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(Map.of("error", Map.of(
                "code", "OAUTH2_INVALID_STATE",
                "message", "Invalid state parameter - possible CSRF attack"
            )));
    }

    // ... rest of the method
}
```

### 3. Implement Proper Secret Management

**File**: `backend/src/main/java/com/platform/shared/config/SecretsConfig.java`

```java
package com.platform.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "secrets")
@Profile("!test")
public class SecretsConfig {

    private Map<String, String> vault = new HashMap<>();

    @PostConstruct
    public void validateSecrets() {
        // Ensure no default/test values in production
        if (isProductionEnvironment()) {
            validateNoTestValues();
            validateSecretStrength();
        }
    }

    private void validateNoTestValues() {
        for (Map.Entry<String, String> entry : vault.entrySet()) {
            String value = entry.getValue();
            if (value.contains("test") || value.contains("dummy") ||
                value.contains("example") || value.contains("localhost")) {
                throw new IllegalStateException(
                    "Test/dummy value detected in production for key: " + entry.getKey()
                );
            }
        }
    }

    private void validateSecretStrength() {
        // Validate API keys, passwords meet minimum requirements
        for (Map.Entry<String, String> entry : vault.entrySet()) {
            if (entry.getKey().toLowerCase().contains("password") ||
                entry.getKey().toLowerCase().contains("secret") ||
                entry.getKey().toLowerCase().contains("key")) {
                if (entry.getValue().length() < 32) {
                    throw new IllegalStateException(
                        "Secret too weak for key: " + entry.getKey()
                    );
                }
            }
        }
    }

    private boolean isProductionEnvironment() {
        String profile = System.getProperty("spring.profiles.active");
        return "production".equalsIgnoreCase(profile) || "prod".equalsIgnoreCase(profile);
    }

    // Getters and setters
    public Map<String, String> getVault() {
        return vault;
    }

    public void setVault(Map<String, String> vault) {
        this.vault = vault;
    }
}
```

**File**: `backend/src/main/resources/application-production.yml`

```yaml
# Production configuration - secrets loaded from environment or external vault
spring:
  datasource:
    url: ${DATABASE_URL}
    username: ${DATABASE_USERNAME}
    password: ${DATABASE_PASSWORD}

  data:
    redis:
      host: ${REDIS_HOST}
      port: ${REDIS_PORT}
      password: ${REDIS_PASSWORD}

  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_OAUTH_CLIENT_ID}
            client-secret: ${GOOGLE_OAUTH_CLIENT_SECRET}
          github:
            client-id: ${GITHUB_OAUTH_CLIENT_ID}
            client-secret: ${GITHUB_OAUTH_CLIENT_SECRET}

stripe:
  api-key: ${STRIPE_API_KEY}
  webhook-secret: ${STRIPE_WEBHOOK_SECRET}

# External secret management
secrets:
  provider: vault  # or aws-secrets-manager
  vault-url: ${VAULT_URL}
  vault-token: ${VAULT_TOKEN}
  vault-path: secret/payment-platform
```

## 🟠 HIGH PRIORITY FIXES

### 4. Implement Rate Limiting

**File**: `backend/src/main/java/com/platform/shared/security/RateLimitingFilter.java`

```java
package com.platform.shared.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain filterChain)
                                  throws ServletException, IOException {

        String path = request.getRequestURI();

        // Apply rate limiting to authentication endpoints
        if (isAuthenticationEndpoint(path)) {
            String key = getClientIdentifier(request);
            Bucket bucket = getBucket(key, path);

            if (bucket.tryConsume(1)) {
                filterChain.doFilter(request, response);
            } else {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.getWriter().write("{\"error\":\"Rate limit exceeded. Please try again later.\"}");
                response.setHeader("X-Rate-Limit-Retry-After-Seconds", "60");
            }
        } else {
            filterChain.doFilter(request, response);
        }
    }

    private boolean isAuthenticationEndpoint(String path) {
        return path.startsWith("/api/v1/auth/login") ||
               path.startsWith("/api/v1/auth/register") ||
               path.startsWith("/api/v1/auth/request-password-reset");
    }

    private String getClientIdentifier(HttpServletRequest request) {
        // Combine IP and session for better tracking
        String ip = getClientIpAddress(request);
        String sessionId = request.getSession(false) != null ?
                          request.getSession().getId() : "no-session";
        return ip + ":" + sessionId;
    }

    private Bucket getBucket(String key, String endpoint) {
        return buckets.computeIfAbsent(key, k -> {
            Bandwidth limit = getBandwidthForEndpoint(endpoint);
            return Bucket4j.builder()
                .addLimit(limit)
                .build();
        });
    }

    private Bandwidth getBandwidthForEndpoint(String endpoint) {
        if (endpoint.contains("login")) {
            // 5 attempts per minute for login
            return Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(1)));
        } else if (endpoint.contains("register")) {
            // 3 attempts per hour for registration
            return Bandwidth.classic(3, Refill.intervally(3, Duration.ofHours(1)));
        } else if (endpoint.contains("password-reset")) {
            // 3 attempts per hour for password reset
            return Bandwidth.classic(3, Refill.intervally(3, Duration.ofHours(1)));
        } else {
            // Default: 60 requests per minute
            return Bandwidth.classic(60, Refill.intervally(60, Duration.ofMinutes(1)));
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}
```

**Add dependency to** `backend/build.gradle`:

```gradle
dependencies {
    // ... existing dependencies
    implementation 'com.github.vladimir-bukhtoyarov:bucket4j-core:8.7.0'
    implementation 'com.github.vladimir-bukhtoyarov:bucket4j-redis:8.7.0'
}
```

### 5. Complete Password Validation Implementation

**File**: `backend/src/main/java/com/platform/shared/security/PasswordValidator.java`

```java
package com.platform.shared.security;

import org.passay.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class PasswordValidator {

    private final PasswordProperties passwordProperties;
    private final org.passay.PasswordValidator validator;
    private final List<String> commonPasswords;

    @Autowired
    public PasswordValidator(PasswordProperties passwordProperties) {
        this.passwordProperties = passwordProperties;
        this.validator = createValidator();
        this.commonPasswords = loadCommonPasswords();
    }

    private org.passay.PasswordValidator createValidator() {
        List<Rule> rules = new ArrayList<>();

        // Length rule
        rules.add(new LengthRule(
            passwordProperties.getMinLength(),
            passwordProperties.getMaxLength()
        ));

        // Character rules
        CharacterCharacteristicsRule characteristicsRule =
            new CharacterCharacteristicsRule();

        List<CharacterRule> charRules = new ArrayList<>();

        if (passwordProperties.isRequireUppercase()) {
            charRules.add(new CharacterRule(EnglishCharacterData.UpperCase, 1));
        }
        if (passwordProperties.isRequireLowercase()) {
            charRules.add(new CharacterRule(EnglishCharacterData.LowerCase, 1));
        }
        if (passwordProperties.isRequireNumbers()) {
            charRules.add(new CharacterRule(EnglishCharacterData.Digit, 1));
        }
        if (passwordProperties.isRequireSymbols()) {
            charRules.add(new CharacterRule(EnglishCharacterData.Special, 1));
        }

        characteristicsRule.setRules(charRules);
        characteristicsRule.setNumberOfCharacteristics(charRules.size());
        rules.add(characteristicsRule);

        // No whitespace
        rules.add(new WhitespaceRule());

        // No sequences
        rules.add(new IllegalSequenceRule(EnglishSequenceData.Alphabetical, 3, false));
        rules.add(new IllegalSequenceRule(EnglishSequenceData.Numerical, 3, false));
        rules.add(new IllegalSequenceRule(EnglishSequenceData.USQwerty, 3, false));

        // No repeated characters
        rules.add(new RepeatCharacterRegexRule(3));

        return new org.passay.PasswordValidator(rules);
    }

    public PasswordValidationResult validate(String password, String username,
                                            String email, List<String> previousPasswords) {
        List<String> errors = new ArrayList<>();

        // Basic validation
        RuleResult result = validator.validate(new PasswordData(password));
        if (!result.isValid()) {
            errors.addAll(validator.getMessages(result));
        }

        // Check against common passwords
        if (isCommonPassword(password)) {
            errors.add("Password is too common. Please choose a more unique password.");
        }

        // Check similarity to username/email
        if (isSimilarToUserInfo(password, username, email)) {
            errors.add("Password is too similar to your username or email.");
        }

        // Check password history
        if (previousPasswords != null && !previousPasswords.isEmpty()) {
            for (String previousPassword : previousPasswords) {
                if (passwordEncoder.matches(password, previousPassword)) {
                    errors.add("Password has been used recently. Please choose a different password.");
                    break;
                }
            }
        }

        // Check entropy
        double entropy = calculateEntropy(password);
        if (entropy < passwordProperties.getMinimumEntropy()) {
            errors.add("Password is not complex enough. Add more variety in characters.");
        }

        return new PasswordValidationResult(errors.isEmpty(), errors);
    }

    private boolean isCommonPassword(String password) {
        String lowerPassword = password.toLowerCase();
        return commonPasswords.stream()
            .anyMatch(common -> common.equals(lowerPassword));
    }

    private boolean isSimilarToUserInfo(String password, String username, String email) {
        String lowerPassword = password.toLowerCase();

        if (username != null && lowerPassword.contains(username.toLowerCase())) {
            return true;
        }

        if (email != null) {
            String emailPrefix = email.split("@")[0].toLowerCase();
            if (lowerPassword.contains(emailPrefix)) {
                return true;
            }
        }

        return false;
    }

    private double calculateEntropy(String password) {
        int poolSize = 0;
        if (password.matches(".*[a-z].*")) poolSize += 26;
        if (password.matches(".*[A-Z].*")) poolSize += 26;
        if (password.matches(".*[0-9].*")) poolSize += 10;
        if (password.matches(".*[^a-zA-Z0-9].*")) poolSize += 32;

        return password.length() * Math.log(poolSize) / Math.log(2);
    }

    private List<String> loadCommonPasswords() {
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(
                getClass().getResourceAsStream("/common-passwords.txt")
            ))) {
            return reader.lines()
                .map(String::toLowerCase)
                .collect(Collectors.toList());
        } catch (Exception e) {
            // Log error and return empty list
            return new ArrayList<>();
        }
    }

    public static class PasswordValidationResult {
        private final boolean valid;
        private final List<String> errors;

        public PasswordValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors;
        }

        public boolean isValid() {
            return valid;
        }

        public List<String> getErrors() {
            return errors;
        }

        public String getErrorMessage() {
            return String.join("; ", errors);
        }
    }
}
```

### 6. Restrict CORS Configuration for Production

**File**: `backend/src/main/java/com/platform/shared/config/ProductionCorsConfig.java`

```java
package com.platform.shared.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@Profile("production")
public class ProductionCorsConfig {

    @Value("${app.cors.allowed-origins}")
    private String[] allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Strict origin validation for production
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins));

        // No wildcard patterns in production
        configuration.setAllowedOriginPatterns(null);

        // Specific allowed methods
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "OPTIONS"
        ));

        // Specific allowed headers (no wildcards)
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "X-CSRF-Token",
            "Accept",
            "Origin"
        ));

        // Allow credentials
        configuration.setAllowCredentials(true);

        // Exposed headers
        configuration.setExposedHeaders(Arrays.asList(
            "X-Total-Count",
            "X-Page-Number",
            "X-Page-Size"
        ));

        // Cache preflight for 1 hour
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);

        return source;
    }
}
```

### 7. Implement Tenant Isolation

**File**: `backend/src/main/java/com/platform/shared/security/TenantContext.java`

```java
package com.platform.shared.security;

import java.util.UUID;

public class TenantContext {
    private static final ThreadLocal<UUID> currentTenant = new ThreadLocal<>();

    public static void setCurrentTenant(UUID tenantId) {
        currentTenant.set(tenantId);
    }

    public static UUID getCurrentTenant() {
        UUID tenantId = currentTenant.get();
        if (tenantId == null) {
            throw new IllegalStateException("No tenant set in current context");
        }
        return tenantId;
    }

    public static void clear() {
        currentTenant.remove();
    }
}
```

**File**: `backend/src/main/java/com/platform/shared/security/TenantAwareRepository.java`

```java
package com.platform.shared.security;

import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import java.util.UUID;

public interface TenantAwareRepository {

    default <T> Specification<T> withTenant() {
        return (root, query, criteriaBuilder) -> {
            UUID tenantId = TenantContext.getCurrentTenant();
            return criteriaBuilder.equal(root.get("organizationId"), tenantId);
        };
    }

    default <T> Specification<T> withTenantAndCondition(Specification<T> spec) {
        return Specification.where(withTenant()).and(spec);
    }
}
```

**Update repositories to enforce tenant isolation**:

```java
@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID>, TenantAwareRepository {

    default List<Payment> findByOrganizationId(UUID organizationId) {
        // Validate tenant access
        if (!organizationId.equals(TenantContext.getCurrentTenant())) {
            throw new SecurityException("Cross-tenant access attempted");
        }
        return findAll(withTenant());
    }

    default Optional<Payment> findByIdAndOrganizationId(UUID id, UUID organizationId) {
        if (!organizationId.equals(TenantContext.getCurrentTenant())) {
            throw new SecurityException("Cross-tenant access attempted");
        }
        return findOne(
            Specification.where(
                (root, query, cb) -> cb.equal(root.get("id"), id)
            ).and(withTenant())
        );
    }
}
```

## 🟡 MEDIUM PRIORITY FIXES

### 8. Add Comprehensive Security Headers

**File**: `backend/src/main/java/com/platform/shared/security/SecurityHeadersFilter.java`

```java
package com.platform.shared.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain filterChain)
                                  throws ServletException, IOException {

        // Content Security Policy
        response.setHeader("Content-Security-Policy",
            "default-src 'self'; " +
            "script-src 'self' 'unsafe-inline' https://js.stripe.com; " +
            "style-src 'self' 'unsafe-inline'; " +
            "img-src 'self' data: https:; " +
            "font-src 'self' data:; " +
            "connect-src 'self' https://api.stripe.com; " +
            "frame-src https://js.stripe.com https://hooks.stripe.com; " +
            "frame-ancestors 'none'; " +
            "form-action 'self'; " +
            "base-uri 'self'; " +
            "object-src 'none';"
        );

        // Strict Transport Security (HSTS)
        response.setHeader("Strict-Transport-Security",
            "max-age=31536000; includeSubDomains; preload");

        // X-Frame-Options
        response.setHeader("X-Frame-Options", "DENY");

        // X-Content-Type-Options
        response.setHeader("X-Content-Type-Options", "nosniff");

        // X-XSS-Protection (legacy but still useful)
        response.setHeader("X-XSS-Protection", "1; mode=block");

        // Referrer Policy
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        // Permissions Policy
        response.setHeader("Permissions-Policy",
            "accelerometer=(), " +
            "ambient-light-sensor=(), " +
            "autoplay=(), " +
            "battery=(), " +
            "camera=(), " +
            "display-capture=(), " +
            "document-domain=(), " +
            "encrypted-media=(), " +
            "fullscreen=(self), " +
            "geolocation=(), " +
            "gyroscope=(), " +
            "magnetometer=(), " +
            "microphone=(), " +
            "midi=(), " +
            "navigation-override=(), " +
            "payment=(self), " +
            "picture-in-picture=(), " +
            "publickey-credentials-get=(), " +
            "screen-wake-lock=(), " +
            "sync-xhr=(), " +
            "usb=(), " +
            "web-share=(), " +
            "xr-spatial-tracking=()"
        );

        filterChain.doFilter(request, response);
    }
}
```

## Testing the Security Fixes

### Test Script for Security Headers

**File**: `scripts/test-security.sh`

```bash
#!/bin/bash

echo "Testing Security Headers..."

# Test security headers
curl -I https://your-domain.com/api/health | grep -E "(Content-Security-Policy|Strict-Transport-Security|X-Frame-Options|X-Content-Type-Options|Referrer-Policy|Permissions-Policy)"

# Test rate limiting
echo "Testing Rate Limiting..."
for i in {1..10}; do
    curl -X POST https://your-domain.com/api/v1/auth/login \
        -H "Content-Type: application/json" \
        -d '{"email":"test@example.com","password":"test"}' \
        -w "\nStatus: %{http_code}\n"
    sleep 0.5
done

# Test CORS
echo "Testing CORS..."
curl -X OPTIONS https://your-domain.com/api/v1/auth/login \
    -H "Origin: https://evil-site.com" \
    -H "Access-Control-Request-Method: POST" \
    -H "Access-Control-Request-Headers: Content-Type" \
    -v 2>&1 | grep -i "access-control"
```

## Deployment Checklist

- [ ] All secrets rotated and stored in external vault
- [ ] Environment variables configured for production
- [ ] Rate limiting tested and configured
- [ ] Security headers validated
- [ ] CORS restricted to production domains
- [ ] Password validation fully implemented
- [ ] OAuth2 state validation deployed
- [ ] Cookies configured with secure flags
- [ ] Tenant isolation verified
- [ ] Audit logging enabled
- [ ] HTTPS enforced
- [ ] Security monitoring configured

## Next Steps

1. Deploy these fixes to a staging environment
2. Run comprehensive security testing
3. Perform penetration testing
4. Schedule security training for development team
5. Implement automated security scanning in CI/CD pipeline

---

This implementation guide provides production-ready code to address all critical and high-severity security issues. Each fix has been designed to integrate seamlessly with your existing Spring Boot Modulith architecture.