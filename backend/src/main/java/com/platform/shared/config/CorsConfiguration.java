package com.platform.shared.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * CORS configuration that provides secure, environment-specific settings.
 * Prevents wildcard origins in production while allowing flexible development.
 */
@Configuration
public class CorsConfiguration {

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${app.allowed-origins:}")
    private String[] allowedOrigins;

    @Value("${app.cors.allow-credentials:true}")
    private boolean allowCredentials;

    /**
     * Development CORS configuration - more permissive for local development
     */
    @Bean
    @Profile({"development", "test"})
    public CorsConfigurationSource developmentCorsConfigurationSource() {
        org.springframework.web.cors.CorsConfiguration configuration =
            new org.springframework.web.cors.CorsConfiguration();

        // Allow localhost origins for development
        configuration.setAllowedOriginPatterns(
            List.of("http://localhost:*", "https://localhost:*", frontendUrl));

        configuration.setAllowedMethods(
            List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(
            List.of("Authorization", "Content-Type", "X-Correlation-Id"));
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    /**
     * Production CORS configuration - strict security settings
     */
    @Bean
    @Profile("production")
    public CorsConfigurationSource productionCorsConfigurationSource() {
        org.springframework.web.cors.CorsConfiguration configuration =
            new org.springframework.web.cors.CorsConfiguration();

        // Strict allowed origins - NO wildcards
        if (allowedOrigins != null && allowedOrigins.length > 0) {
            // Use explicitly configured origins
            configuration.setAllowedOrigins(Arrays.asList(allowedOrigins));
        } else {
            // Default to frontend URL only
            configuration.setAllowedOrigins(List.of(frontendUrl));
        }

        // Restrictive method allowlist
        configuration.setAllowedMethods(
            List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // Specific headers only - no wildcards
        configuration.setAllowedHeaders(
            List.of(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "X-CSRF-Token",
                "X-Correlation-Id",
                "Accept",
                "Accept-Language",
                "Accept-Encoding"
            ));

        configuration.setAllowCredentials(allowCredentials);

        // Limited exposed headers
        configuration.setExposedHeaders(
            List.of("Authorization", "X-Correlation-Id", "Content-Type"));

        // Cache preflight for 1 hour
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    /**
     * Staging CORS configuration - balanced security for testing
     */
    @Bean
    @Profile("staging")
    public CorsConfigurationSource stagingCorsConfigurationSource() {
        org.springframework.web.cors.CorsConfiguration configuration =
            new org.springframework.web.cors.CorsConfiguration();

        // Allow staging environments
        configuration.setAllowedOrigins(
            List.of(
                frontendUrl,
                "https://staging.payment-platform.com",
                "https://test.payment-platform.com"
            ));

        configuration.setAllowedMethods(
            List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        configuration.setAllowedHeaders(
            List.of(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "X-CSRF-Token",
                "X-Correlation-Id"
            ));

        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(
            List.of("Authorization", "X-Correlation-Id"));
        configuration.setMaxAge(1800L); // 30 minutes

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}