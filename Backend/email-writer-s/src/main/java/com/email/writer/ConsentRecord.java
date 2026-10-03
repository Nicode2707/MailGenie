package com.email.writer;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "consent_records", indexes = {
    @Index(name = "idx_consent_user", columnList = "userId"),
    @Index(name = "idx_consent_email", columnList = "email"),
    @Index(name = "idx_consent_type", columnList = "consentType"),
    @Index(name = "idx_consent_status", columnList = "status"),
    @Index(name = "idx_consent_source", columnList = "source")
})
public class ConsentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String consentType; // MARKETING, TRANSACTIONAL, ANALYTICS, THIRD_PARTY, PROFILING, NEWSLETTER

    @Column(nullable = false)
    private String status; // GRANTED, WITHDRAWN, EXPIRED, PENDING, REFUSED

    @Column(nullable = false)
    private String source; // WEB_FORM, API, IMPORT, DOUBLE_OPTIN, PAPER, PHONE, IN_APP

    private String purpose;

    @Column(columnDefinition = "TEXT")
    private String evidenceUrl; // link to signed form, screenshot, etc.

    private String ipAddress;

    private String userAgent;

    private Boolean doubleOptInConfirmed;

    private LocalDateTime doubleOptInDate;

    private LocalDateTime consentDate;

    private LocalDateTime expiryDate;

    private LocalDateTime withdrawalDate;

    private String withdrawalReason;

    @Column(columnDefinition = "TEXT")
    private String metadataJson;

    private Boolean isActive;

    private Integer version; // consent version for audit trail

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (consentDate == null) consentDate = LocalDateTime.now();
        if (status == null) status = "PENDING";
        if (isActive == null) isActive = true;
        if (version == null) version = 1;
        if (doubleOptInConfirmed == null) doubleOptInConfirmed = false;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public ConsentRecord() {}

    public ConsentRecord(String userId, String email, String consentType, String source) {
        this.userId = userId;
        this.email = email;
        this.consentType = consentType;
        this.source = source;
    }

    public boolean isGranted() { return "GRANTED".equals(status); }
    public boolean isWithdrawn() { return "WITHDRAWN".equals(status); }
    public boolean isExpired() {
        if ("EXPIRED".equals(status)) return true;
        return expiryDate != null && LocalDateTime.now().isAfter(expiryDate);
    }
    public boolean isValid() { return isGranted() && !isExpired(); }
    public boolean requiresDoubleOptIn() { return !Boolean.TRUE.equals(doubleOptInConfirmed); }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getConsentType() { return consentType; }
    public void setConsentType(String consentType) { this.consentType = consentType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    public String getEvidenceUrl() { return evidenceUrl; }
    public void setEvidenceUrl(String evidenceUrl) { this.evidenceUrl = evidenceUrl; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public Boolean getDoubleOptInConfirmed() { return doubleOptInConfirmed; }
    public void setDoubleOptInConfirmed(Boolean doubleOptInConfirmed) { this.doubleOptInConfirmed = doubleOptInConfirmed; }
    public LocalDateTime getDoubleOptInDate() { return doubleOptInDate; }
    public void setDoubleOptInDate(LocalDateTime doubleOptInDate) { this.doubleOptInDate = doubleOptInDate; }
    public LocalDateTime getConsentDate() { return consentDate; }
    public void setConsentDate(LocalDateTime consentDate) { this.consentDate = consentDate; }
    public LocalDateTime getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDateTime expiryDate) { this.expiryDate = expiryDate; }
    public LocalDateTime getWithdrawalDate() { return withdrawalDate; }
    public void setWithdrawalDate(LocalDateTime withdrawalDate) { this.withdrawalDate = withdrawalDate; }
    public String getWithdrawalReason() { return withdrawalReason; }
    public void setWithdrawalReason(String withdrawalReason) { this.withdrawalReason = withdrawalReason; }
    public String getMetadataJson() { return metadataJson; }
    public void setMetadataJson(String metadataJson) { this.metadataJson = metadataJson; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
