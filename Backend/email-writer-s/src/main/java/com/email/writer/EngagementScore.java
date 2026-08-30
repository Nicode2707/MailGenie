package com.email.writer;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "engagement_scores", indexes = {
    @Index(name = "idx_engagement_user", columnList = "userId"),
    @Index("idx_engagement_email"),
    @Index("idx_engagement_segment"),
    @Index("idx_engagement_score")
})
public class EngagementScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false, unique = true)
    private String email;

    private String segmentName;

    private double openRate;

    private double clickRate;

    private double replyRate;

    private double bounceRate;

    private double complaintRate;

    private double unsubscribeRate;

    private double engagementScore; // weighted composite 0-100

    private String engagementTier; // HOT, WARM, COLD, INACTIVE, UNENGAGED

    private long totalSent;

    private long totalDelivered;

    private long totalOpened;

    private long totalClicked;

    private long totalReplied;

    private long totalBounced;

    private long totalComplaints;

    private long totalUnsubscribed;

    private Long avgTimeToOpenSeconds;

    private Long avgTimeToClickSeconds;

    private Long maxTimeToOpenSeconds;

    private Long maxTimeToClickSeconds;

    private LocalDateTime lastEngagementAt;

    private LocalDateTime lastSentAt;

    private LocalDateTime lastOpenAt;

    private LocalDateTime lastClickAt;

    private int daysSinceLastEngagement;

    private boolean isVip;

    private boolean isAtRisk;

    private double scoreTrend; // positive = improving, negative = declining

    private LocalDateTime calculatedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (calculatedAt == null) calculatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public EngagementScore() {}

    public EngagementScore(String userId, String email) {
        this.userId = userId;
        this.email = email;
    }

    public void computeEngagementScore() {
        this.engagementScore = (openRate * 25) + (clickRate * 35) + (replyRate * 30)
            - (bounceRate * 5) - (complaintRate * 10) - (unsubscribeRate * 8);
        this.engagementScore = Math.max(0, Math.min(100, this.engagementScore));
        computeTier();
    }

    public void computeTier() {
        if (engagementScore >= 75) this.engagementTier = "HOT";
        else if (engagementScore >= 50) this.engagementTier = "WARM";
        else if (engagementScore >= 25) this.engagementTier = "COLD";
        else if (engagementScore >= 5) this.engagementTier = "INACTIVE";
        else this.engagementTier = "UNENGAGED";
        this.isAtRisk = "COLD".equals(engagementTier) || "INACTIVE".equals(engagementTier);
        this.isVip = "HOT".equals(engagementTier) && totalSent >= 10;
    }

    public boolean isHot() { return "HOT".equals(engagementTier); }
    public boolean isWarm() { return "WARM".equals(engagementTier); }
    public boolean isCold() { return "COLD".equals(engagementTier); }
    public boolean isInactive() { return "INACTIVE".equals(engagementTier) || "UNENGAGED".equals(engagementTier); }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getSegmentName() { return segmentName; }
    public void setSegmentName(String segmentName) { this.segmentName = segmentName; }
    public double getOpenRate() { return openRate; }
    public void setOpenRate(double openRate) { this.openRate = openRate; }
    public double getClickRate() { return clickRate; }
    public void setClickRate(double clickRate) { this.clickRate = clickRate; }
    public double getReplyRate() { return replyRate; }
    public void setReplyRate(double replyRate) { this.replyRate = replyRate; }
    public double getBounceRate() { return bounceRate; }
    public void setBounceRate(double bounceRate) { this.bounceRate = bounceRate; }
    public double getComplaintRate() { return complaintRate; }
    public void setComplaintRate(double complaintRate) { this.complaintRate = complaintRate; }
    public double getUnsubscribeRate() { return unsubscribeRate; }
    public void setUnsubscribeRate(double unsubscribeRate) { this.unsubscribeRate = unsubscribeRate; }
    public double getEngagementScore() { return engagementScore; }
    public void setEngagementScore(double engagementScore) { this.engagementScore = engagementScore; }
    public String getEngagementTier() { return engagementTier; }
    public void setEngagementTier(String engagementTier) { this.engagementTier = engagementTier; }
    public long getTotalSent() { return totalSent; }
    public void setTotalSent(long totalSent) { this.totalSent = totalSent; }
    public long getTotalDelivered() { return totalDelivered; }
    public void setTotalDelivered(long totalDelivered) { this.totalDelivered = totalDelivered; }
    public long getTotalOpened() { return totalOpened; }
    public void setTotalOpened(long totalOpened) { this.totalOpened = totalOpened; }
    public long getTotalClicked() { return totalClicked; }
    public void setTotalClicked(long totalClicked) { this.totalClicked = totalClicked; }
    public long getTotalReplied() { return totalReplied; }
    public void setTotalReplied(long totalReplied) { this.totalReplied = totalReplied; }
    public long getTotalBounced() { return totalBounced; }
    public void setTotalBounced(long totalBounced) { this.totalBounced = totalBounced; }
    public long getTotalComplaints() { return totalComplaints; }
    public void setTotalComplaints(long totalComplaints) { this.totalComplaints = totalComplaints; }
    public long getTotalUnsubscribed() { return totalUnsubscribed; }
    public void setTotalUnsubscribed(long totalUnsubscribed) { this.totalUnsubscribed = totalUnsubscribed; }
    public Long getAvgTimeToOpenSeconds() { return avgTimeToOpenSeconds; }
    public void setAvgTimeToOpenSeconds(Long avgTimeToOpenSeconds) { this.avgTimeToOpenSeconds = avgTimeToOpenSeconds; }
    public Long getAvgTimeToClickSeconds() { return avgTimeToClickSeconds; }
    public void setAvgTimeToClickSeconds(Long avgTimeToClickSeconds) { this.avgTimeToClickSeconds = avgTimeToClickSeconds; }
    public Long getMaxTimeToOpenSeconds() { return maxTimeToOpenSeconds; }
    public void setMaxTimeToOpenSeconds(Long maxTimeToOpenSeconds) { this.maxTimeToOpenSeconds = maxTimeToOpenSeconds; }
    public Long getMaxTimeToClickSeconds() { return maxTimeToClickSeconds; }
    public void setMaxTimeToClickSeconds(Long maxTimeToClickSeconds) { this.maxTimeToClickSeconds = maxTimeToClickSeconds; }
    public LocalDateTime getLastEngagementAt() { return lastEngagementAt; }
    public void setLastEngagementAt(LocalDateTime lastEngagementAt) { this.lastEngagementAt = lastEngagementAt; }
    public LocalDateTime getLastSentAt() { return lastSentAt; }
    public void setLastSentAt(LocalDateTime lastSentAt) { this.lastSentAt = lastSentAt; }
    public LocalDateTime getLastOpenAt() { return lastOpenAt; }
    public void setLastOpenAt(LocalDateTime lastOpenAt) { this.lastOpenAt = lastOpenAt; }
    public LocalDateTime getLastClickAt() { return lastClickAt; }
    public void setLastClickAt(LocalDateTime lastClickAt) { this.lastClickAt = lastClickAt; }
    public int getDaysSinceLastEngagement() { return daysSinceLastEngagement; }
    public void setDaysSinceLastEngagement(int daysSinceLastEngagement) { this.daysSinceLastEngagement = daysSinceLastEngagement; }
    public boolean isVip() { return isVip; }
    public void setIsVip(boolean isVip) { this.isVip = isVip; }
    public boolean isAtRisk() { return isAtRisk; }
    public void setIsAtRisk(boolean isAtRisk) { this.isAtRisk = isAtRisk; }
    public double getScoreTrend() { return scoreTrend; }
    public void setScoreTrend(double scoreTrend) { this.scoreTrend = scoreTrend; }
    public LocalDateTime getCalculatedAt() { return calculatedAt; }
    public void setCalculatedAt(LocalDateTime calculatedAt) { this.calculatedAt = calculatedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
