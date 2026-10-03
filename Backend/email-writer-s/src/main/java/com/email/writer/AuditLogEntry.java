package com.email.writer;

public class AuditLogEntry {
    private String userId;
    private String action;
    private String resourceType;
    private Long resourceId;
    private String resourceName;
    private String changesJson;
    private String outcome;
    private String failureReason;
    private Long durationMs;
    private String requestMetadata;
    private Boolean containsSensitiveData;
    private String ipAddress;
    private String userAgent;

    public AuditLogEntry() {}

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }
    public Long getResourceId() { return resourceId; }
    public void setResourceId(Long resourceId) { this.resourceId = resourceId; }
    public String getResourceName() { return resourceName; }
    public void setResourceName(String resourceName) { this.resourceName = resourceName; }
    public String getChangesJson() { return changesJson; }
    public void setChangesJson(String changesJson) { this.changesJson = changesJson; }
    public String getOutcome() { return outcome; }
    public void setOutcome(String outcome) { this.outcome = outcome; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
    public String getRequestMetadata() { return requestMetadata; }
    public void setRequestMetadata(String requestMetadata) { this.requestMetadata = requestMetadata; }
    public Boolean getContainsSensitiveData() { return containsSensitiveData; }
    public void setContainsSensitiveData(Boolean containsSensitiveData) { this.containsSensitiveData = containsSensitiveData; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
}
