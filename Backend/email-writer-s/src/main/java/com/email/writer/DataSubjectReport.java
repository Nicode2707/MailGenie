package com.email.writer;

import java.util.List;

public class DataSubjectReport {
    private String userId;
    private String email;
    private List<ConsentRecord> consentRecords;
    private List<ComplianceEvent> complianceEvents;
    private List<AuditLog> auditLogs;
    private long totalEmailsSent;
    private long totalTemplatesUsed;
    private long totalSnippetsUsed;
    private long totalCampaignsInvolved;
    private List<String> dataCategories;
    private List<String> thirdPartiesWithAccess;
    private long totalDataSizeBytes;

    public DataSubjectReport() {}

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public List<ConsentRecord> getConsentRecords() { return consentRecords; }
    public void setConsentRecords(List<ConsentRecord> consentRecords) { this.consentRecords = consentRecords; }
    public List<ComplianceEvent> getComplianceEvents() { return complianceEvents; }
    public void setComplianceEvents(List<ComplianceEvent> complianceEvents) { this.complianceEvents = complianceEvents; }
    public List<AuditLog> getAuditLogs() { return auditLogs; }
    public void setAuditLogs(List<AuditLog> auditLogs) { this.auditLogs = auditLogs; }
    public long getTotalEmailsSent() { return totalEmailsSent; }
    public void setTotalEmailsSent(long totalEmailsSent) { this.totalEmailsSent = totalEmailsSent; }
    public long getTotalTemplatesUsed() { return totalTemplatesUsed; }
    public void setTotalTemplatesUsed(long totalTemplatesUsed) { this.totalTemplatesUsed = totalTemplatesUsed; }
    public long getTotalSnippetsUsed() { return totalSnippetsUsed; }
    public void setTotalSnippetsUsed(long totalSnippetsUsed) { this.totalSnippetsUsed = totalSnippetsUsed; }
    public long getTotalCampaignsInvolved() { return totalCampaignsInvolved; }
    public void setTotalCampaignsInvolved(long totalCampaignsInvolved) { this.totalCampaignsInvolved = totalCampaignsInvolved; }
    public List<String> getDataCategories() { return dataCategories; }
    public void setDataCategories(List<String> dataCategories) { this.dataCategories = dataCategories; }
    public List<String> getThirdPartiesWithAccess() { return thirdPartiesWithAccess; }
    public void setThirdPartiesWithAccess(List<String> thirdPartiesWithAccess) { this.thirdPartiesWithAccess = thirdPartiesWithAccess; }
    public long getTotalDataSizeBytes() { return totalDataSizeBytes; }
    public void setTotalDataSizeBytes(long totalDataSizeBytes) { this.totalDataSizeBytes = totalDataSizeBytes; }
}
