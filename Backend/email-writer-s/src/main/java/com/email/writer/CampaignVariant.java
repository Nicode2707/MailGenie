package com.email.writer;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;

/**
 * Entity representing a single variant within an A/B test campaign.
 * Each variant has its own subject line, body, tone, and performance metrics.
 */
@Entity
@Table(name = "campaign_variants", indexes = {
        @Index(name = "idx_cv_campaign_id", columnList = "campaignId"),
        @Index(name = "idx_cv_label", columnList = "label")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CampaignVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Parent campaign reference.
     */
    @Column(nullable = false)
    private Long campaignId;

    /**
     * Variant label (e.g. "A", "B", "Control").
     */
    @Column(nullable = false, length = 50)
    private String label;

    /**
     * Subject line for this variant.
     */
    @Column(nullable = false, length = 500)
    private String subject;

    /**
     * Email body content for this variant.
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String body;

    /**
     * Tone applied to this variant: professional, casual, friendly, urgent, persuasive.
     */
    @Column(length = 30)
    private String tone;

    /**
     * Language for this variant.
     */
    @Column(length = 30)
    @Builder.Default
    private String language = "English";

    /**
     * Traffic allocation percentage (e.g. 50 for a 50/50 split).
     */
    @Column(nullable = false)
    @Builder.Default
    private Double trafficPercent = 50.0;

    /**
     * Number of emails sent for this variant.
     */
    @Column(nullable = false)
    @Builder.Default
    private Long sentCount = 0L;

    /**
     * Number of opens tracked.
     */
    @Column(nullable = false)
    @Builder.Default
    private Long openCount = 0L;

    /**
     * Number of clicks tracked.
     */
    @Column(nullable = false)
    @Builder.Default
    private Long clickCount = 0L;

    /**
     * Number of replies tracked.
     */
    @Column(nullable = false)
    @Builder.Default
    private Long replyCount = 0L;

    /**
     * Number of conversions tracked.
     */
    @Column(nullable = false)
    @Builder.Default
    private Long conversionCount = 0L;

    /**
     * Number of unsubscribes.
     */
    @Column(nullable = false)
    @Builder.Default
    private Long unsubscribeCount = 0L;

    /**
     * Number of bounce-backs.
     */
    @Column(nullable = false)
    @Builder.Default
    private Long bounceCount = 0L;

    /**
     * Whether this variant is the selected winner.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean isWinner = false;

    /**
     * Confidence score for this variant being the winner (0-100).
     */
    @Builder.Default
    private Double confidenceScore = 0.0;

    /**
     * Average response time for replies to this variant (ms).
     */
    private Long avgResponseTimeMs;

    /**
     * Average revenue per email for this variant.
     */
    @Builder.Default
    private Double revenuePerEmail = 0.0;

    /**
     * Optional notes about this variant's design rationale.
     */
    @Column(columnDefinition = "TEXT")
    private String notes;

    /**
     * LLM prompt used to generate this variant (for audit).
     */
    @Column(columnDefinition = "TEXT")
    private String generationPrompt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Calculate open rate as a percentage.
     */
    @Transient
    public double getOpenRate() {
        return sentCount > 0 ? Math.round(((double) openCount / sentCount) * 10000.0) / 100.0 : 0.0;
    }

    /**
     * Calculate click-through rate as a percentage.
     */
    @Transient
    public double getClickRate() {
        return sentCount > 0 ? Math.round(((double) clickCount / sentCount) * 10000.0) / 100.0 : 0.0;
    }

    /**
     * Calculate reply rate as a percentage.
     */
    @Transient
    public double getReplyRate() {
        return sentCount > 0 ? Math.round(((double) replyCount / sentCount) * 10000.0) / 100.0 : 0.0;
    }

    /**
     * Calculate conversion rate as a percentage.
     */
    @Transient
    public double getConversionRate() {
        return sentCount > 0 ? Math.round(((double) conversionCount / sentCount) * 10000.0) / 100.0 : 0.0;
    }

    /**
     * Calculate unsubscribe rate as a percentage.
     */
    @Transient
    public double getUnsubscribeRate() {
        return sentCount > 0 ? Math.round(((double) unsubscribeCount / sentCount) * 10000.0) / 100.0 : 0.0;
    }

    /**
     * Calculate bounce rate as a percentage.
     */
    @Transient
    public double getBounceRate() {
        return sentCount > 0 ? Math.round(((double) bounceCount / sentCount) * 10000.0) / 100.0 : 0.0;
    }

    /**
     * Calculate total engagement (opens + clicks + replies).
     */
    @Transient
    public long getTotalEngagement() {
        return openCount + clickCount + replyCount;
    }

    /**
     * Calculate engagement rate as a percentage of sent.
     */
    @Transient
    public double getEngagementRate() {
        return sentCount > 0 ? Math.round(((double) getTotalEngagement() / sentCount) * 10000.0) / 100.0 : 0.0;
    }
}
