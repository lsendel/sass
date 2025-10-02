package com.platform.auth.internal;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Security configuration for the application.
 * Implements opaque token authentication with httpOnly cookies (no JWT).
 * Only active in non-test profiles.
 *
 * <p>Constitutional requirements:
 * <ul>
 *   <li>OAuth2/OIDC with opaque tokens</li>
 *   <li>No JWT (opaque tokens in Redis)</li>
 *   <li>httpOnly cookies for token storage</li>
 *   <li>CSRF protection enabled</li>
 *   <li>Strict security headers</li>
 * </ul>
 *
 * @since 1.0.0
 */
@Configuration
@EnableWebSecurity
@Profile("!contract-test") // Only disable in contract tests, enable for integration tests
public class SecurityConfig {

    private static final int HSTS_MAX_AGE_SECONDS = 31536000; // 1 year
    private static final long CORS_MAX_AGE_SECONDS = 3600L; // 1 hour
    private static final int BCRYPT_STRENGTH = 12;

    private final OpaqueTokenAuthenticationFilter tokenFilter;

    /**
     * Constructor with dependency injection.
     *
     * @param tokenFilter the opaque token authentication filter
     */
    public SecurityConfig(final OpaqueTokenAuthenticationFilter tokenFilter) {
        this.tokenFilter = tokenFilter;
    }

    /**
     * Configures the security filter chain.
     *
     * @param http the HttpSecurity to configure
     * @return the configured security filter chain
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain filterChain(final HttpSecurity http) throws Exception {
        http
                // CSRF protection with cookie-based tokens
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                )

                // CORS configuration
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Stateless session management (tokens in Redis)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Authorization rules
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/api/v1/auth/login", "/api/v1/auth/register").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        // All other requests require authentication
                        .anyRequest().authenticated()
                )

                // Add custom opaque token filter
                .addFilterBefore(tokenFilter, UsernamePasswordAuthenticationFilter.class)

                // Security headers
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives(
                                        "default-src 'self'; "
                                                + "script-src 'self' 'unsafe-inline'; "
                                                + "style-src 'self' 'unsafe-inline'; "
                                                + "img-src 'self' data: https:; "
                                                + "font-src 'self' data:; "
                                                + "connect-src 'self'"
                                )
                        )
                        .frameOptions(frame -> frame.deny())
                        .xssProtection(xss -> xss.disable()) // XSS protection deprecated, use CSP instead
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(HSTS_MAX_AGE_SECONDS)
                        )
                );

        return http.build();
    }

    /**
     * Configures CORS to allow frontend access.
     *
     * @return the CORS configuration source
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        final CorsConfiguration configuration = new CorsConfiguration();

        // Allow frontend origin (configure via properties in production)
        configuration.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://localhost:5173"
        ));

        // Allow common HTTP methods
        configuration.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));

        // Allow all headers
        configuration.setAllowedHeaders(List.of("*"));

        // Allow credentials (cookies)
        configuration.setAllowCredentials(true);

        // Cache preflight requests for 1 hour
        configuration.setMaxAge(CORS_MAX_AGE_SECONDS);

        final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Configures password encoder using BCrypt with strength 12.
     *
     * @return the password encoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(BCRYPT_STRENGTH);
    }
}
