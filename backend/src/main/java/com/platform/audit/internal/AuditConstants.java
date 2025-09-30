package com.platform.audit.internal;

/**
 * Constants used throughout the audit logging system.
 */
public final class AuditConstants {

    // Logging prefixes
    public static final String LOG_SECURITY_VIOLATION_ACCESS = "Security violation - audit log access by user: {}";

    // Error codes
    public static final String ERROR_ACCESS_DENIED = "ACCESS_DENIED";
    public static final String ERROR_INTERNAL_ERROR = "INTERNAL_ERROR";
    public static final String ERROR_USER_NOT_AUTHENTICATED = "USER_NOT_AUTHENTICATED";
    public static final String ERROR_RATE_LIMIT_EXCEEDED = "RATE_LIMIT_EXCEEDED";

    // Error messages
    public static final String MSG_ACCESS_DENIED_LOGS = "Access denied to audit logs";
    public static final String MSG_UNABLE_RETRIEVE_LOGS = "Unable to retrieve audit logs";
    public static final String MSG_USER_AUTH_REQUIRED = "User authentication required";

    // Validation messages
    public static final String INVALID_DATE_RANGE_MESSAGE = "Date from must be before date to";
    public static final String SEARCH_TEXT_TOO_LONG_MESSAGE = "Search text exceeds maximum length";

    // Limits
    public static final int MAX_SEARCH_TEXT_LENGTH = 255;
    public static final int MAX_PAGE_SIZE = 1000;

    // Error codes
    public static final String ERROR_INVALID_PAGE_SIZE = "INVALID_PAGE_SIZE";
    public static final String ERROR_INVALID_DATE_FORMAT = "INVALID_DATE_FORMAT";
    public static final String ERROR_INVALID_DATE_RANGE = "INVALID_DATE_RANGE";
    public static final String ERROR_INVALID_FORMAT = "INVALID_FORMAT";

    // Error messages
    public static final String MSG_PAGE_SIZE_EXCEEDED = "Page size exceeds maximum allowed";
    public static final String MSG_INVALID_DATE_FROM = "Invalid dateFrom format";
    public static final String MSG_INVALID_DATE_TO = "Invalid dateTo format";
    public static final String MSG_INVALID_DATE_RANGE = "DateFrom must be before dateTo";

    private AuditConstants() {
        // Utility class
    }
}
