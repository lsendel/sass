package com.platform.audit.api;

/**
 * Result of zero-trust security validation
 */
public class ZeroTrustValidationResult {
    private final boolean allowed;
    private final String reason;
    private final String riskLevel;

    public ZeroTrustValidationResult(boolean allowed, String reason, String riskLevel) {
        this.allowed = allowed;
        this.reason = reason;
        this.riskLevel = riskLevel;
    }

    public boolean isAllowed() {
        return allowed;
    }

    public String getReason() {
        return reason;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    /**
     * Get trust score for zero-trust validation
     */
    public double getTrustScore() {
        // Calculate trust score based on validation result
        return allowed ? 0.9 : 0.1;
    }

    public static ZeroTrustValidationResult denied(String reason) {
        return new ZeroTrustValidationResult(false, reason, "HIGH");
    }

    public static ZeroTrustValidationResult allowed() {
        return new ZeroTrustValidationResult(true, "VALIDATED", "LOW");
    }

    /**
     * Check if access is granted based on zero-trust validation
     */
    public boolean isAccessGranted() {
        return allowed;
    }

    /**
     * Get the reason for denial if access was not granted
     */
    public String getDenialReason() {
        return allowed ? null : reason;
    }
}