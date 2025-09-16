package com.platform.shared.security;

import com.platform.auth.internal.SessionService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Custom OAuth2 authentication success handler.
 */
@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2AuthenticationSuccessHandler.class);

    @Value("${app.frontend-url}")
    private String frontendUrl;

    private final SessionService sessionService;

    public OAuth2AuthenticationSuccessHandler(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                      HttpServletResponse response,
                                      Authentication authentication) throws IOException, ServletException {

        logger.info("OAuth2 authentication successful for user: {}",
                   authentication.getName());

        try {
            if (authentication.getPrincipal() instanceof OAuth2User oauth2User) {
                // Create session and generate token
                SessionService.AuthenticationResult result = sessionService.handleOAuth2Authentication(
                    oauth2User,
                    getClientIpAddress(request),
                    request.getHeader("User-Agent")
                );

                // Redirect to frontend with token
                String targetUrl = buildSuccessRedirectUrl(result.token());
                logger.info("Redirecting to: {}", targetUrl);
                response.sendRedirect(targetUrl);
            } else {
                // Fallback redirect without token
                response.sendRedirect(frontendUrl + "/auth/error?error=invalid_authentication");
            }
        } catch (Exception e) {
            logger.error("Error processing OAuth2 authentication success", e);
            response.sendRedirect(frontendUrl + "/auth/error?error=processing_failed");
        }
    }

    private String buildSuccessRedirectUrl(String token) {
        // In production, you might want to set the token in a secure HTTP-only cookie
        // For now, we'll pass it as a URL parameter (should be HTTPS only)
        return frontendUrl + "/auth/callback?token=" + token;
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