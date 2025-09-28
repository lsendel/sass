package com.platform.audit.api;

import java.time.Instant;
import java.util.List;

/**
 * Represents a request object for querying audit events with advanced filtering criteria.
 *
 * <p>This class encapsulates all possible parameters that can be used to filter and search
 * for audit events. It is designed to be used as a Data Transfer Object (DTO) in API endpoints.
 * An instance of this class can be constructed using the provided {@link Builder}.
 * </p>
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

    /**
     * Default constructor for frameworks like Jackson.
     */
    public AuditQueryRequest() {}

    /**
     * Constructs an {@code AuditQueryRequest} from a {@link Builder} instance.
     *
     * @param builder The builder to construct the object from.
     */
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

    /**
     * Creates a new {@link Builder} instance for constructing an {@code AuditQueryRequest}.
     *
     * @return A new {@link Builder} instance.
     */
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

    /**
     * Builder class for creating instances of {@link AuditQueryRequest}.
     *
     * <p>This class follows the builder pattern to allow for flexible and readable
     * construction of {@code AuditQueryRequest} objects.
     * </p>
     */
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

        /**
         * Sets the user ID to filter by.
         * @param userId The ID of the user.
         * @return This builder instance for chaining.
         */
        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        /**
         * Sets the list of event types to filter by.
         * @param eventTypes A list of event type strings.
         * @return This builder instance for chaining.
         */
        public Builder eventTypes(List<String> eventTypes) {
            this.eventTypes = eventTypes;
            return this;
        }

        /**
         * Sets the list of severities to filter by.
         * @param severities A list of severity strings.
         * @return This builder instance for chaining.
         */
        public Builder severities(List<String> severities) {
            this.severities = severities;
            return this;
        }

        /**
         * Sets the start of the date range for the query.
         * @param fromDate The start date and time.
         * @return This builder instance for chaining.
         */
        public Builder fromDate(Instant fromDate) {
            this.fromDate = fromDate;
            return this;
        }

        /**
         * Sets the end of the date range for the query.
         * @param toDate The end date and time.
         * @return This builder instance for chaining.
         */
        public Builder toDate(Instant toDate) {
            this.toDate = toDate;
            return this;
        }

        /**
         * Sets the IP address to filter by.
         * @param ipAddress The IP address.
         * @return This builder instance for chaining.
         */
        public Builder ipAddress(String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }

        /**
         * Sets the correlation ID to filter by.
         * @param correlationId The correlation ID.
         * @return This builder instance for chaining.
         */
        public Builder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        /**
         * Sets the identity of the requester.
         * @param requestedBy The identifier of the entity that requested the audit.
         * @return This builder instance for chaining.
         */
        public Builder requestedBy(String requestedBy) {
            this.requestedBy = requestedBy;
            return this;
        }

        /**
         * Sets a general-purpose criteria string for searching.
         * @param criteria The search criteria.
         * @return This builder instance for chaining.
         */
        public Builder criteria(String criteria) {
            this.criteria = criteria;
            return this;
        }

        /**
         * Builds and returns a new {@link AuditQueryRequest} instance.
         * @return A new {@link AuditQueryRequest} with the configured properties.
         */
        public AuditQueryRequest build() {
            return new AuditQueryRequest(this);
        }
    }
}