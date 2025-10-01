# Architecture Fixes Implementation Guide

**Generated**: 2025-09-30
**Priority**: CRITICAL
**Estimated Effort**: 6-8 weeks

## Executive Summary

This guide provides step-by-step instructions to fix the critical architecture issues identified in the comprehensive analysis. The system currently has:

- ❌ **5 of 6 modules missing** (auth, user, payment, subscription, shared)
- ❌ **0% security compliance** (no OAuth2, JWT violation)
- ❌ **0% backend test coverage** (TDD violation)
- ❌ **Missing infrastructure** (Docker, K8s)

## Phase 1: Create Missing Modules (Priority 1)

### 1.1 Shared Module (STARTED ✅)

**Status**: Foundation created
**Location**: `backend/src/main/java/com/platform/shared/`

**Already Created**:
- ✅ `package-info.java`
- ✅ `events/DomainEvent.java`
- ✅ `exceptions/DomainException.java`
- ✅ `exceptions/ValidationException.java`
- ✅ `exceptions/ResourceNotFoundException.java`
- ✅ `types/Money.java`

**TODO**: Add remaining shared components
```java
// shared/types/Email.java
public record Email(String value) {
    public Email {
        if (!value.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new ValidationException("Invalid email format");
        }
    }
}

// shared/security/PasswordPolicy.java
public interface PasswordPolicy {
    boolean isValid(String password);
    String getRequirements();
}
```

### 1.2 Auth Module (STARTED ✅)

**Status**: Structure created
**Location**: `backend/src/main/java/com/platform/auth/`

**Created**:
- ✅ `package-info.java`
- ✅ Directory structure (`api/`, `internal/`, `events/`)

**TODO**: Implement core auth components

#### User Entity
```java
// auth/User.java
package com.platform.auth;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auth_users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    private String firstName;
    private String lastName;

    @Enumerated(EnumType.STRING)
    private UserStatus status;

    @Column(name = "failed_login_attempts")
    private int failedLoginAttempts;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    // Getters, setters, constructors
}

enum UserStatus {
    ACTIVE, LOCKED, DISABLED, PENDING_VERIFICATION
}
```

#### Auth Repository
```java
// auth/internal/UserRepository.java
package com.platform.auth.internal;

import com.platform.auth.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;
import java.util.UUID;

interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.email = :email AND u.status = 'ACTIVE'")
    Optional<User> findActiveUserByEmail(String email);

    boolean existsByEmail(String email);
}
```

#### Auth Service
```java
// auth/internal/AuthenticationService.java
package com.platform.auth.internal;

import com.platform.auth.User;
import com.platform.auth.events.UserAuthenticatedEvent;
import com.platform.shared.exceptions.ValidationException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OpaqueTokenService tokenService;
    private final ApplicationEventPublisher eventPublisher;

    // Constructor injection

    @Transactional
    public String authenticate(String email, String password) {
        User user = userRepository.findActiveUserByEmail(email)
            .orElseThrow(() -> new ValidationException("Invalid credentials"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            handleFailedLogin(user);
            throw new ValidationException("Invalid credentials");
        }

        resetFailedAttempts(user);
        String token = tokenService.generateToken(user);

        eventPublisher.publishEvent(new UserAuthenticatedEvent(
            user.getId(), user.getEmail(), Instant.now()
        ));

        return token;
    }

    private void handleFailedLogin(User user) {
        user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);

        if (user.getFailedLoginAttempts() >= 5) {
            user.setStatus(UserStatus.LOCKED);
            user.setLockedUntil(Instant.now().plus(Duration.ofMinutes(30)));
        }

        userRepository.save(user);
    }

    private void resetFailedAttempts(User user) {
        if (user.getFailedLoginAttempts() > 0) {
            user.setFailedLoginAttempts(0);
            userRepository.save(user);
        }
    }
}
```

