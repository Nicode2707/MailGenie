package com.email.writer;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Stores the result of an email categorization analysis.
 * Each record captures the detected category, confidence score,
 * extracted tags, and sentiment for a given email content.
 */
@Entity
@Table(name = "email_categories", indexes = {
        @Index(name = "idx_ecategory_type", columnList = "category"),
        @Index(name = "idx_ecategory_sentiment", columnList = "sentiment")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The original email content that was categorized.
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String originalContent;

    /**
     * Primary detected category: INQUIRY, COMPLAINT, FOLLOW_UP, MEETING,
     * APPRECIATION, REQUEST, DECLINE, INTRODUCTION, FEEDBACK, OTHER.
     */
    @Column(nullable = false)
    private String category;

    /**
     * Secondary/alternate category if confidence is borderline.
     */
    private String secondaryCategory;

    /**
     * Confidence score for the primary category (0.0 - 1.0).
     */
    @Column(nullable = false)
    private double confidence;

    /**
     * Comma-separated auto-extracted tags (e.g., "deadline,urgent,project-alpha").
     */
    @Column(columnDefinition = "TEXT")
    private String tags;

    /**
     * Detected sentiment: POSITIVE, NEGATIVE, NEUTRAL, MIXED.
     */
    @Column(nullable = false)
    @Builder.Default
    private String sentiment = "NEUTRAL";

    /**
     * Urgency level: HIGH, MEDIUM, LOW, NONE.
     */
    @Builder.Default
    private String urgency = "NONE";

    /**
     * Detected language of the email.
     */
    private String detectedLanguage;

    /**
     * Word count of the original content.
     */
    private int wordCount;

    /**
     * Whether the email contains questions (detected via ? marks).
     */
    @Builder.Default
    private boolean containsQuestions = false;

    /**
     * Whether the email contains attachments references.
     */
    @Builder.Default
    private boolean mentionsAttachments = false;

    /**
     * Whether a deadline or date is mentioned.
     */
    @Builder.Default
    private boolean mentionsDeadline = false;

    /**
     * The tone detected (matches with the existing tone system).
     */
    private String detectedTone;

    /**
     * Optional label assigned by the user.
     */
    private String userLabel;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * Returns the tags as a list.
     */
    public java.util.List<String> getTagList() {
        if (tags == null || tags.trim().isEmpty()) {
            return java.util.Collections.emptyList();
        }
        String[] parts = tags.split(",");
        java.util.List<String> result = new java.util.ArrayList<>();
        for (String tag : parts) {
            String trimmed = tag.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    /**
     * Returns true if confidence is high (>= 0.7).
     */
    public boolean isHighConfidence() {
        return confidence >= 0.7;
    }

    /**
     * Returns true if this is an actionable email (inquiry, request, meeting).
     */
    public boolean isActionable() {
        return "INQUIRY".equals(category) || "REQUEST".equals(category)
                || "MEETING".equals(category) || "FOLLOW_UP".equals(category);
    }
}
