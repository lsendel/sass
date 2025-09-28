package com.platform.audit.api;

import java.time.Instant;

/**
 * Represents a request to generate a forensic report for a specific user or context.
 *
 * <p>This class encapsulates the parameters required for a forensic investigation,
 * such as the user ID, time frame, reason for the request, and the desired scope of analysis.
 * It is used as a Data Transfer Object (DTO) for initiating forensic analysis.
 * </p>
 */
public class ForensicRequest {

    private String userId;
    private Instant fromDate;
    private Instant toDate;
    private String requestedBy;
    private String reason;
    private ForensicScope scope;

    /**
     * Default constructor for frameworks like Jackson.
     */
    public ForensicRequest() {}

    private ForensicRequest(Builder builder) {
        this.userId = builder.userId;
        this.fromDate = builder.fromDate;
        this.toDate = builder.toDate;
        this.requestedBy = builder.requestedBy;
        this.reason = builder.reason;
        this.scope = builder.scope;
    }

    /**
     * Creates a new {@link Builder} for constructing a {@code ForensicRequest}.
     *
     * @return A new {@link Builder} instance.
     */
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

    /**
     * Builder for creating instances of {@link ForensicRequest}.
     */
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

        /**
         * Builds the {@link ForensicRequest} instance.
         * @return A new {@link ForensicRequest}.
         */
        public ForensicRequest build() {
            return new ForensicRequest(this);
        }
    }

    /**
     * Defines the scope and level of detail for a forensic analysis.
     */
    public enum ForensicScope {
        /** A basic summary of user activity. */
        BASIC,
        /** A standard forensic analysis of events and patterns. */
        STANDARD,
        /** A detailed analysis with event correlations. */
        DETAILED,
        /** A full forensic analysis including risk assessment and timeline reconstruction. */
        COMPREHENSIVE
    }
}