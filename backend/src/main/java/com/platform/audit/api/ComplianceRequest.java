package com.platform.audit.api;

import java.time.Instant;

/**
 * Represents a request to generate a compliance report.
 *
 * <p>This class encapsulates the parameters needed to generate a compliance report,
 * such as the specific regulation, the time frame, and the scope of the report.
 * It is designed to be used as a Data Transfer Object (DTO) for API endpoints.
 * </p>
 */
public class ComplianceRequest {

    private ComplianceRegulation regulation;
    private Instant fromDate;
    private Instant toDate;
    private String requestedBy;
    private String organizationId;
    private ComplianceScope scope;

    /**
     * Default constructor for frameworks like Jackson.
     */
    public ComplianceRequest() {}

    private ComplianceRequest(Builder builder) {
        this.regulation = builder.regulation;
        this.fromDate = builder.fromDate;
        this.toDate = builder.toDate;
        this.requestedBy = builder.requestedBy;
        this.organizationId = builder.organizationId;
        this.scope = builder.scope;
    }

    /**
     * Creates a new {@link Builder} for constructing a {@code ComplianceRequest}.
     *
     * @return A new {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    // Getters
    public ComplianceRegulation getRegulation() { return regulation; }
    public Instant getFromDate() { return fromDate; }
    public Instant getToDate() { return toDate; }
    public String getRequestedBy() { return requestedBy; }
    public String getOrganizationId() { return organizationId; }
    public ComplianceScope getScope() { return scope; }

    // Setters
    public void setRegulation(ComplianceRegulation regulation) { this.regulation = regulation; }
    public void setFromDate(Instant fromDate) { this.fromDate = fromDate; }
    public void setToDate(Instant toDate) { this.toDate = toDate; }
    public void setRequestedBy(String requestedBy) { this.requestedBy = requestedBy; }
    public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }
    public void setScope(ComplianceScope scope) { this.scope = scope; }

    /**
     * Builder for creating instances of {@link ComplianceRequest}.
     */
    public static class Builder {
        private ComplianceRegulation regulation;
        private Instant fromDate;
        private Instant toDate;
        private String requestedBy;
        private String organizationId;
        private ComplianceScope scope = ComplianceScope.STANDARD;

        public Builder regulation(ComplianceRegulation regulation) {
            this.regulation = regulation;
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

        public Builder organizationId(String organizationId) {
            this.organizationId = organizationId;
            return this;
        }

        public Builder scope(ComplianceScope scope) {
            this.scope = scope;
            return this;
        }

        /**
         * Builds the {@link ComplianceRequest} instance.
         * @return A new {@link ComplianceRequest}.
         */
        public ComplianceRequest build() {
            return new ComplianceRequest(this);
        }
    }

    /**
     * Enumerates common compliance regulations that can be reported on.
     */
    public enum ComplianceRegulation {
        /** General Data Protection Regulation */
        GDPR,
        /** Payment Card Industry Data Security Standard */
        PCI_DSS,
        /** Sarbanes-Oxley Act */
        SOX,
        /** Health Insurance Portability and Accountability Act */
        HIPAA,
        /** ISO/IEC 27001 Information Security Management */
        ISO_27001,
        /** NIST Cybersecurity Framework */
        NIST,
        /** California Consumer Privacy Act */
        CCPA
    }

    /**
     * Defines the scope and level of detail for a compliance report.
     */
    public enum ComplianceScope {
        /** Basic compliance metrics only. */
        BASIC,
        /** A standard, well-rounded compliance report. */
        STANDARD,
        /** A detailed compliance analysis with extensive evidence. */
        DETAILED,
        /** A comprehensive, audit-ready compliance report. */
        AUDIT_READY
    }
}