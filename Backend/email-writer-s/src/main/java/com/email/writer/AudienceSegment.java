package com.email.writer;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audience_segments", indexes = {
    @Index("idx_segment_user"),
    @Index("idx_segment_name"),
    @Index("idx_segment_type")
})
public class AudienceSegment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private String segmentType; // DYNAMIC, STATIC, BEHAVIORAL, ENGAGEMENT_BASED

    private String criteriaJson; // filter rules: min opens, tier, domain, activity window, etc.

    private long memberCount;

    private long activeMemberCount;

    private double avgEngagementScore;

    private String dominantTier;

    private boolean isActive;

    private String triggerEvent; // for behavioral: OPEN, CLICK, BOUNCE, etc.

    private Integer triggerWindowDays;

    private Integer triggerMinCount;

    private LocalDateTime lastRefreshedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (isActive == null) isActive = true;
        if (segmentType == null) segmentType = "DYNAMIC";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public AudienceSegment() {}

    public AudienceSegment(String userId, String name, String segmentType) {
        this.userId = userId;
        this.name = name;
        this.segmentType = segmentType;
    }

    public boolean isDynamic() { return "DYNAMIC".equals(segmentType); }
    public boolean isStatic() { return "STATIC".equals(segmentType); }
    public boolean isBehavioral() { return "BEHAVIORAL".equals(segmentType); }
    public boolean isEngagementBased() { return "ENGAGEMENT_BASED".equals(segmentType); }
    public boolean isEmpty() { return memberCount == 0; }
    public boolean hasHighEngagement() { return avgEngagementScore >= 75; }
    public boolean needsRefresh() { return lastRefreshedAt == null || lastRefreshedAt.isBefore(LocalDateTime.now().minusHours(24)); }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getSegmentType() { return segmentType; }
    public void setSegmentType(String segmentType) { this.segmentType = segmentType; }
    public String getCriteriaJson() { return criteriaJson; }
    public void setCriteriaJson(String criteriaJson) { this.criteriaJson = criteriaJson; }
    public long getMemberCount() { return memberCount; }
    public void setMemberCount(long memberCount) { this.memberCount = memberCount; }
    public long getActiveMemberCount() { return activeMemberCount; }
    public void setActiveMemberCount(long activeMemberCount) { this.activeMemberCount = activeMemberCount; }
    public double getAvgEngagementScore() { return avgEngagementScore; }
    public void setAvgEngagementScore(double avgEngagementScore) { this.avgEngagementScore = avgEngagementScore; }
    public String getDominantTier() { return dominantTier; }
    public void setDominantTier(String dominantTier) { this.dominantTier = dominantTier; }
    public boolean getIsActive() { return isActive; }
    public void setIsActive(boolean isActive) { this.isActive = isActive; }
    public String getTriggerEvent() { return triggerEvent; }
    public void setTriggerEvent(String triggerEvent) { this.triggerEvent = triggerEvent; }
    public Integer getTriggerWindowDays() { return triggerWindowDays; }
    public void setTriggerWindowDays(Integer triggerWindowDays) { this.triggerWindowDays = triggerWindowDays; }
    public Integer getTriggerMinCount() { return triggerMinCount; }
    public void setTriggerMinCount(Integer triggerMinCount) { this.triggerMinCount = triggerMinCount; }
    public LocalDateTime getLastRefreshedAt() { return lastRefreshedAt; }
    public void setLastRefreshedAt(LocalDateTime lastRefreshedAt) { this.lastRefreshedAt = lastRefreshedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