#### Opaque Token Service
```java
// auth/internal/OpaqueTokenService.java
package com.platform.auth.internal;

import com.platform.auth.User;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

@Service
class OpaqueTokenService {

    private static final String TOKEN_PREFIX = "token:";
    private static final int TOKEN_LENGTH = 32;
    private static final Duration TOKEN_EXPIRY = Duration.ofHours(24);

    private final RedisTemplate<String, String> redisTemplate;
    private final SecureRandom secureRandom;

    // Constructor

    public String generateToken(User user) {
        String token = generateRandomToken();
        String key = TOKEN_PREFIX + token;

        redisTemplate.opsForValue().set(
            key,
            user.getId().toString(),
            TOKEN_EXPIRY
        );

        return token;
    }

    public Optional<UUID> validateToken(String token) {
        String key = TOKEN_PREFIX + token;
        String userId = redisTemplate.opsForValue().get(key);

        if (userId == null) {
            return Optional.empty();
        }

        // Extend token expiry on use (sliding expiration)
        redisTemplate.expire(key, TOKEN_EXPIRY);

        return Optional.of(UUID.fromString(userId));
    }

    public void revokeToken(String token) {
        String key = TOKEN_PREFIX + token;
        redisTemplate.delete(key);
    }

    private String generateRandomToken() {
        byte[] bytes = new byte[TOKEN_LENGTH];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
```

#### Auth Controller
```java
// auth/api/AuthController.java
package com.platform.auth.api;

import com.platform.auth.internal.AuthenticationService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
class AuthController {

    private final AuthenticationService authService;

    // Constructor

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        String token = authService.authenticate(request.email(), request.password());

        // Set httpOnly cookie for token (constitutional requirement)
        ResponseCookie cookie = ResponseCookie.from("auth_token", token)
            .httpOnly(true)
            .secure(true) // HTTPS only
            .sameSite("Strict")
            .path("/")
            .maxAge(24 * 60 * 60) // 24 hours
            .build();

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, cookie.toString())
            .body(new LoginResponse("Login successful"));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue("auth_token") String token) {
        authService.revokeToken(token);

        ResponseCookie cookie = ResponseCookie.from("auth_token", "")
            .httpOnly(true)
            .secure(true)
            .sameSite("Strict")
            .path("/")
            .maxAge(0)
            .build();

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, cookie.toString())
            .build();
    }
}

record LoginRequest(String email, String password) {}
record LoginResponse(String message) {}
```

#### Events
```java
// auth/events/UserAuthenticatedEvent.java
package com.platform.auth.events;

import com.platform.shared.events.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record UserAuthenticatedEvent(
    UUID userId,
    String email,
    Instant occurredOn
) implements DomainEvent {}

// auth/events/UserRegisteredEvent.java
public record UserRegisteredEvent(
    UUID userId,
    String email,
    Instant occurredOn
) implements DomainEvent {}

// auth/events/PasswordChangedEvent.java
public record PasswordChangedEvent(
    UUID userId,
    Instant occurredOn
) implements DomainEvent {}
```

### 1.3 User Module

**Location**: `backend/src/main/java/com/platform/user/`

**Structure**:
```
user/
├── package-info.java
├── UserProfile.java (entity)
├── api/
│   └── UserController.java
├── internal/
│   ├── UserProfileRepository.java
│   └── UserProfileService.java
└── events/
    ├── UserProfileUpdatedEvent.java
    └── UserPreferencesChangedEvent.java
```

**Key Implementation**:
```java
// user/UserProfile.java
@Entity
@Table(name = "user_profiles")
public class UserProfile {
    @Id
    private UUID userId; // References auth.User

    private String displayName;
    private String avatarUrl;
    private String timezone;
    private String language;

    @Convert(converter = JpaConverterJson.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> preferences;

    // Additional profile fields
}
```

### 1.4 Payment Module

**Location**: `backend/src/main/java/com/platform/payment/`

**Structure**:
```
payment/
├── package-info.java
├── Payment.java (entity)
├── PaymentMethod.java (entity)
├── api/
│   ├── PaymentController.java
│   └── dto/
│       ├── CreatePaymentRequest.java
│       └── PaymentResponse.java
├── internal/
│   ├── PaymentRepository.java
│   ├── PaymentMethodRepository.java
│   ├── PaymentService.java
│   └── StripeIntegrationService.java
└── events/
    ├── PaymentProcessedEvent.java
    ├── PaymentFailedEvent.java
    └── PaymentMethodAddedEvent.java
```

