package com.platform.auth.internal;

import com.platform.auth.User;
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
import java.util.Collections;
import java.util.Optional;

/**
 * Filter that extracts and validates opaque tokens from httpOnly cookies.
 * Sets Spring Security authentication context if token is valid.
 *
 * @since 1.0.0
 */
@Component
public final class OpaqueTokenAuthenticationFilter extends OncePerRequestFilter {

        private static final String AUTH_COOKIE_NAME = "auth_token";

        private final OpaqueTokenService tokenService;
        private final UserRepository userRepository;

        /**
         * Constructor with dependency injection.
         */
        public OpaqueTokenAuthenticationFilter(
                final OpaqueTokenService tokenService,
                final UserRepository userRepository) {
            this.tokenService = tokenService;
            this.userRepository = userRepository;
        }

        @Override
        protected void doFilterInternal(
                final HttpServletRequest request,
                final HttpServletResponse response,
                final FilterChain filterChain) throws ServletException, IOException {

            extractToken(request).ifPresent(token ->
                    tokenService.validateToken(token).ifPresent(userId ->
                            userRepository.findById(userId)
                                    .filter(User::isActive)
                                    .ifPresent(user -> {
                                            final UsernamePasswordAuthenticationToken authentication =
                                                    new UsernamePasswordAuthenticationToken(
                                                            user,
                                                            null,
                                                            Collections.emptyList() // Authorities will be added later
                                                    );
                                            SecurityContextHolder.getContext().setAuthentication(authentication);
                                    })
                    )
            );

            filterChain.doFilter(request, response);
        }

    /**
     * Extracts the authentication token from the request cookies.
     *
     * @param request the HTTP request
     * @return optional containing the token if found
     */
    private Optional<String> extractToken(final HttpServletRequest request) {
        final Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }

        return Arrays.stream(cookies)
            .filter(cookie -> AUTH_COOKIE_NAME.equals(cookie.getName()))
            .map(Cookie::getValue)
            .filter(value -> value != null && !value.isEmpty())
            .findFirst();
    }
}
