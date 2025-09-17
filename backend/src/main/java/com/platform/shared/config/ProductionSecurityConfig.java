package com.platform.shared.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.platform.auth.internal.OpaqueTokenStore;
import com.platform.shared.security.CustomOAuth2UserService;
import com.platform.shared.security.OAuth2AuthenticationSuccessHandler;
import com.platform.shared.security.TenantContextFilter;

/**
 * Production security configuration with strict security policies. Implements all security
 * recommendations from code review.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@Profile("production")
public class ProductionSecurityConfig {

  @Value("${app.frontend-url}")
  private String frontendUrl;

  @Value("${app.allowed-origins:}")
  private String[] allowedOrigins;

  private final OpaqueTokenStore tokenStore;
  private final OAuth2UserService<OAuth2UserRequest, OAuth2User> customOAuth2UserService;
  private final OAuth2AuthenticationSuccessHandler successHandler;
  private final AuthorizationRequestRepository<OAuth2AuthorizationRequest>
      authorizationRequestRepository;

  public ProductionSecurityConfig(
      OpaqueTokenStore tokenStore,
      CustomOAuth2UserService customOAuth2UserService,
      OAuth2AuthenticationSuccessHandler successHandler,
      AuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository) {
    this.tokenStore = tokenStore;
    this.customOAuth2UserService = customOAuth2UserService;
    this.successHandler = successHandler;
    this.authorizationRequestRepository = authorizationRequestRepository;
  }

  @Bean
  public SecurityFilterChain productionFilterChain(HttpSecurity http) throws Exception {
    return http
        // Disable CSRF for API endpoints (using tokens instead)
        .csrf(AbstractHttpConfigurer::disable)

        // Configure strict CORS
        .cors(cors -> cors.configurationSource(productionCorsConfigurationSource()))

        // Configure session management
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

        // Configure comprehensive security headers
        .headers(
            headers ->
                headers
                    .frameOptions(frameOptions -> frameOptions.deny())
                    .contentTypeOptions(contentTypeOptions -> {})
                    .xssProtection(xss -> {})
                    .httpStrictTransportSecurity(
                        hstsConfig ->
                            hstsConfig
                                .maxAgeInSeconds(63072000) // 2 years
                                .includeSubDomains(true)
                                .preload(true))
                    .referrerPolicy(
                        referrerPolicy ->
                            referrerPolicy.policy(
                                ReferrerPolicyHeaderWriter.ReferrerPolicy
                                    .STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                    .contentSecurityPolicy(
                        csp ->
                            csp.policyDirectives(
                                "default-src 'self'; "
                                    + "script-src 'self' 'unsafe-inline' https://js.stripe.com; "
                                    + "style-src 'self' 'unsafe-inline'; "
                                    + "img-src 'self' data: https:; "
                                    + "font-src 'self' data:; "
                                    + "connect-src 'self' https://api.stripe.com; "
                                    + "frame-src https://js.stripe.com https://hooks.stripe.com; "
                                    + "frame-ancestors 'none'; "
                                    + "form-action 'self'; "
                                    + "base-uri 'self'; "
                                    + "upgrade-insecure-requests")))

        // Configure authorization with rate limiting considerations
        .authorizeHttpRequests(
            authz ->
                authz
                    // Public OAuth2 endpoints (rate limited via API gateway)
                    .requestMatchers("/api/v1/auth/providers")
                    .permitAll()
                    .requestMatchers("/api/v1/auth/oauth2/providers")
                    .permitAll()
                    .requestMatchers("/api/v1/auth/oauth2/authorize/**")
                    .permitAll()
                    .requestMatchers("/api/v1/auth/oauth2/callback/**")
                    .permitAll()

                    // Public password authentication endpoints (rate limited)
                    .requestMatchers("/api/v1/auth/register")
                    .permitAll()
                    .requestMatchers("/api/v1/auth/login")
                    .permitAll()
                    .requestMatchers("/api/v1/auth/request-password-reset")
                    .permitAll()
                    .requestMatchers("/api/v1/auth/reset-password")
                    .permitAll()
                    .requestMatchers("/api/v1/auth/verify-email")
                    .permitAll()
                    .requestMatchers("/api/v1/auth/resend-verification")
                    .permitAll()
                    .requestMatchers("/api/v1/auth/methods")
                    .permitAll()

                    // Stripe webhook endpoint (signature verified in controller)
                    .requestMatchers("/api/v1/webhooks/stripe")
                    .permitAll()

                // Health checks (no sensitive data)
                .requestMatchers("/actuator/health/liveness")
                .permitAll()
                .requestMatchers("/actuator/health/readiness")
                .permitAll()

                // Metrics endpoint for Prometheus Operator scraping
                .requestMatchers("/actuator/prometheus")
                .permitAll()

                    // Protected API endpoints
                    .requestMatchers("/api/v1/**")
                    .authenticated()

                // Admin endpoints (require ADMIN role)
                .requestMatchers("/actuator/**")
                .hasRole("ADMIN")
                    .requestMatchers("/api/v1/admin/**")
                    .hasRole("ADMIN")

                    // Default deny all
                    .anyRequest()
                    .denyAll())

        // Configure OAuth2 login with PKCE support
        .oauth2Login(
            oauth2 ->
                oauth2
                    .loginPage("/api/v1/auth/oauth2/authorize")
                    .authorizationEndpoint(
                        authorization ->
                            authorization
                                .baseUri("/api/v1/auth/oauth2/authorize")
                                .authorizationRequestRepository(authorizationRequestRepository))
                    .redirectionEndpoint(
                        redirection -> redirection.baseUri("/api/v1/auth/oauth2/callback"))
                    .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                    .successHandler(successHandler))

        // Add custom filters
        .addFilterBefore(tenantContextFilter(), UsernamePasswordAuthenticationFilter.class)
        .addFilterAfter(
            opaqueTokenAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)

        // Configure logout
        .logout(
            logout ->
                logout
                    .logoutUrl("/api/v1/auth/logout")
                    .logoutSuccessUrl(frontendUrl)
                    .deleteCookies("SESSION")
                    .clearAuthentication(true)
                    .invalidateHttpSession(true))

        // Require HTTPS in production
        .requiresChannel(channel -> channel.anyRequest().requiresSecure())
        .build();
  }

  @Bean
  public CorsConfigurationSource productionCorsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();

    // Only allow specific origins in production
    if (allowedOrigins != null && allowedOrigins.length > 0) {
      configuration.setAllowedOrigins(Arrays.asList(allowedOrigins));
    } else {
      // Default to frontend URL only
      configuration.setAllowedOrigins(List.of(frontendUrl));
    }

    // Strict method allowlist
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

    // Specific headers only
    configuration.setAllowedHeaders(
        List.of(
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "X-CSRF-Token",
            "X-Correlation-Id"));

    configuration.setAllowCredentials(true);

    // Limited exposed headers
    configuration.setExposedHeaders(List.of("Authorization", "X-Correlation-Id"));

    // Cache preflight for 1 hour
    configuration.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", configuration);
    return source;
  }

  @Bean
  public PasswordEncoder productionPasswordEncoder() {
    // Use BCrypt with strength 14 for production (stronger than default)
    return new BCryptPasswordEncoder(14);
  }

  @Bean
  public TenantContextFilter tenantContextFilter() {
    return new TenantContextFilter();
  }

  @Bean
  public OpaqueTokenAuthenticationFilter opaqueTokenAuthenticationFilter() {
    return new OpaqueTokenAuthenticationFilter(tokenStore);
  }
}