**Key Implementation**:
```java
// payment/Payment.java
@Entity
@Table(name = "payment_payments")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID organizationId;
    private UUID subscriptionId;

    @Embedded
    private Money amount; // Using shared.types.Money

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private String stripePaymentIntentId;
    private Instant createdAt;
    private Instant updatedAt;
}

enum PaymentStatus {
    PENDING, PROCESSING, SUCCEEDED, FAILED, REFUNDED
}
```

### 1.5 Subscription Module

**Location**: `backend/src/main/java/com/platform/subscription/`

**Structure**:
```
subscription/
├── package-info.java
├── Subscription.java (entity)
├── SubscriptionPlan.java (entity)
├── api/
│   └── SubscriptionController.java
├── internal/
│   ├── SubscriptionRepository.java
│   ├── SubscriptionService.java
│   └── BillingService.java
└── events/
    ├── SubscriptionCreatedEvent.java
    ├── SubscriptionRenewedEvent.java
    └── SubscriptionCancelledEvent.java
```

**Key Implementation**:
```java
// subscription/Subscription.java
@Entity
@Table(name = "subscription_subscriptions")
public class Subscription {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID organizationId;

    @ManyToOne
    @JoinColumn(name = "plan_id")
    private SubscriptionPlan plan;

    @Enumerated(EnumType.STRING)
    private SubscriptionStatus status;

    private Instant currentPeriodStart;
    private Instant currentPeriodEnd;

    private String stripeSubscriptionId;
}

enum SubscriptionStatus {
    ACTIVE, PAUSED, CANCELED, EXPIRED, TRIAL
}
```

## Phase 2: Fix Database Migrations (Priority 2)

### Current Issue
All tables in one file: `V001__initial_schema.sql`

### Solution
Split into module-specific migrations:

```bash
# Create new migration structure
mkdir -p backend/src/main/resources/db/migration/modules/{auth,user,payment,subscription,organization,audit}
```

**New Migrations**:

```sql
-- V001__auth_schema.sql
CREATE TABLE IF NOT EXISTS auth_users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255),
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING_VERIFICATION',
    failed_login_attempts INT NOT NULL DEFAULT 0,
    locked_until TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_auth_users_email ON auth_users(email) WHERE deleted_at IS NULL;
CREATE INDEX idx_auth_users_status ON auth_users(status);

-- V002__user_schema.sql
CREATE TABLE IF NOT EXISTS user_profiles (
    user_id UUID PRIMARY KEY REFERENCES auth_users(id),
    display_name VARCHAR(255),
    avatar_url TEXT,
    timezone VARCHAR(50) DEFAULT 'UTC',
    language VARCHAR(10) DEFAULT 'en',
    preferences JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- V003__payment_schema.sql
CREATE TABLE IF NOT EXISTS payment_payment_methods (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    type VARCHAR(50) NOT NULL,
    stripe_payment_method_id VARCHAR(255),
    last_four VARCHAR(4),
    brand VARCHAR(50),
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS payment_payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    subscription_id UUID,
    amount_value DECIMAL(10, 2) NOT NULL,
    amount_currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    status VARCHAR(50) NOT NULL,
    stripe_payment_intent_id VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_payment_payments_org_id ON payment_payments(organization_id);
CREATE INDEX idx_payment_payments_status ON payment_payments(status);

-- V004__subscription_schema.sql
CREATE TABLE IF NOT EXISTS subscription_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    amount_value DECIMAL(10, 2) NOT NULL,
    amount_currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    billing_period VARCHAR(20) NOT NULL,
    features JSONB,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS subscription_subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    plan_id UUID NOT NULL REFERENCES subscription_plans(id),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    current_period_start TIMESTAMP WITH TIME ZONE NOT NULL,
    current_period_end TIMESTAMP WITH TIME ZONE NOT NULL,
    stripe_subscription_id VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_subscription_subscriptions_org_id ON subscription_subscriptions(organization_id);
CREATE INDEX idx_subscription_subscriptions_status ON subscription_subscriptions(status);

-- V005__organization_schema.sql
CREATE TABLE IF NOT EXISTS organizations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS organization_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id),
    user_id UUID NOT NULL REFERENCES auth_users(id),
    role VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(organization_id, user_id)
);

-- V006__audit_schema.sql
CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type VARCHAR(100) NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    user_id UUID REFERENCES auth_users(id),
    organization_id UUID REFERENCES organizations(id),
    event_data JSONB NOT NULL,
    metadata JSONB,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45),
    user_agent TEXT
);

CREATE INDEX idx_audit_logs_event_type ON audit_logs(event_type);
CREATE INDEX idx_audit_logs_aggregate ON audit_logs(aggregate_type, aggregate_id);
CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_org_id ON audit_logs(organization_id);
CREATE INDEX idx_audit_logs_timestamp ON audit_logs(timestamp DESC);
```

