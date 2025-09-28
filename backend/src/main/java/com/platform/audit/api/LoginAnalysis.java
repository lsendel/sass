package com.platform.audit.api;

/**
 * Represents login pattern analysis results for forensic reporting.
 * This is a placeholder implementation for the audit log viewer feature.
 */
public class LoginAnalysis {
    private int totalLogins;
    private int failedLogins;
    private int successfulLogins;
    private double failureRate;

    public LoginAnalysis() {}

    public LoginAnalysis(int totalLogins, int failedLogins, int successfulLogins, double failureRate) {
        this.totalLogins = totalLogins;
        this.failedLogins = failedLogins;
        this.successfulLogins = successfulLogins;
        this.failureRate = failureRate;
    }

    public int getTotalLogins() {
        return totalLogins;
    }

    public void setTotalLogins(int totalLogins) {
        this.totalLogins = totalLogins;
    }

    public int getFailedLogins() {
        return failedLogins;
    }

    public void setFailedLogins(int failedLogins) {
        this.failedLogins = failedLogins;
    }

    public int getSuccessfulLogins() {
        return successfulLogins;
    }

    public void setSuccessfulLogins(int successfulLogins) {
        this.successfulLogins = successfulLogins;
    }

    public double getFailureRate() {
        return failureRate;
    }

    public void setFailureRate(double failureRate) {
        this.failureRate = failureRate;
    }
}