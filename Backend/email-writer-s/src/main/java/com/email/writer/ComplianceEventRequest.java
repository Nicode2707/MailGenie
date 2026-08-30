package com.email.writer;

public class ComplianceEventRequest {
    private String eventType;
    private String userId;
    private String targetUserId;
    private String targetEmail;
    private String description;
    private String severity;
    private String regulation;
    private String lawfulBasis;
    private String metadataJson;
    private Integer dataSubjectRequestType;
    private Integer deadlineDays;

    public ComplianceEventRequest() {}

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getTargetUserId() { return targetUserId; }
    public void setTargetUserId(String targetUserId) { this.targetUserId = targetUserId; }
    public String getTargetEmail() { return targetEmail; }
    public void setTargetEmail(String targetEmail) { this.targetEmail = targetEmail; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getRegulation() { return regulation; }
    public void setRegulation(String regulation) { this.regulation = regulation; }
    public String getLawfulBasis() { return lawfulBasis; }
    public void setLawfulBasis(String lawfulBasis) { this.lawfulBasis = lawfulBasis; }
    public String getMetadataJson() { return metadataJson; }
    public void setMetadataJson(String metadataJson) { this.metadataJson = metadataJson; }
    public Integer getDataSubjectRequestType() { return dataSubjectRequestType; }
    public void setDataSubjectRequestType(Integer dataSubjectRequestType) { this.dataSubjectRequestType = dataSubjectRequestType; }
    public Integer getDeadlineDays() { return deadlineDays; }
    public void setDeadlineDays(Integer deadlineDays) { this.deadlineDays = deadlineDays; }
}