### Migration Execution

Update `application.yml`:
```yaml
spring:
  flyway:
    locations:
      - classpath:db/migration
      - classpath:db/migration/modules/{auth,user,payment,subscription,organization,audit}
    baseline-on-migrate: true
    validate-on-migrate: true
```

## Phase 3: Implement Security Configuration (Priority 3)

### 3.1 SecurityConfig

```java
// config/SecurityConfig.java
package com.platform.config;

import com.platform.auth.internal.OpaqueTokenAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final OpaqueTokenAuthenticationFilter tokenFilter;

    public SecurityConfig(OpaqueTokenAuthenticationFilter tokenFilter) {
        this.tokenFilter = tokenFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
            )
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/login", "/api/v1/auth/register").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/api-docs/**", "/swagger-ui/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(tokenFilter, UsernamePasswordAuthenticationFilter.class)
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives("default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'")
                )
                .frameOptions(frame -> frame.deny())
                .xssProtection(xss -> xss.headerValue("1; mode=block"))
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000)
                )
            );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:3000")); // Frontend URL
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
```

### 3.2 Opaque Token Authentication Filter

```java
// auth/internal/OpaqueTokenAuthenticationFilter.java
package com.platform.auth.internal;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

@Component
public class OpaqueTokenAuthenticationFilter extends OncePerRequestFilter {

    private final OpaqueTokenService tokenService;
    private final UserRepository userRepository;

    public OpaqueTokenAuthenticationFilter(OpaqueTokenService tokenService, UserRepository userRepository) {
        this.tokenService = tokenService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        extractToken(request).ifPresent(token -> {
            tokenService.validateToken(token).ifPresent(userId -> {
                userRepository.findById(userId).ifPresent(user -> {
                    UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(user, null, List.of());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                });
            });
        });

        filterChain.doFilter(request, response);
    }

    private Optional<String> extractToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }

        return Arrays.stream(cookies)
            .filter(cookie -> "auth_token".equals(cookie.getName()))
            .map(Cookie::getValue)
            .findFirst();
    }
}
```

### 3.3 Redis Configuration

```java
// config/RedisConfig.java
package com.platform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 86400) // 24 hours
public class RedisConfig {

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        return template;
    }
}
```

## Phase 4: Fix Frontend Authentication (Priority 4)

### 4.1 Update authApi.ts

Replace JWT handling with opaque token + httpOnly cookies:

```typescript
// frontend/src/store/api/authApi.ts
import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  message: string;
  // NO TOKEN IN RESPONSE - it's in httpOnly cookie
}

export interface RegisterRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
}

export const authApi = createApi({
  reducerPath: 'authApi',
  baseQuery: fetchBaseQuery({
    baseUrl: '/api/v1',
    credentials: 'include', // CRITICAL: Include cookies in requests
    prepareHeaders: (headers) => {
      // Don't manually add Authorization header
      // Cookies are sent automatically with credentials: 'include'
      return headers;
    },
  }),
  endpoints: (builder) => ({
    login: builder.mutation<LoginResponse, LoginRequest>({
      query: (credentials) => ({
        url: '/auth/login',
        method: 'POST',
        body: credentials,
      }),
      // Remove localStorage.setItem - token is in httpOnly cookie
    }),

    register: builder.mutation<LoginResponse, RegisterRequest>({
      query: (userData) => ({
        url: '/auth/register',
        method: 'POST',
        body: userData,
      }),
    }),

    logout: builder.mutation<void, void>({
      query: () => ({
        url: '/auth/logout',
        method: 'POST',
      }),
      // Cookie cleared by backend
    }),

    getCurrentUser: builder.query<User, void>({
      query: () => '/auth/me',
    }),
  }),
});

export const {
  useLoginMutation,
  useRegisterMutation,
  useLogoutMutation,
  useGetCurrentUserQuery,
} = authApi;
```

