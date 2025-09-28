package com.platform.audit.api;

import com.platform.audit.internal.ComprehensiveAuditService;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Represents a comprehensive forensic report for security investigations.
 *
 * <p>This class aggregates detailed audit data to provide insights into user activity, potential
 * security threats, and a calculated risk score. It is designed to be immutable and is constructed
 * using the {@link Builder}.
 */
public class ForensicReport {

  private String reportId;
  private String userId;
  private Instant fromDate;
  private Instant toDate;
  private String requestedBy;
  private Instant generatedAt;
  private int totalEvents;
  private Map<String, Long> eventTypeCounts;
  private List<ComprehensiveAuditService.SuspiciousPattern> suspiciousPatterns;
  private ComprehensiveAuditService.LoginAnalysis loginAnalysis;
  private ComprehensiveAuditService.AccessAnalysis accessAnalysis;
  private List<ComprehensiveAuditService.TimelineEvent> timeline;
  private double riskScore;
  private ForensicSummary summary;

  private ForensicReport(Builder builder) {
    this.reportId = builder.reportId;
    this.userId = builder.userId;
    this.fromDate = builder.fromDate;
    this.toDate = builder.toDate;
    this.requestedBy = builder.requestedBy;
    this.generatedAt = builder.generatedAt;
    this.totalEvents = builder.totalEvents;
    this.eventTypeCounts = builder.eventTypeCounts;
    this.suspiciousPatterns = builder.suspiciousPatterns;
    this.loginAnalysis = builder.loginAnalysis;
    this.accessAnalysis = builder.accessAnalysis;
    this.timeline = builder.timeline;
    this.riskScore = builder.riskScore;
    this.summary = builder.summary;
  }

  /**
   * Creates a new {@link Builder} for constructing a {@code ForensicReport}.
   *
   * @return a new {@link Builder} instance
   */
  public static Builder builder() {
    return new Builder();
  }

  // Getters
  public String getReportId() {
    return reportId;
  }

  public String getUserId() {
    return userId;
  }

  public Instant getFromDate() {
    return fromDate;
  }

  public Instant getToDate() {
    return toDate;
  }

  public String getRequestedBy() {
    return requestedBy;
  }

  public Instant getGeneratedAt() {
    return generatedAt;
  }

  public int getTotalEvents() {
    return totalEvents;
  }

  public Map<String, Long> getEventTypeCounts() {
    return eventTypeCounts;
  }

  public List<ComprehensiveAuditService.SuspiciousPattern> getSuspiciousPatterns() {
    return suspiciousPatterns;
  }

  public ComprehensiveAuditService.LoginAnalysis getLoginAnalysis() {
    return loginAnalysis;
  }

  public ComprehensiveAuditService.AccessAnalysis getAccessAnalysis() {
    return accessAnalysis;
  }

  public List<ComprehensiveAuditService.TimelineEvent> getTimeline() {
    return timeline;
  }

  public double getRiskScore() {
    return riskScore;
  }

  public ForensicSummary getSummary() {
    return summary;
  }

  /** Builder for creating instances of {@link ForensicReport}. */
  public static class Builder {
    private String reportId;
    private String userId;
    private Instant fromDate;
    private Instant toDate;
    private String requestedBy;
    private Instant generatedAt;
    private int totalEvents;
    private Map<String, Long> eventTypeCounts;
    private List<ComprehensiveAuditService.SuspiciousPattern> suspiciousPatterns;
    private ComprehensiveAuditService.LoginAnalysis loginAnalysis;
    private ComprehensiveAuditService.AccessAnalysis accessAnalysis;
    private List<ComprehensiveAuditService.TimelineEvent> timeline;
    private double riskScore;
    private ForensicSummary summary;

    public Builder reportId(String reportId) {
      this.reportId = reportId;
      return this;
    }

    public Builder userId(String userId) {
      this.userId = userId;
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

    public Builder totalEvents(int totalEvents) {
      this.totalEvents = totalEvents;
      return this;
    }

    public Builder eventTypeCounts(Map<String, Long> eventTypeCounts) {
      this.eventTypeCounts = eventTypeCounts;
      return this;
    }

    public Builder suspiciousPatterns(
        List<ComprehensiveAuditService.SuspiciousPattern> suspiciousPatterns) {
      this.suspiciousPatterns = suspiciousPatterns;
      return this;
    }

    public Builder loginAnalysis(ComprehensiveAuditService.LoginAnalysis loginAnalysis) {
      this.loginAnalysis = loginAnalysis;
      return this;
    }

    public Builder accessAnalysis(ComprehensiveAuditService.AccessAnalysis accessAnalysis) {
      this.accessAnalysis = accessAnalysis;
      return this;
    }

    public Builder timeline(List<ComprehensiveAuditService.TimelineEvent> timeline) {
      this.timeline = timeline;
      return this;
    }

    public Builder riskScore(double riskScore) {
      this.riskScore = riskScore;
      return this;
    }

    public Builder summary(ForensicSummary summary) {
      this.summary = summary;
      return this;
    }

    /**
     * Builds the {@link ForensicReport} instance.
     *
     * @return a new {@link ForensicReport}
     */
    public ForensicReport build() {
      return new ForensicReport(this);
    }
  }

  /** Provides a high-level summary of the forensic report's findings. */
  public static class ForensicSummary {
    private String riskLevel;
    private List<String> keyFindings;
    private List<String> recommendations;
    private boolean anomalousActivity;

    public ForensicSummary() {}

    public String getRiskLevel() {
      return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
      this.riskLevel = riskLevel;
    }

    public List<String> getKeyFindings() {
      return keyFindings;
    }

    public void setKeyFindings(List<String> keyFindings) {
      this.keyFindings = keyFindings;
    }

    public List<String> getRecommendations() {
      return recommendations;
    }

    public void setRecommendations(List<String> recommendations) {
      this.recommendations = recommendations;
    }

    public boolean isAnomalousActivity() {
      return anomalousActivity;
    }

    public void setAnomalousActivity(boolean anomalousActivity) {
      this.anomalousActivity = anomalousActivity;
    }
  }
}