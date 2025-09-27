package com.platform.shared.security;

/**
 * Request object for manually created security incidents
 */
public class ManualIncidentRequest {

    private String title;
    private String description;
    private IncidentSeverity severity;
    private String assignedTo;
    private String createdBy;
    private String category;
    private String affectedSystem;

    public ManualIncidentRequest() {}

    // Getters and setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public IncidentSeverity getSeverity() { return severity; }
    public void setSeverity(IncidentSeverity severity) { this.severity = severity; }

    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getAffectedSystem() { return affectedSystem; }
    public void setAffectedSystem(String affectedSystem) { this.affectedSystem = affectedSystem; }
}