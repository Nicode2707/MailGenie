package com.email.writer;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;

/**
 * Entity tracking individual email deliverability events.
 * Records the full lifecycle of each email from send through delivery,
 * bounce, complaint, or engagement events for reputation scoring.
 */
@Entity
@Table(name = "deliverability_records", indexes = {
        @Index(name = "idx_dr_sender", columnList = "senderDomain"),
        @Index(name = "idx_dr_recipient_domain", columnList = "recipientDomain"),
        @Index(name = "idx_dr_event_type", columnList = "eventType"),
        @Index(name = "idx_dr_sent_at", columnList = "sentAt"),
        @Index(name = "idx_dr_campaign_id", columnList = "campaignId")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliverabilityRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Sender email address or domain identifier.
     */
    @Column(nullable = false, length = 255)
    private String senderEmail;

    /**
     * Sender's domain (e.g., mailgenie.com).
     */
    @Column(nullable = false, length = 255)
    private String senderDomain;

    /**
     * Recipient email address.
     */
    @Column(nullable = false, length = 255)
    private String recipientEmail;

    /**
     * Recipient's domain (e.g., gmail.com, outlook.com).
     */
    @Column(nullable = false, length = 255)
    private String recipientDomain;

    /**
     * Event type: SENT, DELIVERED, BOUNCED, SPAM_COMPLAINT, OPENED,
     * CLICKED, DEFERRED, REJECTED, UNSUBSCRIBED.
     */
    @Column(nullable = false, length = 25)
    private String eventType;

    /**
     * Bounce sub-type: HARD, SOFT, CONTENT, SIZE, FULL, BLOCKED, UNKNOWN.
     */
    @Column(length = 20)
    private String bounceType;

    /**
     * SMTP response code (e.g., 550, 421).
     */
    private Integer smtpCode;

    /**
     * SMTP diagnostic message from the receiving server.
     */
    @Column(columnDefinition = "TEXT")
    private String smtpDiagnostic;

    /**
     * Whether this email was part of an A/B test campaign.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean isCampaignEmail = false;

    /**
     * Reference to the campaign ID (if applicable).
     */
    private Long campaignId;

    /**
     * Reference to the variant ID (if applicable).
     */
    private Long variantId;

    /**
     * IP address of the sending server.
     */
    @Column(length = 45)
    private String sendingIp;

    /**
     * Content type: plain, html, mixed.
     */
    @Column(length = 20)
    @Builder.Default
    private String contentType = "html";

    /**
     * Email subject line (for spam analysis).
     */
    @Column(length = 500)
    private String subjectLine;

    /**
     * SpamAssassin-like score for this email (if computed).
     */
    @Builder.Default
    private Double spamScore = 0.0;

    /**
     * Whether the email passed SPF check.
     */
    private Boolean spfPass;

    /**
     * Whether the email passed DKIM check.
     */
    private Boolean dkimPass;

    /**
     * Whether the email passed DMARC check.
     */
    private Boolean dmarcPass;

    /**
     * Processing latency in milliseconds.
     */
    private Long latencyMs;

    @Column(nullable = false)
    private LocalDateTime sentAt;

    /**
     * When the delivery event was recorded (may differ from sentAt for async events).
     */
    private LocalDateTime eventRecordedAt;

    @PrePersist
    protected void onCreate() {
        if (eventRecordedAt == null) {
            eventRecordedAt = LocalDateTime.now();
        }
    }
}
