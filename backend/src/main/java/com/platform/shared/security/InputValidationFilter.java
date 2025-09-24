package com.platform.shared.security;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * Input validation filter that sanitizes all incoming request parameters,
 * headers, and request body to prevent injection attacks and XSS.
 */
@Component
public class InputValidationFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(InputValidationFilter.class);

    private final InputSanitizer inputSanitizer;

    public InputValidationFilter(InputSanitizer inputSanitizer) {
        this.inputSanitizer = inputSanitizer;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        try {
            // Skip validation for certain endpoints (e.g., webhooks with signatures)
            if (shouldSkipValidation(httpRequest)) {
                chain.doFilter(request, response);
                return;
            }

            // Wrap the request to provide sanitized parameters and body
            SanitizedHttpServletRequestWrapper wrappedRequest =
                new SanitizedHttpServletRequestWrapper(httpRequest, inputSanitizer);

            // Continue with the filter chain
            chain.doFilter(wrappedRequest, response);

        } catch (SecurityException e) {
            logger.warn("Security threat detected in request: {} from IP: {}",
                e.getMessage(), getClientIpAddress(httpRequest));

            sendSecurityErrorResponse(httpResponse, e.getMessage());
        } catch (AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            Throwable cause = unwrap(e);
            if (cause instanceof AccessDeniedException accessDeniedException) {
                throw accessDeniedException;
            }
            if (cause instanceof SecurityException securityException) {
                throw securityException;
            }

            logger.error("Error in input validation filter", e);
            sendSecurityErrorResponse(httpResponse, "Request validation failed");
        }
    }

    private Throwable unwrap(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current != current.getCause()) {
            current = current.getCause();
        }
        return current;
    }

    private boolean shouldSkipValidation(HttpServletRequest request) {
        String uri = request.getRequestURI();

        // Skip validation for webhook endpoints (they have signature validation)
        if (uri.startsWith("/api/v1/webhooks/")) {
            return true;
        }

        // Skip for health check endpoints
        if (uri.startsWith("/actuator/health")) {
            return true;
        }

        return false;
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

    private void sendSecurityErrorResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String jsonResponse = String.format("""
            {
                "error": "INVALID_REQUEST",
                "message": "%s"
            }
            """, message);

        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
    }

    /**
     * Custom HttpServletRequestWrapper that sanitizes request parameters and body.
     */
    private static class SanitizedHttpServletRequestWrapper extends HttpServletRequestWrapper {

        private final InputSanitizer inputSanitizer;
        private final Map<String, String[]> sanitizedParameters;
        private byte[] sanitizedBody;

        public SanitizedHttpServletRequestWrapper(HttpServletRequest request, InputSanitizer inputSanitizer)
                throws IOException {
            super(request);
            this.inputSanitizer = inputSanitizer;
            this.sanitizedParameters = sanitizeParameters(request);
            this.sanitizedBody = sanitizeRequestBody(request);
        }

        @Override
        public String getParameter(String name) {
            String[] values = sanitizedParameters.get(name);
            return values != null && values.length > 0 ? values[0] : null;
        }

        @Override
        public String[] getParameterValues(String name) {
            return sanitizedParameters.get(name);
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            return new HashMap<>(sanitizedParameters);
        }

        @Override
        public Enumeration<String> getParameterNames() {
            return java.util.Collections.enumeration(sanitizedParameters.keySet());
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            if (sanitizedBody == null) {
                return super.getInputStream();
            }

            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(sanitizedBody);
            return new ServletInputStream() {
                @Override
                public int read() throws IOException {
                    return byteArrayInputStream.read();
                }

                @Override
                public boolean isFinished() {
                    return byteArrayInputStream.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                    // Not implemented for this use case
                }
            };
        }

        @Override
        public BufferedReader getReader() throws IOException {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }

        private Map<String, String[]> sanitizeParameters(HttpServletRequest request) {
            Map<String, String[]> sanitized = new HashMap<>();
            Map<String, String[]> originalParams = request.getParameterMap();

            for (Map.Entry<String, String[]> entry : originalParams.entrySet()) {
                String paramName = entry.getKey();
                String[] paramValues = entry.getValue();

                if (paramValues != null) {
                    String[] sanitizedValues = new String[paramValues.length];
                    for (int i = 0; i < paramValues.length; i++) {
                        sanitizedValues[i] = sanitizeParameterValue(paramName, paramValues[i]);
                    }
                    sanitized.put(paramName, sanitizedValues);
                }
            }

            return sanitized;
        }

        private String sanitizeParameterValue(String paramName, String value) {
            if (value == null) {
                return null;
            }

            // Check for security threats first
            if (inputSanitizer.containsSecurityThreats(value)) {
                throw new SecurityException("Malicious content detected in parameter: " + paramName);
            }

            // Apply appropriate sanitization based on parameter name/context
            if (paramName.toLowerCase().contains("email")) {
                return inputSanitizer.sanitizeEmail(value);
            } else if (paramName.toLowerCase().contains("amount") ||
                       paramName.toLowerCase().contains("price")) {
                return inputSanitizer.sanitizeAmount(value);
            } else if (paramName.toLowerCase().contains("search") ||
                       paramName.toLowerCase().contains("query")) {
                return inputSanitizer.sanitizeSearchInput(value);
            } else if (paramName.toLowerCase().contains("name") ||
                       paramName.toLowerCase().contains("title")) {
                return inputSanitizer.sanitizeDisplayName(value);
            } else if (paramName.toLowerCase().contains("url") ||
                       paramName.toLowerCase().contains("link")) {
                return inputSanitizer.sanitizeUrl(value);
            } else if (paramName.toLowerCase().contains("token") ||
                       paramName.toLowerCase().contains("key")) {
                return inputSanitizer.sanitizeToken(value);
            } else {
                // Default sanitization for other parameters
                return inputSanitizer.sanitizeUserInput(value);
            }
        }

        private byte[] sanitizeRequestBody(HttpServletRequest request) throws IOException {
            String contentType = request.getContentType();

            // Only sanitize JSON and form-encoded content
            if (contentType == null ||
                (!contentType.contains("application/json") &&
                 !contentType.contains("application/x-www-form-urlencoded"))) {
                return null;
            }

            // Read the original body
            StringBuilder body = new StringBuilder();
            try (BufferedReader reader = request.getReader()) {
                String line;
                while ((line = reader.readLine()) != null) {
                    body.append(line);
                }
            }

            String originalBody = body.toString();
            if (originalBody.isEmpty()) {
                return null;
            }

            // Check for security threats in the body
            if (inputSanitizer.containsSecurityThreats(originalBody)) {
                throw new SecurityException("Malicious content detected in request body");
            }

            // For JSON content, perform basic sanitization
            if (contentType.contains("application/json")) {
                String sanitizedBody = inputSanitizer.sanitizeJsonValue(originalBody);
                return sanitizedBody.getBytes(StandardCharsets.UTF_8);
            }

            // For form data, return as-is since parameters are sanitized separately
            return originalBody.getBytes(StandardCharsets.UTF_8);
        }
    }
}
