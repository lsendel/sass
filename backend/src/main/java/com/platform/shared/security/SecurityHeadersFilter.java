package com.platform.shared.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Security headers filter that adds comprehensive security headers to all HTTP responses.
 * This helps protect against various web vulnerabilities including XSS, clickjacking, and MIME sniffing.
 */
@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain filterChain)
                                  throws ServletException, IOException {

        // Content Security Policy - Restrictive policy for payment platform
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

        // Strict Transport Security (HSTS) - Force HTTPS
        response.setHeader("Strict-Transport-Security",
            "max-age=31536000; includeSubDomains; preload");

        // X-Frame-Options - Prevent clickjacking
        response.setHeader("X-Frame-Options", "DENY");

        // X-Content-Type-Options - Prevent MIME sniffing
        response.setHeader("X-Content-Type-Options", "nosniff");

        // X-XSS-Protection - Enable XSS protection (legacy but still useful)
        response.setHeader("X-XSS-Protection", "1; mode=block");

        // Referrer Policy - Control referrer information
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        // Permissions Policy - Control browser features
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