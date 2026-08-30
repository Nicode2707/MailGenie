package com.email.writer;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "email_events", indexes = {
    @Index(name = "idx_event_type", columnList = "eventType"),
    @Index(name = "idx_event_user", columnList = "userId"),
    @Index(name = "idx_event_email", columnList = "recipientEmail"),
    @Index(name = "idx_event_date", columnList = "eventTimestamp"),
    @Index(name = "idx_event_campaign", columnList = "campaignId")
})
public class EmailEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String eventType; // SENT, DELIVERED, OPENED, CLICKED, BOUNCED, COMPLAINED, UNSUBSCRIBED, FORWARD

    @Column(nullable = false)
    private String userId;

    private Long emailId;

    private Long campaignId;

    @Column(nullable = false)
    private String recipientEmail;

    private String senderEmail;

    private String subject;

    private String linkUrl; // for click events

    private String linkText;

    private String bounceType; // HARD, SOFT

    private String bounceReason;

    private String deviceType; // DESKTOP, MOBILE, TABLET, UNKNOWN

    private String deviceOs; // IOS, ANDROID, WINDOWS, MACOS, LINUX

    private String emailClient; // GMAIL, OUTLOOK, APPLE_MAIL, THUNDERBIRD

    private String ipAddress;

    private String geoCountry;

    private String geoCity;

    private Long timeToOpenSeconds; // seconds from send to first open

    private Long timeToClickSeconds; // seconds from send to first click

    private Boolean isFirstOpen;

    private Boolean isFirstClick;

    @Column(columnDefinition = "TEXT")
    private String metadataJson;

    private LocalDateTime eventTimestamp;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (eventTimestamp == null) eventTimestamp = LocalDateTime.now();
        if (isFirstOpen == null) isFirstOpen = false;
        if (isFirstClick == null) isFirstClick = false;
    }

    public EmailEvent() {}

    public EmailEvent(String eventType, String userId, String recipientEmail) {
        this.eventType = eventType;
        this.userId = userId;
        this.recipientEmail = recipientEmail;
    }

    public boolean isEngagementEvent() { return "OPENED".equals(eventType) || "CLICKED".equals(eventType); }
    public boolean isNegativeEvent() { return "BOUNCED".equals(eventType) || "COMPLAINED".equals(eventType) || "UNSUBSCRIBED".equals(eventType); }
    public boolean isDeliveryEvent() { return "SENT".equals(eventType) || "DELIVERED".equals(eventType); }
    public boolean hasTimingData() { return timeToOpenSeconds != null || timeToClickSeconds != null; }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public Long getEmailId() { return emailId; }
    public void setEmailId(Long emailId) { this.emailId = emailId; }
    public Long getCampaignId() { return campaignId; }
    public void setCampaignId(Long campaignId) { this.campaignId = campaignId; }
    public String getRecipientEmail() { return recipientEmail; }
    public void setRecipientEmail(String recipientEmail) { this.recipientEmail = recipientEmail; }
    public String getSenderEmail() { return senderEmail; }
    public void setSenderEmail(String senderEmail) { this.senderEmail = senderEmail; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getLinkUrl() { return linkUrl; }
    public void setLinkUrl(String linkUrl) { this.linkUrl = linkUrl; }
    public String getLinkText() { return linkText; }
    public void setLinkText(String linkText) { this.linkText = linkText; }
    public String getBounceType() { return bounceType; }
    public void setBounceType(String bounceType) { this.bounceType = bounceType; }
    public String getBounceReason() { return bounceReason; }
    public void setBounceReason(String bounceReason) { this.bounceReason = bounceReason; }
    public String getDeviceType() { return deviceType; }
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }
    public String getDeviceOs() { return deviceOs; }
    public void setDeviceOs(String deviceOs) { this.deviceOs = deviceOs; }
    public String getEmailClient() { return emailClient; }
    public void setEmailClient(String emailClient) { this.emailClient = emailClient; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public String getGeoCountry() { return geoCountry; }
    public void setGeoCountry(String geoCountry) { this.geoCountry = geoCountry; }
    public String getGeoCity() { return geoCity; }
    public void setGeoCity(String geoCity) { this.geoCity = geoCity; }
    public Long getTimeToOpenSeconds() { return timeToOpenSeconds; }
    public void setTimeToOpenSeconds(Long timeToOpenSeconds) { this.timeToOpenSeconds = timeToOpenSeconds; }
    public Long getTimeToClickSeconds() { return timeToClickSeconds; }
    public void setTimeToClickSeconds(Long timeToClickSeconds) { this.timeToClickSeconds = timeToClickSeconds; }
    public Boolean getIsFirstOpen() { return isFirstOpen; }
    public void setIsFirstOpen(Boolean isFirstOpen) { this.isFirstOpen = isFirstOpen; }
    public Boolean getIsFirstClick() { return isFirstClick; }
    public void setIsFirstClick(Boolean isFirstClick) { this.isFirstClick = isFirstClick; }
    public String getMetadataJson() { return metadataJson; }
    public void setMetadataJson(String metadataJson) { this.metadataJson = metadataJson; }
    public LocalDateTime getEventTimestamp() { return eventTimestamp; }
    public void setEventTimestamp(LocalDateTime eventTimestamp) { this.eventTimestamp = eventTimestamp; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
