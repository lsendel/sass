package com.platform.shared.config;

import com.platform.auth.internal.OpaqueTokenStore;
import com.platform.shared.security.TenantContextFilter;
import com.platform.shared.security.CustomOAuth2UserService;
import com.platform.shared.security.OAuth2AuthenticationSuccessHandler;
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
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Security configuration for OAuth2 authentication with opaque token storage.
 * Implements constitutional requirement for opaque tokens (no JWT).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@Profile("!test")
public class SecurityConfig {

    @Value("${app.frontend-url}")
    private String frontendUrl;

    private final OpaqueTokenStore tokenStore;
    private final OAuth2UserService<OAuth2UserRequest, OAuth2User> customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler successHandler;
    private final AuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository;

    public SecurityConfig(OpaqueTokenStore tokenStore,
                         CustomOAuth2UserService customOAuth2UserService,
                         OAuth2AuthenticationSuccessHandler successHandler,
                         AuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository) {
        this.tokenStore = tokenStore;
        this.customOAuth2UserService = customOAuth2UserService;
        this.successHandler = successHandler;
        this.authorizationRequestRepository = authorizationRequestRepository;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            // Disable CSRF for API endpoints
            .csrf(AbstractHttpConfigurer::disable)

            // Configure CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // Configure session management
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Configure security headers
            .headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions.deny())
                .contentTypeOptions(contentTypeOptions -> {})
                .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                    .maxAgeInSeconds(31536000)
                    .includeSubDomains(true))
                .referrerPolicy(referrerPolicy ->
                    referrerPolicy.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)))

            // Configure authorization
            .authorizeHttpRequests(authz -> authz
                // Public OAuth2 endpoints
                .requestMatchers("/api/v1/auth/oauth2/providers").permitAll()
                .requestMatchers("/api/v1/auth/oauth2/authorize/**").permitAll()
                .requestMatchers("/api/v1/auth/oauth2/callback/**").permitAll()

                // Public API endpoints
                .requestMatchers("/api/v1/plans").permitAll()
                .requestMatchers("/api/v1/webhooks/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/actuator/info").permitAll()

                // Protected OAuth2 endpoints (require authentication)
                .requestMatchers("/api/v1/auth/oauth2/session").authenticated()
                .requestMatchers("/api/v1/auth/oauth2/logout").authenticated()

                // Protected API endpoints
                .requestMatchers("/api/v1/**").authenticated()

                // Admin endpoints
                .requestMatchers("/actuator/**").hasRole("ADMIN")

                // Default deny all
                .anyRequest().denyAll())

            // Configure OAuth2 login with PKCE support
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/api/v1/auth/oauth2/authorize")
                .authorizationEndpoint(authorization -> authorization
                    .baseUri("/api/v1/auth/oauth2/authorize")
                    .authorizationRequestRepository(authorizationRequestRepository))
                .redirectionEndpoint(redirection -> redirection
                    .baseUri("/api/v1/auth/oauth2/callback"))
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService))
                .successHandler(successHandler))

            // Add custom filters
            .addFilterBefore(tenantContextFilter(), UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(opaqueTokenAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)

            // Configure logout
            .logout(logout -> logout
                .logoutUrl("/api/v1/auth/oauth2/logout")
                .logoutSuccessUrl(frontendUrl)
                .deleteCookies("JSESSIONID")
                .clearAuthentication(true)
                .invalidateHttpSession(true))

            .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of(frontendUrl, "http://localhost:*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(List.of("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
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