package com.platform.shared.security;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Result object for security testing pipeline execution.
 */
public class SecurityPipelineResult {

    private final String pipelineId;
    private final Instant startTime;
    private final Instant endTime;
    private final PipelineStatus status;
    private final double overallSecurityScore;
    private final Map<String, SecurityTestResult> testResults;
    private final String error;

    private SecurityPipelineResult(Builder builder) {
        this.pipelineId = builder.pipelineId;
        this.startTime = builder.startTime;
        this.endTime = builder.endTime;
        this.status = builder.status;
        this.overallSecurityScore = builder.overallSecurityScore;
        this.testResults = builder.testResults;
        this.error = builder.error;
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters
    public String getPipelineId() { return pipelineId; }
    public Instant getStartTime() { return startTime; }
    public Instant getEndTime() { return endTime; }
    public PipelineStatus getStatus() { return status; }
    public double getOverallSecurityScore() { return overallSecurityScore; }
    public Map<String, SecurityTestResult> getTestResults() { return testResults; }
    public String getError() { return error; }

    public static class Builder {
        private String pipelineId;
        private Instant startTime;
        private Instant endTime;
        private PipelineStatus status = PipelineStatus.IN_PROGRESS;
        private double overallSecurityScore = 0.0;
        private Map<String, SecurityTestResult> testResults = new HashMap<>();
        private String error;

        public Builder pipelineId(String pipelineId) {
            this.pipelineId = pipelineId;
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

        public Builder status(PipelineStatus status) {
            this.status = status;
            return this;
        }

        public Builder overallSecurityScore(double score) {
            this.overallSecurityScore = score;
            return this;
        }

        public Builder addTestResult(String testType, SecurityTestResult result) {
            this.testResults.put(testType, result);
            return this;
        }

        public Builder error(String error) {
            this.error = error;
            return this;
        }

        public Map<String, SecurityTestResult> getTestResults() {
            return testResults;
        }

        public PipelineStatus getStatus() {
            return status;
        }

        public SecurityPipelineResult build() {
            return new SecurityPipelineResult(this);
        }
    }
}