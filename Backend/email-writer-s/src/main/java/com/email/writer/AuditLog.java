package com.email.writer;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_user", columnList = "userId"),
    @Index(name = "idx_audit_action", columnList = "action"),
    @Index(name = "idx_audit_resource", columnList = "resourceType"),
    @Index(name = "idx_audit_date", columnList = "timestamp"),
    @Index(name = "idx_audit_outcome", columnList = "outcome")
})
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userId;

    private String username;

    private String ipAddress;

    private String userAgent;

    @Column(nullable = false)
    private String action; // CREATE, READ, UPDATE, DELETE, LOGIN, LOGOUT, EXPORT, IMPORT, SEND_EMAIL, APPROVE, REJECT, SHARE

    @Column(nullable = false)
    private String resourceType; // TEMPLATE, EMAIL, SNIPPET, CAMPAIGN, USER, ORGANIZATION, SETTINGS, CONSENT

    private Long resourceId;

    private String resourceName;

    @Column(columnDefinition = "TEXT")
    private String changesJson; // before/after diff

    @Column(nullable = false)
    private String outcome; // SUCCESS, FAILURE, DENIED, TIMEOUT

    private String failureReason;

    private Long durationMs;

    @Column(columnDefinition = "TEXT")
    private String requestMetadata; // additional context

    private Boolean containsSensitiveData;

    private Boolean requiresReview;

    private LocalDateTime timestamp;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (timestamp == null) timestamp = LocalDateTime.now();
        if (outcome == null) outcome = "SUCCESS";
    }

    public AuditLog() {}

    public AuditLog(String userId, String action, String resourceType, String outcome) {
        this.userId = userId;
        this.action = action;
        this.resourceType = resourceType;
        this.outcome = outcome;
    }

    public boolean isSuccess() { return "SUCCESS".equals(outcome); }
    public boolean isFailure() { return "FAILURE".equals(outcome); }
    public boolean isDenied() { return "DENIED".equals(outcome); }
    public boolean needsReview() { return Boolean.TRUE.equals(requiresReview); }
    public boolean hasSensitiveData() { return Boolean.TRUE.equals(containsSensitiveData); }
    public boolean isDestructive() { return "DELETE".equals(action); }
    public boolean isWriteOperation() { return "CREATE".equals(action) || "UPDATE".equals(action) || "DELETE".equals(action); }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
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
    public Boolean getRequiresReview() { return requiresReview; }
    public void setRequiresReview(Boolean requiresReview) { this.requiresReview = requiresReview; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
