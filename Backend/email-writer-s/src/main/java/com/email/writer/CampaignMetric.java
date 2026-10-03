package com.email.writer;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;

/**
 * Entity tracking individual metric events for campaign variants.
 * Records each open, click, reply, conversion, bounce, and unsubscribe.
 */
@Entity
@Table(name = "campaign_metrics", indexes = {
        @Index(name = "idx_cm_campaign_id", columnList = "campaignId"),
        @Index(name = "idx_cm_variant_id", columnList = "variantId"),
        @Index(name = "idx_cm_event_type", columnList = "eventType"),
        @Index(name = "idx_cm_recorded_at", columnList = "recordedAt")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CampaignMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long campaignId;

    @Column(nullable = false)
    private Long variantId;

    /**
     * Event type: SENT, OPEN, CLICK, REPLY, CONVERSION, BOUNCE, UNSUBSCRIBE.
     */
    @Column(nullable = false, length = 20)
    private String eventType;

    /**
     * Recipient email associated with this event (optional for anonymity).
     */
    @Column(length = 255)
    private String recipientEmail;

    /**
     * Device type: desktop, mobile, tablet, unknown.
     */
    @Column(length = 20)
    private String deviceType;

    /**
     * Geographic region (optional).
     */
    @Column(length = 100)
    private String region;

    /**
     * Time from send to this event in milliseconds (for opens/clicks/replies).
     */
    private Long timeToEventMs;

    /**
     * Revenue attributed to this event (for conversions).
     */
    @Builder.Default
    private Double revenue = 0.0;

    /**
     * Custom metadata as JSON (e.g., link URL clicked, reply length).
     */
    @Column(columnDefinition = "TEXT")
    private String metadata;

    @Column(nullable = false)
    private LocalDateTime recordedAt;

    @PrePersist
    protected void onCreate() {
        recordedAt = LocalDateTime.now();
    }
}
