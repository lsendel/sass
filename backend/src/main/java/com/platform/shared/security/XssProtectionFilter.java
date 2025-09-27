package com.platform.shared.security;

import java.io.IOException;
import java.util.regex.Pattern;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * XSS Protection Filter that sanitizes request parameters and headers.
 * Prevents cross-site scripting attacks by detecting and blocking malicious scripts.
 */
@Component
@Order(1)
public class XssProtectionFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(XssProtectionFilter.class);

    @Autowired
    private SecurityEventLogger securityEventLogger;

    // XSS attack patterns
    private static final Pattern[] XSS_PATTERNS = {
        Pattern.compile("<script[^>]*>.*?</script>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
        Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE),
        Pattern.compile("vbscript:", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onload(.*?)=", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
        Pattern.compile("onerror(.*?)=", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
        Pattern.compile("onclick(.*?)=", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
        Pattern.compile("onmouseover(.*?)=", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
        Pattern.compile("onfocus(.*?)=", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
        Pattern.compile("eval\\((.*)\\)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
        Pattern.compile("expression\\((.*)\\)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
        Pattern.compile("<iframe[^>]*>.*?</iframe>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
        Pattern.compile("<object[^>]*>.*?</object>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
        Pattern.compile("<embed[^>]*>.*?</embed>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
        Pattern.compile("<meta[^>]*>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
        Pattern.compile("<link[^>]*>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL)
    };

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Skip XSS filtering for certain content types (like file uploads)
        if (shouldSkipFiltering(httpRequest)) {
            chain.doFilter(request, response);
            return;
        }

        // Wrap request to sanitize parameters
        XssProtectedHttpServletRequest wrappedRequest = new XssProtectedHttpServletRequest(httpRequest);

        // Check for XSS attempts
        if (detectXssAttempt(wrappedRequest)) {
            handleXssAttempt(httpRequest, httpResponse);
            return;
        }

        chain.doFilter(wrappedRequest, response);
    }

    private boolean shouldSkipFiltering(HttpServletRequest request) {
        String contentType = request.getContentType();
        String requestURI = request.getRequestURI();

        // Skip for file uploads
        if (contentType != null && contentType.toLowerCase().contains("multipart/form-data")) {
            return true;
        }

        // Skip for API documentation endpoints
        if (requestURI.startsWith("/swagger") || requestURI.startsWith("/api-docs")) {
            return true;
        }

        // Skip for actuator endpoints
        if (requestURI.startsWith("/actuator")) {
            return true;
        }

        return false;
    }

    private boolean detectXssAttempt(XssProtectedHttpServletRequest request) {
        // Check parameters
        for (String paramName : request.getParameterMap().keySet()) {
            String[] paramValues = request.getParameterValues(paramName);
            if (paramValues != null) {
                for (String paramValue : paramValues) {
                    if (containsXss(paramValue)) {
                        logger.warn("XSS attempt detected in parameter '{}': {}", paramName, paramValue);
                        return true;
                    }
                }
            }
        }

        // Check headers
        java.util.Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            if (headerValue != null && containsXss(headerValue)) {
                logger.warn("XSS attempt detected in header '{}': {}", headerName, headerValue);
                return true;
            }
        }

        return false;
    }

    private boolean containsXss(String input) {
        if (input == null || input.trim().isEmpty()) {
            return false;
        }

        for (Pattern pattern : XSS_PATTERNS) {
            if (pattern.matcher(input).find()) {
                return true;
            }
        }

        return false;
    }

    private void handleXssAttempt(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String clientIp = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        String requestURI = request.getRequestURI();

        // Log security event
        securityEventLogger.logSuspiciousActivity(
            null, // User ID may not be available at this point
            "XSS_ATTEMPT",
            clientIp,
            "XSS attempt on endpoint: " + requestURI
        );

        // Return 400 Bad Request
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String jsonResponse = """
            {
                "error": "Bad Request",
                "message": "Invalid input detected. Request contains potentially malicious content.",
                "timestamp": "%s"
            }
            """.formatted(java.time.Instant.now());

        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String[] headerNames = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP"
        };

        for (String headerName : headerNames) {
            String ip = request.getHeader(headerName);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }

        return request.getRemoteAddr();
    }

    /**
     * Wrapper class that sanitizes request parameters and headers.
     */
    private static class XssProtectedHttpServletRequest extends HttpServletRequestWrapper {

        public XssProtectedHttpServletRequest(HttpServletRequest request) {
            super(request);
        }

        @Override
        public String getParameter(String name) {
            String value = super.getParameter(name);
            return sanitizeInput(value);
        }

        @Override
        public String[] getParameterValues(String name) {
            String[] values = super.getParameterValues(name);
            if (values == null) {
                return null;
            }

            String[] sanitizedValues = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                sanitizedValues[i] = sanitizeInput(values[i]);
            }
            return sanitizedValues;
        }

        @Override
        public String getHeader(String name) {
            String value = super.getHeader(name);
            // Only sanitize specific headers, not all (to avoid breaking authentication headers)
            if (name != null && (name.toLowerCase().contains("user") || name.toLowerCase().contains("referer"))) {
                return sanitizeInput(value);
            }
            return value;
        }

        private String sanitizeInput(String input) {
            if (input == null) {
                return null;
            }

            String sanitized = input;

            // Remove script tags
            sanitized = sanitized.replaceAll("(?i)<script[^>]*>.*?</script>", "");

            // Remove javascript and vbscript protocols
            sanitized = sanitized.replaceAll("(?i)javascript:", "");
            sanitized = sanitized.replaceAll("(?i)vbscript:", "");

            // Remove event handlers
            sanitized = sanitized.replaceAll("(?i)on\\w+\\s*=", "");

            // Remove eval and expression
            sanitized = sanitized.replaceAll("(?i)eval\\s*\\(", "");
            sanitized = sanitized.replaceAll("(?i)expression\\s*\\(", "");

            // Encode remaining potentially dangerous characters
            sanitized = sanitized.replace("<", "&lt;");
            sanitized = sanitized.replace(">", "&gt;");
            sanitized = sanitized.replace("\"", "&quot;");
            sanitized = sanitized.replace("'", "&#x27;");
            sanitized = sanitized.replace("/", "&#x2F;");

            return sanitized;
        }
    }
}