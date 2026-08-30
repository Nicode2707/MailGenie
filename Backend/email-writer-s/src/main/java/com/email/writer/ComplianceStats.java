package com.email.writer;

import java.util.Map;

public class ComplianceStats {
    private long totalComplianceEvents;
    private long pendingEvents;
    private long criticalEvents;
    private long overdueEvents;
    private long totalConsents;
    private long grantedConsents;
    private long withdrawnConsents;
    private long expiredConsents;
    private long totalAuditLogs;
    private long failedAuditLogs;
    private long deniedAccessAttempts;
    private long sensitiveDataExposures;
    private Map<String, Long> eventsByRegulation;
    private Map<String, Long> eventsByType;
    private Map<String, Long> consentsByType;
    private Map<String, Long> consentsByStatus;
    private Map<String, Long> auditActionsBreakdown;
    private Double averageAuditDurationMs;
    private double complianceScore;

    public ComplianceStats() {}

    public long getTotalComplianceEvents() { return totalComplianceEvents; }
    public void setTotalComplianceEvents(long totalComplianceEvents) { this.totalComplianceEvents = totalComplianceEvents; }
    public long getPendingEvents() { return pendingEvents; }
    public void setPendingEvents(long pendingEvents) { this.pendingEvents = pendingEvents; }
    public long getCriticalEvents() { return criticalEvents; }
    public void setCriticalEvents(long criticalEvents) { this.criticalEvents = criticalEvents; }
    public long getOverdueEvents() { return overdueEvents; }
    public void setOverdueEvents(long overdueEvents) { this.overdueEvents = overdueEvents; }
    public long getTotalConsents() { return totalConsents; }
    public void setTotalConsents(long totalConsents) { this.totalConsents = totalConsents; }
    public long getGrantedConsents() { return grantedConsents; }
    public void setGrantedConsents(long grantedConsents) { this.grantedConsents = grantedConsents; }
    public long getWithdrawnConsents() { return withdrawnConsents; }
    public void setWithdrawnConsents(long withdrawnConsents) { this.withdrawnConsents = withdrawnConsents; }
    public long getExpiredConsents() { return expiredConsents; }
    public void setExpiredConsents(long expiredConsents) { this.expiredConsents = expiredConsents; }
    public long getTotalAuditLogs() { return totalAuditLogs; }
    public void setTotalAuditLogs(long totalAuditLogs) { this.totalAuditLogs = totalAuditLogs; }
    public long getFailedAuditLogs() { return failedAuditLogs; }
    public void setFailedAuditLogs(long failedAuditLogs) { this.failedAuditLogs = failedAuditLogs; }
    public long getDeniedAccessAttempts() { return deniedAccessAttempts; }
    public void setDeniedAccessAttempts(long deniedAccessAttempts) { this.deniedAccessAttempts = deniedAccessAttempts; }
    public long getSensitiveDataExposures() { return sensitiveDataExposures; }
    public void setSensitiveDataExposures(long sensitiveDataExposures) { this.sensitiveDataExposures = sensitiveDataExposures; }
    public Map<String, Long> getEventsByRegulation() { return eventsByRegulation; }
    public void setEventsByRegulation(Map<String, Long> eventsByRegulation) { this.eventsByRegulation = eventsByRegulation; }
    public Map<String, Long> getEventsByType() { return eventsByType; }
    public void setEventsByType(Map<String, Long> eventsByType) { this.eventsByType = eventsByType; }
    public Map<String, Long> getConsentsByType() { return consentsByType; }
    public void setConsentsByType(Map<String, Long> consentsByType) { this.consentsByType = consentsByType; }
    public Map<String, Long> getConsentsByStatus() { return consentsByStatus; }
    public void setConsentsByStatus(Map<String, Long> consentsByStatus) { this.consentsByStatus = consentsByStatus; }
    public Map<String, Long> getAuditActionsBreakdown() { return auditActionsBreakdown; }
    public void setAuditActionsBreakdown(Map<String, Long> auditActionsBreakdown) { this.auditActionsBreakdown = auditActionsBreakdown; }
    public Double getAverageAuditDurationMs() { return averageAuditDurationMs; }
    public void setAverageAuditDurationMs(Double averageAuditDurationMs) { this.averageAuditDurationMs = averageAuditDurationMs; }
    public double getComplianceScore() { return complianceScore; }
    public void setComplianceScore(double complianceScore) { this.complianceScore = complianceScore; }
}