### 4.2 Remove localStorage Token Usage

**Find and remove**:
```typescript
// DELETE ALL OCCURRENCES:
localStorage.getItem('token')
localStorage.setItem('token', ...)
localStorage.removeItem('token')
localStorage.getItem('refreshToken')
localStorage.setItem('refreshToken', ...)
localStorage.removeItem('refreshToken')
```

**Replace with**: Nothing! Cookies are handled automatically.

### 4.3 Update Auth Slice

```typescript
// frontend/src/store/slices/authSlice.ts
import { createSlice } from '@reduxjs/toolkit';
import { authApi } from '../api/authApi';

interface AuthState {
  user: User | null;
  isAuthenticated: boolean;
}

const initialState: AuthState = {
  user: null,
  isAuthenticated: false,
};

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    clearAuth: (state) => {
      state.user = null;
      state.isAuthenticated = false;
    },
  },
  extraReducers: (builder) => {
    builder
      .addMatcher(
        authApi.endpoints.login.matchFulfilled,
        (state) => {
          state.isAuthenticated = true;
        }
      )
      .addMatcher(
        authApi.endpoints.getCurrentUser.matchFulfilled,
        (state, action) => {
          state.user = action.payload;
          state.isAuthenticated = true;
        }
      )
      .addMatcher(
        authApi.endpoints.logout.matchFulfilled,
        (state) => {
          state.user = null;
          state.isAuthenticated = false;
        }
      );
  },
});

export const { clearAuth } = authSlice.actions;
export default authSlice.reducer;
```

## Phase 5: Add Backend Test Infrastructure (Priority 5)

### 5.1 Test Configuration

```yaml
# backend/src/test/resources/application-test.yml
spring:
  datasource:
    url: jdbc:tc:postgresql:15:///testdb
    driver-class-name: org.testcontainers.jdbc.ContainerDatabaseDriver

  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true

  redis:
    host: localhost
    port: 6379

  flyway:
    enabled: true
    locations: classpath:db/migration
```

### 5.2 Abstract Integration Test

```java
// backend/src/test/java/com/platform/AbstractIntegrationTest.java
package com.platform;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
public abstract class AbstractIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", redis::getFirstMappedPort);
    }
}
```

### 5.3 Example Integration Test

```java
// backend/src/test/java/com/platform/auth/AuthenticationIntegrationTest.java
package com.platform.auth;

import com.platform.AbstractIntegrationTest;
import com.platform.auth.api.AuthController;
import com.platform.auth.internal.AuthenticationService;
import com.platform.auth.internal.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class AuthenticationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldAuthenticateUserWithValidCredentials() throws Exception {
        // Given: User exists in database
        User user = new User();
        user.setEmail("test@example.com");
        user.setPasswordHash("$2a$12$hashedPassword"); // BCrypt
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        // When: Login request sent
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "test@example.com",
                      "password": "correctPassword"
                    }
                    """))
            // Then: Success response with httpOnly cookie
            .andExpect(status().isOk())
            .andExpect(cookie().exists("auth_token"))
            .andExpect(cookie().httpOnly("auth_token", true))
            .andExpect(cookie().secure("auth_token", true))
            .andExpect(jsonPath("$.message").value("Login successful"));
    }

    @Test
    void shouldRejectInvalidCredentials() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "test@example.com",
                      "password": "wrongPassword"
                    }
                    """))
            .andExpect(status().isUnauthorized());
    }
}
```

### 5.4 Contract Test Example

```java
// backend/src/test/java/com/platform/auth/AuthContractTest.java
package com.platform.auth;

import com.platform.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class AuthContractTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void loginEndpoint_shouldMatchContract() throws Exception {
        // Validates API contract from specs/
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType("application/json")
                .content("{\"email\":\"test@example.com\",\"password\":\"password\"}"))
            .andExpect(status().isAnyOf(200, 401))
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.message").exists());
    }
}
```

## Phase 6: Update CI/CD (Priority 6)

