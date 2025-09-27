package com.platform.audit.api;

import java.time.Instant;
import java.util.List;

/**
 * Request object for querying audit events with advanced filtering
 */
public class AuditQueryRequest {

    private String userId;
    private List<String> eventTypes;
    private List<String> severities;
    private Instant fromDate;
    private Instant toDate;
    private String ipAddress;
    private String correlationId;
    private String requestedBy;
    private String criteria;

    public AuditQueryRequest() {}

    private AuditQueryRequest(Builder builder) {
        this.userId = builder.userId;
        this.eventTypes = builder.eventTypes;
        this.severities = builder.severities;
        this.fromDate = builder.fromDate;
        this.toDate = builder.toDate;
        this.ipAddress = builder.ipAddress;
        this.correlationId = builder.correlationId;
        this.requestedBy = builder.requestedBy;
        this.criteria = builder.criteria;
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters
    public String getUserId() { return userId; }
    public List<String> getEventTypes() { return eventTypes; }
    public List<String> getSeverities() { return severities; }
    public Instant getFromDate() { return fromDate; }
    public Instant getToDate() { return toDate; }
    public String getIpAddress() { return ipAddress; }
    public String getCorrelationId() { return correlationId; }
    public String getRequestedBy() { return requestedBy; }
    public String getCriteria() { return criteria; }

    // Setters
    public void setUserId(String userId) { this.userId = userId; }
    public void setEventTypes(List<String> eventTypes) { this.eventTypes = eventTypes; }
    public void setSeverities(List<String> severities) { this.severities = severities; }
    public void setFromDate(Instant fromDate) { this.fromDate = fromDate; }
    public void setToDate(Instant toDate) { this.toDate = toDate; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public void setRequestedBy(String requestedBy) { this.requestedBy = requestedBy; }
    public void setCriteria(String criteria) { this.criteria = criteria; }

    public static class Builder {
        private String userId;
        private List<String> eventTypes;
        private List<String> severities;
        private Instant fromDate;
        private Instant toDate;
        private String ipAddress;
        private String correlationId;
        private String requestedBy;
        private String criteria;

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder eventTypes(List<String> eventTypes) {
            this.eventTypes = eventTypes;
            return this;
        }

        public Builder severities(List<String> severities) {
            this.severities = severities;
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

        public Builder ipAddress(String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }

        public Builder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public Builder requestedBy(String requestedBy) {
            this.requestedBy = requestedBy;
            return this;
        }

        public Builder criteria(String criteria) {
            this.criteria = criteria;
            return this;
        }

        public AuditQueryRequest build() {
            return new AuditQueryRequest(this);
        }
    }
}