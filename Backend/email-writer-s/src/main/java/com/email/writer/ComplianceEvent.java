package com.email.writer;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "compliance_events", indexes = {
    @Index(name = "idx_compliance_event_type", columnList = "eventType"),
    @Index(name = "idx_compliance_event_user", columnList = "userId"),
    @Index(name = "idx_compliance_event_date", columnList = "eventDate"),
    @Index(name = "idx_compliance_event_status", columnList = "status")
})
public class ComplianceEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String eventType; // GDPR_ACCESS, GDPR_DELETION, GDPR_EXPORT, CONSENT_COLLECTED, CONSENT_WITHDRAWN, CAN_SPAM_UNSUBSCRIBE, DATA_BREACH, RETENTION_DELETE, AUDIT_LOGIN, AUDIT_EXPORT

    @Column(nullable = false)
    private String userId;

    private String targetUserId; // for admin actions on other users

    private String targetEmail;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String status; // PENDING, IN_PROGRESS, COMPLETED, FAILED, EXEMPT

    @Column(nullable = false)
    private String severity; // INFO, WARNING, CRITICAL, EMERGENCY

    @Column(nullable = false)
    private String regulation; // GDPR, CAN_SPAM, CCPA, PECR, LGPD

    private String lawfulBasis; // consent, legitimate_interest, legal_obligation, contract, vital_interest, public_task

    @Column(columnDefinition = "TEXT")
    private String metadataJson;

    private Integer dataSubjectRequestType; // 1=ACCESS, 2=RECTIFICATION, 3=ERASURE, 4=RESTRICTION, 5=PORTABILITY, 6=OBJECTION

    private LocalDateTime eventDate;

    private LocalDateTime completionDeadline;

    private LocalDateTime completedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (eventDate == null) eventDate = LocalDateTime.now();
        if (status == null) status = "PENDING";
        if (severity == null) severity = "INFO";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public ComplianceEvent() {}

    public ComplianceEvent(String eventType, String userId, String regulation) {
        this.eventType = eventType;
        this.userId = userId;
        this.regulation = regulation;
    }

    public boolean isCompleted() { return "COMPLETED".equals(status); }
    public boolean isPending() { return "PENDING".equals(status); }
    public boolean isFailed() { return "FAILED".equals(status); }
    public boolean isCritical() { return "CRITICAL".equals(severity) || "EMERGENCY".equals(severity); }
    public boolean isOverdue() { return completionDeadline != null && LocalDateTime.now().isAfter(completionDeadline) && !isCompleted(); }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
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
    public LocalDateTime getEventDate() { return eventDate; }
    public void setEventDate(LocalDateTime eventDate) { this.eventDate = eventDate; }
    public LocalDateTime getCompletionDeadline() { return completionDeadline; }
    public void setCompletionDeadline(LocalDateTime completionDeadline) { this.completionDeadline = completionDeadline; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
