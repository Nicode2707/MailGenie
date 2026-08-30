package com.email.writer;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Entity representing an email that is scheduled for future processing.
 * When the scheduled time arrives, the background processor will pick it up
 * and trigger email generation via the configured LLM provider.
 */
@Entity
@Table(name = "scheduled_emails", indexes = {
        @Index(name = "idx_scheduled_status", columnList = "status"),
        @Index(name = "idx_scheduled_time", columnList = "scheduledAt")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduledEmail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String recipientEmail;

    @Column(nullable = false)
    private String recipientName;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String emailContent;

    private String tone;

    private String language;

    private String provider;

    private String model;

    private String customInstructions;

    private boolean composeMode;

    @Column(columnDefinition = "TEXT")
    private String generatedReply;

    /**
     * ISO-8601 timestamp when the email should be processed.
     */
    @Column(nullable = false)
    private LocalDateTime scheduledAt;

    /**
     * Current lifecycle status: PENDING, PROCESSING, COMPLETED, FAILED, CANCELLED.
     */
    @Column(nullable = false)
    @Builder.Default
    private String status = "PENDING";

    /**
     * Optional user label for grouping / filtering scheduled emails.
     */
    private String label;

    /**
     * Number of processing attempts made so far.
     */
    @Builder.Default
    private int attempts = 0;

    /**
     * Maximum retry attempts allowed before marking as FAILED.
     */
    @Builder.Default
    private int maxAttempts = 3;

    /**
     * Error message from the last failed attempt, if any.
     */
    @Column(columnDefinition = "TEXT")
    private String lastError;

    /**
     * Whether the generated reply should be emailed automatically
     * or held in the queue for manual review.
     */
    @Builder.Default
    private boolean autoSend = false;

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
     * Marks the email as being processed.
     */
    public void markProcessing() {
        this.status = "PROCESSING";
        this.attempts++;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Marks the email as completed with a generated reply.
     */
    public void markCompleted(String reply) {
        this.status = "COMPLETED";
        this.generatedReply = reply;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Marks the email as failed with an error message.
     */
    public void markFailed(String error) {
        this.lastError = error;
        if (this.attempts >= this.maxAttempts) {
            this.status = "FAILED";
        }
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Cancels the scheduled email if it hasn't started processing yet.
     */
    public boolean cancel() {
        if ("PENDING".equals(this.status)) {
            this.status = "CANCELLED";
            this.updatedAt = LocalDateTime.now();
            return true;
        }
        return false;
    }

    /**
     * Returns true if the email is eligible for processing.
     */
    public boolean isEligibleForProcessing() {
        return "PENDING".equals(this.status)
                && attempts < maxAttempts
                && scheduledAt != null
                && !LocalDateTime.now().isBefore(scheduledAt);
    }
}
