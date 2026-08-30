package com.email.writer;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Records every webhook delivery attempt for auditing and debugging.
 * Each row represents one HTTP POST attempt to a webhook URL.
 */
@Entity
@Table(name = "webhook_delivery_logs", indexes = {
        @Index(name = "idx_delivery_status", columnList = "status"),
        @Index(name = "idx_delivery_webhook", columnList = "webhookConfigId"),
        @Index(name = "idx_delivery_event", columnList = "eventType")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookDeliveryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Foreign key reference to the WebhookConfig that was targeted.
     */
    @Column(nullable = false)
    private Long webhookConfigId;

    /**
     * The event type that triggered this delivery.
     */
    @Column(nullable = false)
    private String eventType;

    /**
     * The URL that was POSTed to.
     */
    @Column(nullable = false, length = 2048)
    private String targetUrl;

    /**
     * HTTP status code returned by the remote server.
     * Null if the connection failed entirely.
     */
    private Integer httpStatusCode;

    /**
     * Response body from the remote server (truncated to 2000 chars).
     */
    @Column(columnDefinition = "TEXT")
    private String responseBody;

    /**
     * Current delivery status: DELIVERED, FAILED, RETRYING, EXHAUSTED.
     */
    @Column(nullable = false)
    @Builder.Default
    private String status = "PENDING";

    /**
     * Number of attempts made so far for this delivery.
     */
    @Builder.Default
    private int attemptNumber = 1;

    /**
     * Duration of the HTTP request in milliseconds.
     */
    private Long durationMs;

    /**
     * Error message if the delivery failed (connection error, timeout, etc.).
     */
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * The JSON payload that was sent.
     */
    @Column(columnDefinition = "TEXT")
    private String payload;

    /**
     * The HMAC signature sent in the X-Webhook-Signature header.
     */
    private String signature;

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
     * Marks this delivery as successfully completed.
     */
    public void markDelivered(int httpStatus, String response, long durationMs) {
        this.status = "DELIVERED";
        this.httpStatusCode = httpStatus;
        this.responseBody = truncate(response, 2000);
        this.durationMs = durationMs;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Marks this delivery as failed with an error.
     */
    public void markFailed(String error, long durationMs) {
        this.status = "FAILED";
        this.errorMessage = error;
        this.durationMs = durationMs;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Marks this delivery as retrying after a failure.
     */
    public void markRetrying(int attemptNum) {
        this.status = "RETRYING";
        this.attemptNumber = attemptNum;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Marks this delivery as exhausted all retries.
     */
    public void markExhausted(String lastError) {
        this.status = "EXHAUSTED";
        this.errorMessage = lastError;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Returns true if this delivery is in a terminal state.
     */
    public boolean isTerminal() {
        return "DELIVERED".equals(status) || "EXHAUSTED".equals(status);
    }

    /**
     * Returns the retry delay in milliseconds based on attempt number.
     * Uses exponential backoff: 1s, 2s, 4s, 8s, ...
     */
    public long getRetryDelayMs() {
        return (long) Math.pow(2, attemptNumber - 1) * 1000;
    }

    private static String truncate(String value, int maxLength) {
        if (value == null) return null;
        return value.length() <= maxLength ? value : value.substring(0, maxLength) + "...[truncated]";
    }
}
