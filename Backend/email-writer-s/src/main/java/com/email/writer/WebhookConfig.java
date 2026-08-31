package com.email.writer;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Represents a registered webhook endpoint that receives POST notifications
 * when specific email events occur (generation, scheduling, failure, etc.).
 */
@Entity
@Table(name = "webhook_configs", indexes = {
        @Index(name = "idx_webhook_active", columnList = "active"),
        @Index(name = "idx_webhook_event", columnList = "eventType")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Human-readable name for this webhook.
     */
    @Column(nullable = false)
    private String name;

    /**
     * The URL to POST event payloads to.
     */
    @Column(nullable = false, length = 2048)
    private String url;

    /**
     * Secret used to sign payloads via HMAC-SHA256 in the X-Webhook-Signature header.
     */
    private String secret;

    /**
     * Comma-separated event types this webhook subscribes to.
     * Supported: EMAIL_GENERATED, EMAIL_SCHEDULED, EMAIL_FAILED, EMAIL_COMPLETED
     */
    @Column(nullable = false)
    @Builder.Default
    private String eventType = "EMAIL_GENERATED";

    /**
     * Whether this webhook is currently active and should receive events.
     */
    @Builder.Default
    private boolean active = true;

    /**
     * Maximum number of retry attempts on delivery failure.
     */
    @Builder.Default
    private int maxRetries = 3;

    /**
     * Timeout in seconds for each delivery attempt.
     */
    @Builder.Default
    private int timeoutSeconds = 10;

    /**
     * Total successful deliveries to date.
     */
    @Builder.Default
    private long successCount = 0;

    /**
     * Total failed deliveries to date.
     */
    @Builder.Default
    private long failureCount = 0;

    /**
     * Timestamp of the most recent successful delivery.
     */
    private LocalDateTime lastDeliveredAt;

    /**
     * Timestamp of the most recent failed delivery.
     */
    private LocalDateTime lastFailedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Returns true if this webhook subscribes to the given event type.
     */
    public boolean subscribesTo(String eventType) {
        if (this.eventType == null || this.eventType.trim().isEmpty()) {
            return false;
        }
        if ("*".equals(this.eventType.trim())) {
            return true;
        }
        String[] subscribed = this.eventType.split(",");
        for (String s : subscribed) {
            if (s.trim().equalsIgnoreCase(eventType.trim())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Increments the success counter and updates the timestamp.
     */
    public void recordSuccess() {
        this.successCount++;
        this.lastDeliveredAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Increments the failure counter and updates the timestamp.
     */
    public void recordFailure() {
        this.failureCount++;
        this.lastFailedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Returns the total number of delivery attempts.
     */
    public long getTotalAttempts() {
        return successCount + failureCount;
    }

    /**
     * Returns the delivery success rate as a percentage (0-100).
     */
    public double getSuccessRate() {
        long total = getTotalAttempts();
        if (total == 0) return 0.0;
        return Math.round(((double) successCount / total) * 10000.0) / 100.0;
    }
}
