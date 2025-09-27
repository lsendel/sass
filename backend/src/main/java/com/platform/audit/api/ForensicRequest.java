package com.platform.audit.api;

import java.time.Instant;

/**
 * Request object for generating forensic reports
 */
public class ForensicRequest {

    private String userId;
    private Instant fromDate;
    private Instant toDate;
    private String requestedBy;
    private String reason;
    private ForensicScope scope;

    public ForensicRequest() {}

    private ForensicRequest(Builder builder) {
        this.userId = builder.userId;
        this.fromDate = builder.fromDate;
        this.toDate = builder.toDate;
        this.requestedBy = builder.requestedBy;
        this.reason = builder.reason;
        this.scope = builder.scope;
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters
    public String getUserId() { return userId; }
    public Instant getFromDate() { return fromDate; }
    public Instant getToDate() { return toDate; }
    public String getRequestedBy() { return requestedBy; }
    public String getReason() { return reason; }
    public ForensicScope getScope() { return scope; }

    // Setters
    public void setUserId(String userId) { this.userId = userId; }
    public void setFromDate(Instant fromDate) { this.fromDate = fromDate; }
    public void setToDate(Instant toDate) { this.toDate = toDate; }
    public void setRequestedBy(String requestedBy) { this.requestedBy = requestedBy; }
    public void setReason(String reason) { this.reason = reason; }
    public void setScope(ForensicScope scope) { this.scope = scope; }

    public static class Builder {
        private String userId;
        private Instant fromDate;
        private Instant toDate;
        private String requestedBy;
        private String reason;
        private ForensicScope scope = ForensicScope.STANDARD;

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder fromDate(Instant fromDate) {
            this.fromDate = fromDate;
            return this;
        }

        public Builder toDate(Instant toDate) {
            this.toDate = toDate;
            return this;
        }

        public Builder requestedBy(String requestedBy) {
            this.requestedBy = requestedBy;
            return this;
        }

        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }

        public Builder scope(ForensicScope scope) {
            this.scope = scope;
            return this;
        }

        public ForensicRequest build() {
            return new ForensicRequest(this);
        }
    }

    public enum ForensicScope {
        BASIC,      // Basic activity summary
        STANDARD,   // Standard forensic analysis
        DETAILED,   // Detailed forensic analysis with correlations
        COMPREHENSIVE  // Full forensic analysis with risk assessment
    }
}