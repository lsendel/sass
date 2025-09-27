package com.platform.shared.security;

/**
 * Represents a security issue found during testing.
 */
public class SecurityIssue {

    private final String id;
    private final String title;
    private final String description;
    private final Severity severity;
    private final String category;
    private final String location;
    private final String recommendation;
    private final String cweId;
    private final String owaspCategory;

    private SecurityIssue(Builder builder) {
        this.id = builder.id;
        this.title = builder.title;
        this.description = builder.description;
        this.severity = builder.severity;
        this.category = builder.category;
        this.location = builder.location;
        this.recommendation = builder.recommendation;
        this.cweId = builder.cweId;
        this.owaspCategory = builder.owaspCategory;
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Severity getSeverity() { return severity; }
    public String getCategory() { return category; }
    public String getLocation() { return location; }
    public String getRecommendation() { return recommendation; }
    public String getCweId() { return cweId; }
    public String getOwaspCategory() { return owaspCategory; }

    public static class Builder {
        private String id;
        private String title;
        private String description;
        private Severity severity = Severity.LOW;
        private String category;
        private String location;
        private String recommendation;
        private String cweId;
        private String owaspCategory;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder severity(Severity severity) {
            this.severity = severity;
            return this;
        }

        public Builder category(String category) {
            this.category = category;
            return this;
        }

        public Builder location(String location) {
            this.location = location;
            return this;
        }

        public Builder recommendation(String recommendation) {
            this.recommendation = recommendation;
            return this;
        }

        public Builder cweId(String cweId) {
            this.cweId = cweId;
            return this;
        }

        public Builder owaspCategory(String owaspCategory) {
            this.owaspCategory = owaspCategory;
            return this;
        }

        public SecurityIssue build() {
            return new SecurityIssue(this);
        }
    }
}