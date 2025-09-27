package com.platform.audit.api;

import java.time.Instant;
import java.util.Map;
import java.util.List;

/**
 * Comprehensive compliance report for regulatory requirements
 */
public class ComplianceReport {

    private String reportId;
    private ComplianceRequest.ComplianceRegulation regulation;
    private Instant fromDate;
    private Instant toDate;
    private String requestedBy;
    private Instant generatedAt;
    private ComplianceStatus overallStatus;
    private Map<String, ComplianceRequirement> requirements;
    private List<ComplianceViolation> violations;
    private ComplianceMetrics metrics;
    private List<String> recommendations;

    private ComplianceReport(Builder builder) {
        this.reportId = builder.reportId;
        this.regulation = builder.regulation;
        this.fromDate = builder.fromDate;
        this.toDate = builder.toDate;
        this.requestedBy = builder.requestedBy;
        this.generatedAt = builder.generatedAt;
        this.overallStatus = builder.overallStatus;
        this.requirements = builder.requirements;
        this.violations = builder.violations;
        this.metrics = builder.metrics;
        this.recommendations = builder.recommendations;
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters
    public String getReportId() { return reportId; }
    public ComplianceRequest.ComplianceRegulation getRegulation() { return regulation; }
    public Instant getFromDate() { return fromDate; }
    public Instant getToDate() { return toDate; }
    public String getRequestedBy() { return requestedBy; }
    public Instant getGeneratedAt() { return generatedAt; }
    public ComplianceStatus getOverallStatus() { return overallStatus; }
    public Map<String, ComplianceRequirement> getRequirements() { return requirements; }
    public List<ComplianceViolation> getViolations() { return violations; }
    public ComplianceMetrics getMetrics() { return metrics; }
    public List<String> getRecommendations() { return recommendations; }

    public static class Builder {
        private String reportId;
        private ComplianceRequest.ComplianceRegulation regulation;
        private Instant fromDate;
        private Instant toDate;
        private String requestedBy;
        private Instant generatedAt;
        private ComplianceStatus overallStatus;
        private Map<String, ComplianceRequirement> requirements;
        private List<ComplianceViolation> violations;
        private ComplianceMetrics metrics;
        private List<String> recommendations;

        public Builder reportId(String reportId) {
            this.reportId = reportId;
            return this;
        }

        public Builder regulation(ComplianceRequest.ComplianceRegulation regulation) {
            this.regulation = regulation;
            return this;
        }

        public Builder timeframe(Instant fromDate, Instant toDate) {
            this.fromDate = fromDate;
            this.toDate = toDate;
            return this;
        }

        public Builder requestedBy(String requestedBy) {
            this.requestedBy = requestedBy;
            return this;
        }

        public Builder generatedAt(Instant generatedAt) {
            this.generatedAt = generatedAt;
            return this;
        }

        public Builder overallStatus(ComplianceStatus overallStatus) {
            this.overallStatus = overallStatus;
            return this;
        }

        public Builder requirements(Map<String, ComplianceRequirement> requirements) {
            this.requirements = requirements;
            return this;
        }

        public Builder violations(List<ComplianceViolation> violations) {
            this.violations = violations;
            return this;
        }

        public Builder metrics(ComplianceMetrics metrics) {
            this.metrics = metrics;
            return this;
        }

        public Builder recommendations(List<String> recommendations) {
            this.recommendations = recommendations;
            return this;
        }

        public ComplianceReport build() {
            return new ComplianceReport(this);
        }
    }

    public enum ComplianceStatus {
        COMPLIANT,
        NON_COMPLIANT,
        PARTIALLY_COMPLIANT,
        UNDER_REVIEW
    }

    public static class ComplianceRequirement {
        private String requirementId;
        private String description;
        private ComplianceStatus status;
        private String evidence;
        private List<String> gaps;

        // Constructors, getters, and setters
        public ComplianceRequirement() {}

        public String getRequirementId() { return requirementId; }
        public void setRequirementId(String requirementId) { this.requirementId = requirementId; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public ComplianceStatus getStatus() { return status; }
        public void setStatus(ComplianceStatus status) { this.status = status; }

        public String getEvidence() { return evidence; }
        public void setEvidence(String evidence) { this.evidence = evidence; }

        public List<String> getGaps() { return gaps; }
        public void setGaps(List<String> gaps) { this.gaps = gaps; }
    }

    public static class ComplianceViolation {
        private String violationId;
        private String requirementId;
        private String description;
        private String severity;
        private Instant detectedAt;
        private String remediation;

        // Constructors, getters, and setters
        public ComplianceViolation() {}

        public String getViolationId() { return violationId; }
        public void setViolationId(String violationId) { this.violationId = violationId; }

        public String getRequirementId() { return requirementId; }
        public void setRequirementId(String requirementId) { this.requirementId = requirementId; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }

        public Instant getDetectedAt() { return detectedAt; }
        public void setDetectedAt(Instant detectedAt) { this.detectedAt = detectedAt; }

        public String getRemediation() { return remediation; }
        public void setRemediation(String remediation) { this.remediation = remediation; }
    }

    public static class ComplianceMetrics {
        private double complianceScore;
        private int totalRequirements;
        private int compliantRequirements;
        private int violationCount;
        private Map<String, Long> violationsBySeverity;

        // Constructors, getters, and setters
        public ComplianceMetrics() {}

        public double getComplianceScore() { return complianceScore; }
        public void setComplianceScore(double complianceScore) { this.complianceScore = complianceScore; }

        public int getTotalRequirements() { return totalRequirements; }
        public void setTotalRequirements(int totalRequirements) { this.totalRequirements = totalRequirements; }

        public int getCompliantRequirements() { return compliantRequirements; }
        public void setCompliantRequirements(int compliantRequirements) { this.compliantRequirements = compliantRequirements; }

        public int getViolationCount() { return violationCount; }
        public void setViolationCount(int violationCount) { this.violationCount = violationCount; }

        public Map<String, Long> getViolationsBySeverity() { return violationsBySeverity; }
        public void setViolationsBySeverity(Map<String, Long> violationsBySeverity) { this.violationsBySeverity = violationsBySeverity; }
    }
}