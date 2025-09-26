package com.platform.shared.security;

import java.io.IOException;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

/**
 * Comprehensive input sanitization utility for preventing XSS, injection attacks,
 * and ensuring data integrity across the payment platform.
 */
@Component
public class InputSanitizer {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // Common XSS patterns
    private static final Pattern SCRIPT_PATTERN = Pattern.compile(
        "<script[^>]*>.*?</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final Pattern JAVASCRIPT_PATTERN = Pattern.compile(
        "javascript:", Pattern.CASE_INSENSITIVE
    );
    private static final Pattern ONLOAD_PATTERN = Pattern.compile(
        "on\\w+\\s*=", Pattern.CASE_INSENSITIVE
    );
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile(
        "<[^>]+>", Pattern.CASE_INSENSITIVE
    );

    // SQL injection patterns
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "(union|select|insert|update|delete|drop|create|alter|exec|execute)" +
        "\\s+(.*\\s+)?(from|into|values|table|database|schema)",
        Pattern.CASE_INSENSITIVE
    );

    // Path traversal patterns
    private static final Pattern PATH_TRAVERSAL_PATTERN = Pattern.compile(
        "\\.{2,}[\\/\\\\]|[\\/\\\\]\\.{2,}|\\.\\.%2f|%2e%2e%2f|%2e%2e/",
        Pattern.CASE_INSENSITIVE
    );

    // Common malicious patterns
    private static final Pattern LDAP_INJECTION_PATTERN = Pattern.compile(
        "[\\(\\)\\*\\\\\\|\\&]", Pattern.CASE_INSENSITIVE
    );

    /**
     * Sanitizes user input by removing dangerous content while preserving safe text.
     */
    public String sanitizeUserInput(String input) {
        if (input == null) {
            return null;
        }

        String sanitized = input.trim();

        // Remove script tags and their content
        sanitized = SCRIPT_PATTERN.matcher(sanitized).replaceAll("");

        // Remove javascript: protocol
        sanitized = JAVASCRIPT_PATTERN.matcher(sanitized).replaceAll("");

        // Remove event handlers like onload, onclick, etc.
        sanitized = ONLOAD_PATTERN.matcher(sanitized).replaceAll("");

        // Remove all HTML tags
        sanitized = HTML_TAG_PATTERN.matcher(sanitized).replaceAll("");

        // Encode remaining special characters
        sanitized = htmlEncode(sanitized);

        return sanitized;
    }

    /**
     * Sanitizes text that will be used in search or query contexts.
     */
    public String sanitizeSearchInput(String input) {
        if (input == null) {
            return null;
        }

        String sanitized = sanitizeUserInput(input);

        // Remove potential SQL injection patterns
        if (SQL_INJECTION_PATTERN.matcher(sanitized.toLowerCase()).find()) {
            throw new IllegalArgumentException("Invalid search input detected");
        }

        // Limit length for search terms
        if (sanitized.length() > 100) {
            sanitized = sanitized.substring(0, 100);
        }

        return sanitized;
    }

    /**
     * Sanitizes file names and paths to prevent path traversal attacks.
     */
    public String sanitizeFileName(String fileName) {
        if (fileName == null) {
            return null;
        }

        String sanitized = fileName.trim();

        // Check for path traversal attempts
        if (PATH_TRAVERSAL_PATTERN.matcher(sanitized).find()) {
            throw new IllegalArgumentException("Invalid file name: path traversal detected");
        }

        // Remove dangerous characters
        sanitized = sanitized.replaceAll("[<>:\"|?*\\\\]", "");

        // Remove leading/trailing dots and spaces
        sanitized = sanitized.replaceAll("^[.\\s]+|[.\\s]+$", "");

        // Limit length
        if (sanitized.length() > 255) {
            sanitized = sanitized.substring(0, 255);
        }

        if (sanitized.isEmpty()) {
            throw new IllegalArgumentException("Invalid file name: empty after sanitization");
        }

        return sanitized;
    }

    /**
     * Sanitizes email addresses ensuring they follow proper format.
     */
    public String sanitizeEmail(String email) {
        if (email == null) {
            return null;
        }

        String sanitized = email.trim().toLowerCase();

        // Basic email format validation
        if (!isValidEmail(sanitized)) {
            throw new IllegalArgumentException("Invalid email format");
        }

        // Additional sanitization
        sanitized = sanitized.replaceAll("[<>&\"']", "");

        return sanitized;
    }

    /**
     * Sanitizes URLs ensuring they use safe protocols.
     */
    public String sanitizeUrl(String url) {
        if (url == null) {
            return null;
        }

        String sanitized = url.trim();

        // Only allow http and https protocols
        if (!sanitized.matches("^https?://.*")) {
            throw new IllegalArgumentException("Only HTTP and HTTPS URLs are allowed");
        }

        // Remove javascript: and other dangerous protocols
        if (JAVASCRIPT_PATTERN.matcher(sanitized).find()) {
            throw new IllegalArgumentException("Dangerous URL protocol detected");
        }

        return sanitized;
    }

    /**
     * Sanitizes financial amounts ensuring they are valid numbers.
     */
    public String sanitizeAmount(String amount) {
        if (amount == null) {
            return null;
        }

        String sanitized = amount.trim();

        // Only allow digits, decimal point, and minus sign
        sanitized = sanitized.replaceAll("[^0-9.-]", "");

        // Validate format (simple decimal number)
        if (!sanitized.matches("^-?\\d+(\\.\\d{1,2})?$")) {
            throw new IllegalArgumentException("Invalid amount format");
        }

        return sanitized;
    }

    /**
     * Sanitizes organization/user names for display purposes.
     */
    public String sanitizeDisplayName(String name) {
        if (name == null) {
            return null;
        }

        String sanitized = name.trim();

        // Remove dangerous content but preserve basic formatting
        sanitized = SCRIPT_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = JAVASCRIPT_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = ONLOAD_PATTERN.matcher(sanitized).replaceAll("");

        // Allow basic alphanumeric, spaces, and common punctuation
        sanitized = sanitized.replaceAll("[^a-zA-Z0-9\\s\\-_.,']", "");

        // Limit length
        if (sanitized.length() > 100) {
            sanitized = sanitized.substring(0, 100);
        }

        return sanitized.trim();
    }

    /**
     * Validates and sanitizes session tokens and API keys.
     */
    public String sanitizeToken(String token) {
        if (token == null) {
            return null;
        }

        String sanitized = token.trim();

        // Tokens should only contain alphanumeric characters and specific symbols
        if (!sanitized.matches("^[a-zA-Z0-9_\\-\\.]+$")) {
            throw new IllegalArgumentException("Invalid token format");
        }

        // Reasonable length constraints
        if (sanitized.length() < 10 || sanitized.length() > 500) {
            throw new IllegalArgumentException("Invalid token length");
        }

        return sanitized;
    }

    /**
     * HTML encodes special characters to prevent XSS.
     */
    private String htmlEncode(String input) {
        if (input == null) {
            return null;
        }

        return input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;")
            .replace("/", "&#x2F;");
    }

    /**
     * Basic email validation using regex.
     */
    private boolean isValidEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        return Pattern.compile(emailRegex).matcher(email).matches();
    }

    /**
     * Checks if input contains potential security threats.
     */
    public boolean containsSecurityThreats(String input) {
        if (input == null) {
            return false;
        }

        String lowercaseInput = input.toLowerCase();

        return SCRIPT_PATTERN.matcher(input).find() ||
               JAVASCRIPT_PATTERN.matcher(lowercaseInput).find() ||
               SQL_INJECTION_PATTERN.matcher(lowercaseInput).find() ||
               PATH_TRAVERSAL_PATTERN.matcher(input).find() ||
               LDAP_INJECTION_PATTERN.matcher(input).find();
    }

    /**
     * Sanitizes JSON input by escaping dangerous characters.
     */
    public String sanitizeJsonValue(String jsonValue) {
        if (jsonValue == null || jsonValue.isBlank()) {
            return jsonValue;
        }

        try {
            JsonNode parsed = OBJECT_MAPPER.readTree(jsonValue);
            JsonNode sanitized = sanitizeJsonNode(parsed);
            return OBJECT_MAPPER.writeValueAsString(sanitized);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Invalid JSON payload", ex);
        }
    }

    private JsonNode sanitizeJsonNode(JsonNode node) {
        if (node == null) {
            return null;
        }

        if (node.isTextual()) {
            return TextNode.valueOf(sanitizeUserInput(node.asText()));
        }

        if (node.isObject()) {
            ObjectNode objectNode = OBJECT_MAPPER.createObjectNode();
            node.fields().forEachRemaining(entry -> objectNode.set(entry.getKey(), sanitizeJsonNode(entry.getValue())));
            return objectNode;
        }

        if (node.isArray()) {
            ArrayNode arrayNode = OBJECT_MAPPER.createArrayNode();
            node.forEach(child -> arrayNode.add(sanitizeJsonNode(child)));
            return arrayNode;
        }

        return node;
    }
}
