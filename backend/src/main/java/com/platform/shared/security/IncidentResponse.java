package com.platform.shared.security;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents an automated incident response plan and execution
 */
public class IncidentResponse {

    private final String responseId;
    private final String incidentId;
    private final Instant startTime;
    private final Instant endTime;
    private final IncidentSeverity severity;
    private final List<String> actions;
    private final boolean escalationRequired;
    private final Map<String, Object> actionResults;
    private final String status;

    private IncidentResponse(Builder builder) {
        this.responseId = builder.responseId;
        this.incidentId = builder.incidentId;
        this.startTime = builder.startTime;
        this.endTime = builder.endTime;
        this.severity = builder.severity;
        this.actions = builder.actions;
        this.escalationRequired = builder.escalationRequired;
        this.actionResults = builder.actionResults;
        this.status = builder.status;
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters
    public String getResponseId() { return responseId; }
    public String getIncidentId() { return incidentId; }
    public Instant getStartTime() { return startTime; }
    public Instant getEndTime() { return endTime; }
    public IncidentSeverity getSeverity() { return severity; }
    public List<String> getActions() { return actions; }
    public boolean isEscalationRequired() { return escalationRequired; }
    public Map<String, Object> getActionResults() { return actionResults; }
    public String getStatus() { return status; }

    public static class Builder {
        private String responseId;
        private String incidentId;
        private Instant startTime;
        private Instant endTime;
        private IncidentSeverity severity;
        private List<String> actions = new ArrayList<>();
        private boolean escalationRequired = false;
        private Map<String, Object> actionResults;
        private String status = "PENDING";

        public Builder responseId(String responseId) {
            this.responseId = responseId;
            return this;
        }

        public Builder incidentId(String incidentId) {
            this.incidentId = incidentId;
            return this;
        }

        public Builder startTime(Instant startTime) {
            this.startTime = startTime;
            return this;
        }

        public Builder endTime(Instant endTime) {
            this.endTime = endTime;
            return this;
        }

        public Builder severity(IncidentSeverity severity) {
            this.severity = severity;
            return this;
        }

        public Builder addAction(String action) {
            this.actions.add(action);
            return this;
        }

        public Builder actions(List<String> actions) {
            this.actions = new ArrayList<>(actions);
            return this;
        }

        public Builder escalationRequired(boolean escalationRequired) {
            this.escalationRequired = escalationRequired;
            return this;
        }

        public Builder actionResults(Map<String, Object> actionResults) {
            this.actionResults = actionResults;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public IncidentResponse build() {
            return new IncidentResponse(this);
        }
    }
}