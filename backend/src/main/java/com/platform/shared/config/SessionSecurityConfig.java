package com.platform.shared.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.session.web.http.CookieHttpSessionIdResolver;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.session.web.http.HttpSessionIdResolver;

/**
 * Secure session configuration with Redis backend.
 */
@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 3600) // 1 hour
public class SessionSecurityConfig {

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Bean
    public HttpSessionIdResolver httpSessionIdResolver() {
        CookieHttpSessionIdResolver resolver = new CookieHttpSessionIdResolver();
        resolver.setCookieSerializer(cookieSerializer());
        return resolver;
    }

    @Bean
    public DefaultCookieSerializer cookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName("SESSIONID");
        serializer.setHttpOnly(true);
        serializer.setSecure(isProduction());
        serializer.setSameSite("Strict");
        serializer.setCookieMaxAge(3600); // 1 hour
        serializer.setUseBase64Encoding(false);
        return serializer;
    }

    private boolean isProduction() {
        return !frontendUrl.contains("localhost") && !frontendUrl.contains("127.0.0.1");
    }
}
