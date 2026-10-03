package com.email.writer;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;

/**
 * Entity representing an email campaign that can contain multiple A/B test variants.
 * Tracks overall campaign performance, scheduling, and targeting configuration.
 */
@Entity
@Table(name = "email_campaigns", indexes = {
        @Index(name = "idx_camp_status", columnList = "status"),
        @Index(name = "idx_camp_type", columnList = "campaignType"),
        @Index(name = "idx_camp_start", columnList = "startDate")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailCampaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 500)
    private String description;

    /**
     * Campaign type: AB_TEST, BATCH, DRIP, ONE_SHOT.
     */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String campaignType = "AB_TEST";

    /**
     * Status: DRAFT, RUNNING, PAUSED, COMPLETED, CANCELLED.
     */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "DRAFT";

    /**
     * Total number of recipients targeted.
     */
    @Column(nullable = false)
    @Builder.Default
    private Long totalRecipients = 0L;

    /**
     * Number of variants in this campaign.
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer variantCount = 0;

    /**
     * Winning variant ID (null until campaign completes and a winner is chosen).
     */
    private Long winningVariantId;

    /**
     * Confidence level for the A/B test winner selection (0-100).
     */
    @Builder.Default
    private Double confidenceLevel = 0.0;

    /**
     * Statistical significance threshold (e.g. 0.05 for 95% confidence).
     */
    @Column(nullable = false)
    @Builder.Default
    private Double significanceThreshold = 0.05;

    /**
     * The primary metric for determining the winner: CLICK_RATE, OPEN_RATE, REPLY_RATE, CONVERSION_RATE.
     */
    @Column(length = 30)
    @Builder.Default
    private String primaryMetric = "CLICK_RATE";

    /**
     * Target audience segment (comma-separated tags or "all").
     */
    @Column(length = 500)
    @Builder.Default
    private String targetSegment = "all";

    /**
     * LLM provider used for generating campaign content.
     */
    @Column(length = 30)
    private String provider;

    /**
     * Campaign start date/time.
     */
    private LocalDateTime startDate;

    /**
     * Campaign end date/time.
     */
    private LocalDateTime endDate;

    /**
     * How long to run the test before declaring a winner (in hours).
     */
    @Builder.Default
    private Integer testDurationHours = 48;

    /**
     * Minimum sample size per variant before declaring a winner.
     */
    @Builder.Default
    private Integer minSampleSize = 100;

    /**
     * Tag/label for organizing campaigns.
     */
    @Column(length = 300)
    private String tags;

    /**
     * Creator/author of the campaign.
     */
    @Column(length = 100)
    private String createdBy;

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
     * Check if the campaign test period has elapsed.
     */
    @Transient
    public boolean isTestPeriodComplete() {
        return endDate != null && LocalDateTime.now().isAfter(endDate);
    }

    /**
     * Check if the campaign has enough sample data to determine a winner.
     */
    @Transient
    public boolean hasMinimumSampleSize() {
        return totalRecipients >= minSampleSize;
    }
}