### 6.1 Add Constitutional Compliance Check

```yaml
# .github/workflows/backend-ci.yml
# Add BEFORE test job

constitutional-compliance:
  name: Constitutional Compliance
  runs-on: ubuntu-latest
  steps:
    - uses: actions/checkout@v4

    - name: Set up JDK 21
      uses: actions/setup-java@v4
      with:
        java-version: '21'
        distribution: 'temurin'

    - name: Verify Module Structure
      run: |
        cd backend
        ./gradlew verifyModulithStructure

    - name: Verify Test-First Development
      run: |
        cd backend
        ./gradlew verifyTestsExist --fail-if-no-tests

    - name: Verify Real Dependencies in Tests
      run: |
        cd backend
        ./gradlew verifyTestContainersUsage
```

### 6.2 Add Gradle Tasks

```kotlin
// backend/build.gradle
tasks.register("verifyModulithStructure") {
    group = "verification"
    description = "Verifies Spring Modulith module boundaries"

    doLast {
        javaexec {
            classpath = sourceSets.test.runtimeClasspath
            mainClass = "com.platform.ModulithStructureVerifier"
        }
    }
}

tasks.register("verifyTestsExist") {
    group = "verification"
    description = "Verifies that tests exist for all production code"

    doLast {
        val srcFiles = fileTree("src/main/java") {
            include("**/*.java")
            exclude("**/package-info.java")
        }.files.size

        val testFiles = fileTree("src/test/java") {
            include("**/*Test.java")
        }.files.size

        if (testFiles == 0 && srcFiles > 0) {
            throw GradleException("No tests found but production code exists! TDD violation.")
        }

        println("✅ Found $testFiles test files for $srcFiles source files")
    }
}

tasks.register("verifyTestContainersUsage") {
    group = "verification"
    description = "Verifies that integration tests use TestContainers, not mocks"

    doLast {
        val integrationTests = fileTree("src/test/java") {
            include("**/*IntegrationTest.java")
        }

        integrationTests.forEach { file ->
            val content = file.readText()
            if (content.contains("@Mock") || content.contains("@MockBean")) {
                throw GradleException(
                    "Integration test ${file.name} uses mocks! " +
                    "Constitutional requirement: Use real dependencies with TestContainers."
                )
            }
        }

        println("✅ All integration tests use real dependencies")
    }
}
```

### 6.3 Add Frontend Test Pipeline

```yaml
# .github/workflows/frontend-ci.yml
# Replace test job with:

test-suite:
  name: Test Suite
  runs-on: ubuntu-latest
  needs: build
  strategy:
    matrix:
      test-type: [unit, component, e2e]

  services:
    backend:
      image: openjdk:21-slim
      # Start backend for integration tests

  steps:
    - uses: actions/checkout@v4

    - name: Setup Node.js
      uses: actions/setup-node@v4
      with:
        node-version: '20'
        cache: 'npm'
        cache-dependency-path: frontend/package-lock.json

    - name: Install Dependencies
      run: |
        cd frontend
        npm ci --frozen-lockfile

    - name: Install Playwright Browsers
      if: matrix.test-type == 'e2e'
      run: |
        cd frontend
        npx playwright install --with-deps

    - name: Run ${{ matrix.test-type }} Tests
      run: |
        cd frontend
        npm run test:${{ matrix.test-type }}

    - name: Upload Coverage
      uses: codecov/codecov-action@v3
      with:
        files: ./frontend/coverage/coverage-final.json
        flags: ${{ matrix.test-type }}
```

## Phase 7: Create Docker Infrastructure (Priority 7)

### 7.1 Backend Dockerfile

```dockerfile
# backend/Dockerfile
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

COPY gradle/ gradle/
COPY gradlew .
COPY build.gradle settings.gradle ./
COPY src/ src/

RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
```

### 7.2 Frontend Dockerfile

```dockerfile
# frontend/Dockerfile
FROM node:20-alpine AS builder
WORKDIR /app

COPY package*.json ./
RUN npm ci --frozen-lockfile

COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
HEALTHCHECK --interval=30s --timeout=3s \
  CMD wget --no-verbose --tries=1 --spider http://localhost/health || exit 1

CMD ["nginx", "-g", "daemon off;"]
```

