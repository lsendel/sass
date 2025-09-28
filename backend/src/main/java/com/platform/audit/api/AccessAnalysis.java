package com.platform.audit.api;

/**
 * Represents access pattern analysis results for forensic reporting.
 * This is a placeholder implementation for the audit log viewer feature.
 */
public class AccessAnalysis {
    private int totalAccesses;
    private int uniqueResources;
    private int unauthorizedAttempts;
    private double accessVariance;

    public AccessAnalysis() {}

    public AccessAnalysis(int totalAccesses, int uniqueResources, int unauthorizedAttempts, double accessVariance) {
        this.totalAccesses = totalAccesses;
        this.uniqueResources = uniqueResources;
        this.unauthorizedAttempts = unauthorizedAttempts;
        this.accessVariance = accessVariance;
    }

    public int getTotalAccesses() {
        return totalAccesses;
    }

    public void setTotalAccesses(int totalAccesses) {
        this.totalAccesses = totalAccesses;
    }

    public int getUniqueResources() {
        return uniqueResources;
    }

    public void setUniqueResources(int uniqueResources) {
        this.uniqueResources = uniqueResources;
    }

    public int getUnauthorizedAttempts() {
        return unauthorizedAttempts;
    }

    public void setUnauthorizedAttempts(int unauthorizedAttempts) {
        this.unauthorizedAttempts = unauthorizedAttempts;
    }

    public double getAccessVariance() {
        return accessVariance;
    }

    public void setAccessVariance(double accessVariance) {
        this.accessVariance = accessVariance;
    }
}