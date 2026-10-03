package com.email.writer;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

/**
 * DTO representing the complete result of an email categorization analysis.
 * Contains the category assignment, confidence, tags, sentiment, urgency,
 * and extracted metadata.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailCategorizationResult {

    /**
     * Primary detected category.
     */
    private String category;

    /**
     * Secondary category if applicable.
     */
    private String secondaryCategory;

    /**
     * Confidence score (0.0 - 1.0).
     */
    private double confidence;

    /**
     * Auto-extracted tags.
     */
    private List<String> tags;

    /**
     * Detected sentiment.
     */
    private String sentiment;

    /**
     * Urgency level.
     */
    private String urgency;

    /**
     * Detected language.
     */
    private String detectedLanguage;

    /**
     * Word count of the analyzed content.
     */
    private int wordCount;

    /**
     * Whether the email contains questions.
     */
    private boolean containsQuestions;

    /**
     * Whether attachments are mentioned.
     */
    private boolean mentionsAttachments;

    /**
     * Whether a deadline is mentioned.
     */
    private boolean mentionsDeadline;

    /**
     * Detected tone.
     */
    private String detectedTone;

    /**
     * Human-readable explanation of why this category was chosen.
     */
    private String explanation;

    /**
     * The ID of the persisted EmailCategory record.
     */
    private Long persistedId;
}
