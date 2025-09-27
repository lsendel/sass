package com.platform.shared.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.session.web.http.CookieHttpSessionIdResolver;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.session.web.http.HttpSessionIdResolver;

/**
 * Secure session management configuration.
 * Implements session fixation protection, secure cookies, and proper timeouts.
 */
@Configuration
@EnableRedisHttpSession(
    maxInactiveIntervalInSeconds = 1800, // 30 minutes default
    cleanupCron = "0 */5 * * * *" // Cleanup every 5 minutes
)
public class SessionSecurityConfiguration {

    @Value("${server.servlet.session.timeout:PT30M}")
    private Duration sessionTimeout;

    @Value("${app.session.secure-cookie:true}")
    private boolean secureCookie;

    @Value("${app.session.same-site:Lax}")
    private String sameSite;

    @Value("${app.session.domain:}")
    private String cookieDomain;

    /**
     * Configure secure session cookies with proper security attributes.
     */
    @Bean
    public HttpSessionIdResolver httpSessionIdResolver() {
        CookieHttpSessionIdResolver resolver = new CookieHttpSessionIdResolver();

        DefaultCookieSerializer cookieSerializer = new DefaultCookieSerializer();

        // Security settings
        cookieSerializer.setHttpOnly(true); // Prevent XSS access to session cookie
        cookieSerializer.setSecure(secureCookie); // HTTPS only in production
        cookieSerializer.setSameSite(sameSite); // CSRF protection

        // Cookie naming and domain
        cookieSerializer.setCookieName("PLATFORM_SESSION");
        cookieSerializer.setCookiePath("/");

        if (cookieDomain != null && !cookieDomain.isEmpty()) {
            cookieSerializer.setDomainName(cookieDomain);
        }

        // Session timeout
        cookieSerializer.setCookieMaxAge((int) sessionTimeout.toSeconds());

        resolver.setCookieSerializer(cookieSerializer);
        return resolver;
    }

    /**
     * Session event listener for security monitoring.
     */
    @Bean
    public SessionEventListener sessionEventListener() {
        return new SessionEventListener();
    }

    /**
     * Session fixation protection and concurrent session control.
     */
    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistry();
    }
}