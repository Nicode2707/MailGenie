package com.email.writer;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Request DTO for scheduling a new email.
 * The caller provides the raw email content, desired tone/language,
 * target recipient info, and the desired send time.
 */
@Data
public class ScheduledEmailRequest {

    private String recipientEmail;

    private String recipientName;

    private String emailContent;

    private String tone;

    private String language;

    private String provider;

    private String model;

    private String customInstructions;

    private boolean composeMode;

    /**
     * ISO-8601 local date-time when the email should be processed.
     */
    private LocalDateTime scheduledAt;

    /**
     * Optional label for grouping scheduled emails.
     */
    private String label;

    /**
     * Maximum retry attempts allowed (default 3).
     */
    private int maxAttempts = 3;

    /**
     * Whether to auto-send the generated reply or hold for review.
     */
    private boolean autoSend = false;

    /**
     * Validates that all required fields are present and sensible.
     */
    public boolean isValid() {
        return recipientEmail != null && !recipientEmail.trim().isEmpty()
                && emailContent != null && !emailContent.trim().isEmpty()
                && scheduledAt != null && scheduledAt.isAfter(LocalDateTime.now());
    }
}
