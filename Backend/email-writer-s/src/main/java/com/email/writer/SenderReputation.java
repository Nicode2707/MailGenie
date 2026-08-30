package com.email.writer;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;

/**
 * Entity representing the calculated reputation score for a sender domain.
 * Aggregated from individual deliverability records with weighted scoring.
 */
@Entity
@Table(name = "sender_reputations", indexes = {
        @Index(name = "idx_sr_domain", columnList = "domain", unique = true),
        @Index(name = "idx_sr_score", columnList = "reputationScore DESC")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SenderReputation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The sender domain (e.g., mailgenie.com).
     */
    @Column(nullable = false, unique = true, length = 255)
    private String domain;

    /**
     * Overall reputation score (0-100). Higher is better.
     */
    @Column(nullable = false)
    @Builder.Default
    private Double reputationScore = 50.0;

    /**
     * Health grade: A, B, C, D, F based on reputation score.
     */
    @Column(nullable = false, length = 5)
    @Builder.Default
    private String healthGrade = "C";

    /**
     * Total emails sent from this domain.
     */
    @Column(nullable = false)
    @Builder.Default
    private Long totalSent = 0L;

    /**
     * Total successfully delivered.
     */
    @Column(nullable = false)
    @Builder.Default
    private Long totalDelivered = 0L;

    /**
     * Total hard bounces.
     */
    @Column(nullable = false)
    @Builder.Default
    private Long hardBounces = 0L;

    /**
     * Total soft bounces.
     */
    @Column(nullable = false)
    @Builder.Default
    private Long softBounces = 0L;

    /**
     * Total spam complaints.
     */
    @Column(nullable = false)
    @Builder.Default
    private Long spamComplaints = 0L;

    /**
     * Total unsubscribes.
     */
    @Column(nullable = false)
    @Builder.Default
    private Long unsubscribes = 0L;

    /**
     * Total opens tracked.
     */
    @Column(nullable = false)
    @Builder.Default
    private Long totalOpens = 0L;

    /**
     * Total clicks tracked.
     */
    @Column(nullable = false)
    @Builder.Default
    private Long totalClicks = 0L;

    /**
     * Deliverability rate as a percentage (delivered / sent * 100).
     */
    @Builder.Default
    private Double deliverabilityRate = 100.0;

    /**
     * Bounce rate as a percentage.
     */
    @Builder.Default
    private Double bounceRate = 0.0;

    /**
     * Spam complaint rate as a percentage (complaints / sent * 100).
     */
    @Builder.Default
    private Double spamComplaintRate = 0.0;

    /**
     * Open rate as a percentage.
     */
    @Builder.Default
    private Double openRate = 0.0;

    /**
     * Click rate as a percentage.
     */
    @Builder.Default
    private Double clickRate = 0.0;

    /**
     * SPF pass rate as a percentage.
     */
    @Builder.Default
    private Double spfPassRate = 100.0;

    /**
     * DKIM pass rate as a percentage.
     */
    @Builder.Default
    private Double dkimPassRate = 100.0;

    /**
     * DMARC pass rate as a percentage.
     */
    @Builder.Default
    private Double dmarcPassRate = 100.0;

    /**
     * Average spam score of outgoing emails.
     */
    @Builder.Default
    private Double avgSpamScore = 0.0;

    /**
     * Number of unique recipient domains contacted.
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer uniqueRecipientDomains = 0;

    /**
     * Consecutive days without any negative events (bounces/complaints).
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer streakDays = 0;

    /**
     * Risk level: LOW, MEDIUM, HIGH, CRITICAL.
     */
    @Column(nullable = false, length = 10)
    @Builder.Default
    private String riskLevel = "LOW";

    /**
     * Active blocklist warnings (comma-separated: "spamhaus,sorbs").
     */
    @Column(length = 500)
    private String blocklistWarnings;

    /**
     * Last time the reputation was recalculated.
     */
    private LocalDateTime lastCalculatedAt;

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
     * Check if the domain has a healthy reputation (grade A or B).
     */
    @Transient
    public boolean isHealthy() {
        return "A".equals(healthGrade) || "B".equals(healthGrade);
    }

    /**
     * Check if the domain is at risk (grade D or F).
     */
    @Transient
    public boolean isAtRisk() {
        return "D".equals(healthGrade) || "F".equals(healthGrade);
    }
}
