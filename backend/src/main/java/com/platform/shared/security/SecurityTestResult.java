package com.platform.shared.security;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Result object for individual security test execution.
 */
public class SecurityTestResult {

    private final String testType;
    private final String description;
    private final Instant startTime;
    private final Instant endTime;
    private final TestStatus status;
    private final List<SecurityIssue> issues;
    private final String error;

    private SecurityTestResult(Builder builder) {
        this.testType = builder.testType;
        this.description = builder.description;
        this.startTime = builder.startTime;
        this.endTime = builder.endTime;
        this.status = builder.status;
        this.issues = builder.issues;
        this.error = builder.error;
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters
    public String getTestType() { return testType; }
    public String getDescription() { return description; }
    public Instant getStartTime() { return startTime; }
    public Instant getEndTime() { return endTime; }
    public TestStatus getStatus() { return status; }
    public List<SecurityIssue> getIssues() { return issues; }
    public String getError() { return error; }

    public static class Builder {
        private String testType;
        private String description;
        private Instant startTime;
        private Instant endTime;
        private TestStatus status = TestStatus.SKIPPED;
        private List<SecurityIssue> issues = new ArrayList<>();
        private String error;

        public Builder testType(String testType) {
            this.testType = testType;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
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

        public Builder status(TestStatus status) {
            this.status = status;
            return this;
        }

        public Builder issues(List<SecurityIssue> issues) {
            this.issues = new ArrayList<>(issues);
            return this;
        }

        public Builder addIssue(SecurityIssue issue) {
            this.issues.add(issue);
            return this;
        }

        public Builder error(String error) {
            this.error = error;
            return this;
        }

        public SecurityTestResult build() {
            return new SecurityTestResult(this);
        }
    }
}