### 7.3 Docker Compose for Development

```yaml
# docker-compose.dev.yml
version: '3.8'

services:
  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: platform
      POSTGRES_USER: platform
      POSTGRES_PASSWORD: platform
    ports:
      - "5432:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U platform"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 3s
      retries: 3

  backend:
    build:
      context: ./backend
      dockerfile: Dockerfile
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: dev
      DB_HOST: postgres
      DB_PORT: 5432
      DB_NAME: platform
      DB_USERNAME: platform
      DB_PASSWORD: platform
      REDIS_HOST: redis
      REDIS_PORT: 6379
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "wget", "--spider", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  frontend:
    build:
      context: ./frontend
      dockerfile: Dockerfile
    ports:
      - "3000:80"
    depends_on:
      - backend

volumes:
  postgres-data:
```

### 7.4 Kubernetes Deployment

```yaml
# k8s/base/backend-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: sass-backend
  labels:
    app: sass-backend
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  selector:
    matchLabels:
      app: sass-backend
  template:
    metadata:
      labels:
        app: sass-backend
    spec:
      containers:
      - name: backend
        image: sass-backend:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "production"
        - name: DB_HOST
          valueFrom:
            secretKeyRef:
              name: database-credentials
              key: host
        - name: DB_USERNAME
          valueFrom:
            secretKeyRef:
              name: database-credentials
              key: username
        - name: DB_PASSWORD
          valueFrom:
            secretKeyRef:
              name: database-credentials
              key: password
        - name: REDIS_HOST
          valueFrom:
            configMapKeyRef:
              name: app-config
              key: redis.host
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 5
          timeoutSeconds: 3
          failureThreshold: 3

---
apiVersion: v1
kind: Service
metadata:
  name: sass-backend
spec:
  selector:
    app: sass-backend
  ports:
  - protocol: TCP
    port: 80
    targetPort: 8080
  type: ClusterIP

---
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: sass-backend
  annotations:
    cert-manager.io/cluster-issuer: letsencrypt-prod
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
spec:
  ingressClassName: nginx
  tls:
  - hosts:
    - api.sass-platform.com
    secretName: sass-backend-tls
  rules:
  - host: api.sass-platform.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: sass-backend
            port:
              number: 80
```

## Verification Checklist

After implementing all fixes:

### Backend
- [ ] All 6 modules exist with proper structure
- [ ] package-info.java in every package
- [ ] Database migrations split by module
- [ ] SecurityConfig with OAuth2 implemented
- [ ] Opaque token service with Redis
- [ ] httpOnly cookies for authentication
- [ ] AbstractIntegrationTest with TestContainers
- [ ] At least 20 integration tests passing
- [ ] All ArchUnit/Modulith tests passing

### Frontend
- [ ] authApi updated to use cookies
- [ ] All localStorage token code removed
- [ ] credentials: 'include' in fetch config
- [ ] Component tests added (80%+ coverage)
- [ ] E2E tests running in CI

### CI/CD
- [ ] Constitutional compliance job passing
- [ ] Test hierarchy enforced (contract→integration→e2e→unit)
- [ ] Quality gates implemented
- [ ] Security scanning comprehensive
- [ ] Docker images built and scanned
- [ ] K8s manifests validated

### Infrastructure
- [ ] Dockerfile for backend
- [ ] Dockerfile for frontend
- [ ] docker-compose.dev.yml working
- [ ] K8s deployments validated
- [ ] Health checks configured

## Success Metrics

**Before**:
- Module completeness: 16.7%
- Security compliance: 0%
- Test coverage (backend): 0%
- Constitutional compliance: 22.5%

**After**:
- Module completeness: 100%
- Security compliance: 95%+
- Test coverage (backend): 80%+
- Constitutional compliance: 100%

## Next Steps

1. **Week 1-2**: Implement missing modules (Phases 1-2)
2. **Week 3**: Add security configuration (Phase 3)
3. **Week 4**: Fix frontend auth + add tests (Phases 4-5)
4. **Week 5**: Update CI/CD (Phase 6)
5. **Week 6**: Add infrastructure (Phase 7)
6. **Week 7-8**: Testing, validation, documentation

---

**End of Implementation Guide**
