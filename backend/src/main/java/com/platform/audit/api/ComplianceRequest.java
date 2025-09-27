package com.platform.audit.api;

import java.time.Instant;

/**
 * Request object for generating compliance reports
 */
public class ComplianceRequest {

    private ComplianceRegulation regulation;
    private Instant fromDate;
    private Instant toDate;
    private String requestedBy;
    private String organizationId;
    private ComplianceScope scope;

    public ComplianceRequest() {}

    private ComplianceRequest(Builder builder) {
        this.regulation = builder.regulation;
        this.fromDate = builder.fromDate;
        this.toDate = builder.toDate;
        this.requestedBy = builder.requestedBy;
        this.organizationId = builder.organizationId;
        this.scope = builder.scope;
    }

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

        public ComplianceRequest build() {
            return new ComplianceRequest(this);
        }
    }

    public enum ComplianceRegulation {
        GDPR,       // General Data Protection Regulation
        PCI_DSS,    // Payment Card Industry Data Security Standard
        SOX,        // Sarbanes-Oxley Act
        HIPAA,      // Health Insurance Portability and Accountability Act
        ISO_27001,  // ISO/IEC 27001 Information Security Management
        NIST,       // NIST Cybersecurity Framework
        CCPA        // California Consumer Privacy Act
    }

    public enum ComplianceScope {
        BASIC,      // Basic compliance metrics
        STANDARD,   // Standard compliance report
        DETAILED,   // Detailed compliance analysis
        AUDIT_READY // Full audit-ready compliance report
    }
}