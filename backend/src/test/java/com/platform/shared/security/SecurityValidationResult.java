package com.platform.shared.security;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Result object for security validation tests.
 * Contains comprehensive information about security check results.
 */
public class SecurityValidationResult {

    private final String category;
    private final String description;
    private final boolean secure;
    private final SecurityLevel securityLevel;
    private final List<String> vulnerabilities;
    private final Map<String, Boolean> checks;
    private final Map<String, Object> details;
    private final List<String> protectedVectors;
    private final Map<String, Boolean> designPrinciples;
    private final double configurationScore;
    private final List<String> criticalVulnerabilities;
    private final List<String> highSeverityVulnerabilities;
    private final Map<String, Object> authenticationControls;
    private final List<String> integrityMechanisms;
    private final double loggingCoverage;
    private final List<String> monitoringCapabilities;
    private final List<String> ssrfProtections;
    private final double complianceScore;
    private final String complianceLevel;
    private final Map<String, Boolean> gdprControls;
    private final Map<String, String> attackResults;
    private final Map<String, String> securityHeaders;
    private final Map<String, Object> sessionControls;
    private final Map<String, Object> apiControls;

    private SecurityValidationResult(Builder builder) {
        this.category = builder.category;
        this.description = builder.description;
        this.secure = builder.secure;
        this.securityLevel = builder.securityLevel;
        this.vulnerabilities = builder.vulnerabilities;
        this.checks = builder.checks;
        this.details = builder.details;
        this.protectedVectors = builder.protectedVectors;
        this.designPrinciples = builder.designPrinciples;
        this.configurationScore = builder.configurationScore;
        this.criticalVulnerabilities = builder.criticalVulnerabilities;
        this.highSeverityVulnerabilities = builder.highSeverityVulnerabilities;
        this.authenticationControls = builder.authenticationControls;
        this.integrityMechanisms = builder.integrityMechanisms;
        this.loggingCoverage = builder.loggingCoverage;
        this.monitoringCapabilities = builder.monitoringCapabilities;
        this.ssrfProtections = builder.ssrfProtections;
        this.complianceScore = builder.complianceScore;
        this.complianceLevel = builder.complianceLevel;
        this.gdprControls = builder.gdprControls;
        this.attackResults = builder.attackResults;
        this.securityHeaders = builder.securityHeaders;
        this.sessionControls = builder.sessionControls;
        this.apiControls = builder.apiControls;
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public boolean isSecure() { return secure; }
    public SecurityLevel getSecurityLevel() { return securityLevel; }
    public List<String> getVulnerabilities() { return vulnerabilities; }
    public Map<String, Boolean> getChecks() { return checks; }
    public Map<String, Object> getDetails() { return details; }
    public List<String> getProtectedVectors() { return protectedVectors; }
    public Map<String, Boolean> getDesignPrinciples() { return designPrinciples; }
    public double getConfigurationScore() { return configurationScore; }
    public List<String> getCriticalVulnerabilities() { return criticalVulnerabilities; }
    public List<String> getHighSeverityVulnerabilities() { return highSeverityVulnerabilities; }
    public Map<String, Object> getAuthenticationControls() { return authenticationControls; }
    public List<String> getIntegrityMechanisms() { return integrityMechanisms; }
    public double getLoggingCoverage() { return loggingCoverage; }
    public List<String> getMonitoringCapabilities() { return monitoringCapabilities; }
    public List<String> getSSRFProtections() { return ssrfProtections; }
    public double getComplianceScore() { return complianceScore; }
    public String getComplianceLevel() { return complianceLevel; }
    public Map<String, Boolean> getGDPRControls() { return gdprControls; }
    public Map<String, String> getAttackResults() { return attackResults; }
    public Map<String, String> getSecurityHeaders() { return securityHeaders; }
    public Map<String, Object> getSessionControls() { return sessionControls; }
    public Map<String, Object> getAPIControls() { return apiControls; }

    public static class Builder {
        private String category;
        private String description;
        private boolean secure = false;
        private SecurityLevel securityLevel = SecurityLevel.LOW;
        private List<String> vulnerabilities = new ArrayList<>();
        private Map<String, Boolean> checks = new HashMap<>();
        private Map<String, Object> details = new HashMap<>();
        private List<String> protectedVectors = new ArrayList<>();
        private Map<String, Boolean> designPrinciples = new HashMap<>();
        private double configurationScore = 0.0;
        private List<String> criticalVulnerabilities = new ArrayList<>();
        private List<String> highSeverityVulnerabilities = new ArrayList<>();
        private Map<String, Object> authenticationControls = new HashMap<>();
        private List<String> integrityMechanisms = new ArrayList<>();
        private double loggingCoverage = 0.0;
        private List<String> monitoringCapabilities = new ArrayList<>();
        private List<String> ssrfProtections = new ArrayList<>();
        private double complianceScore = 0.0;
        private String complianceLevel = "UNKNOWN";
        private Map<String, Boolean> gdprControls = new HashMap<>();
        private Map<String, String> attackResults = new HashMap<>();
        private Map<String, String> securityHeaders = new HashMap<>();
        private Map<String, Object> sessionControls = new HashMap<>();
        private Map<String, Object> apiControls = new HashMap<>();

        public Builder category(String category) {
            this.category = category;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder secure(boolean secure) {
            this.secure = secure;
            return this;
        }

        public Builder securityLevel(SecurityLevel level) {
            this.securityLevel = level;
            return this;
        }

        public Builder addVulnerability(String vulnerability) {
            this.vulnerabilities.add(vulnerability);
            return this;
        }

        public Builder addCheck(String checkName, boolean passed) {
            this.checks.put(checkName, passed);
            return this;
        }

        public Builder addDetail(String key, Object value) {
            this.details.put(key, value);
            return this;
        }

        public Builder addProtectedVector(String vector) {
            this.protectedVectors.add(vector);
            return this;
        }

        public Builder addDesignPrinciple(String principle, boolean implemented) {
            this.designPrinciples.put(principle, implemented);
            return this;
        }

        public Builder configurationScore(double score) {
            this.configurationScore = score;
            return this;
        }

        public Builder criticalVulnerabilities(List<String> vulnerabilities) {
            this.criticalVulnerabilities = new ArrayList<>(vulnerabilities);
            return this;
        }

        public Builder highSeverityVulnerabilities(List<String> vulnerabilities) {
            this.highSeverityVulnerabilities = new ArrayList<>(vulnerabilities);
            return this;
        }

        public Builder addAuthControl(String control, Object value) {
            this.authenticationControls.put(control, value);
            return this;
        }

        public Builder addIntegrityMechanism(String mechanism) {
            this.integrityMechanisms.add(mechanism);
            return this;
        }

        public Builder loggingCoverage(double coverage) {
            this.loggingCoverage = coverage;
            return this;
        }

        public Builder addMonitoringCapability(String capability) {
            this.monitoringCapabilities.add(capability);
            return this;
        }

        public Builder addSSRFProtection(String protection) {
            this.ssrfProtections.add(protection);
            return this;
        }

        public Builder complianceScore(double score) {
            this.complianceScore = score;
            return this;
        }

        public Builder complianceLevel(String level) {
            this.complianceLevel = level;
            return this;
        }

        public Builder addGDPRControl(String control, boolean implemented) {
            this.gdprControls.put(control, implemented);
            return this;
        }

        public Builder attackResults(Map<String, String> results) {
            this.attackResults = new HashMap<>(results);
            return this;
        }

        public Builder securityHeaders(Map<String, String> headers) {
            this.securityHeaders = new HashMap<>(headers);
            return this;
        }

        public Builder sessionControls(Map<String, Object> controls) {
            this.sessionControls = new HashMap<>(controls);
            return this;
        }

        public Builder apiControls(Map<String, Object> controls) {
            this.apiControls = new HashMap<>(controls);
            return this;
        }

        public SecurityValidationResult build() {
            return new SecurityValidationResult(this);
        }
    }
}

enum SecurityLevel {
    LOW, MEDIUM, HIGH, CRITICAL
}