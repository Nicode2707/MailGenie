package com.email.writer;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Stores a snapshot of a draft at a specific point in the generation lifecycle.
 * Each time the AI generates or the user modifies a draft, a new version is created.
 * Users can compare, revert, or browse the full version history.
 */
@Entity
@Table(name = "draft_versions", indexes = {
        @Index(name = "idx_draft_session", columnList = "sessionId"),
        @Index(name = "idx_draft_active", columnList = "sessionId,active")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DraftVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Groups related drafts into a session (e.g., one email reply workflow).
     * Generated as a UUID at session start.
     */
    @Column(nullable = false)
    private String sessionId;

    /**
     * Monotonically increasing version number within the session (1, 2, 3...).
     */
    @Column(nullable = false)
    private int versionNumber;

    /**
     * The draft text content at this version.
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    /**
     * How this version was created: AI_GENERATED, USER_EDITED, REVERTED.
     */
    @Column(nullable = false)
    @Builder.Default
    private String source = "AI_GENERATED";

    /**
     * The tone used for this draft.
     */
    private String tone;

    /**
     * The language used for this draft.
     */
    private String language;

    /**
     * The provider used for generation.
     */
    private String provider;

    /**
     * The original email content this draft is responding to.
     */
    @Column(columnDefinition = "TEXT")
    private String originalEmailContent;

    /**
     * The generated reply text (final output for this version).
     */
    @Column(columnDefinition = "TEXT")
    private String generatedReply;

    /**
     * Whether this version is the currently active/selected one.
     */
    @Builder.Default
    private boolean active = true;

    /**
     * Optional user label/name for this version.
     */
    private String label;

    /**
     * Word count of the content at this version.
     */
    private int wordCount;

    /**
     * Character count of the content at this version.
     */
    private int charCount;

    /**
     * Time taken to generate this version (for AI generations), in milliseconds.
     */
    private Long generationTimeMs;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * Calculates word and character counts from the content.
     */
    public void calculateStats() {
        if (content == null || content.isEmpty()) {
            wordCount = 0;
            charCount = 0;
            return;
        }
        charCount = content.length();
        String[] words = content.trim().split("\\s+");
        wordCount = words.length;
    }

    /**
     * Returns a truncated preview of the content.
     */
    public String getPreview(int maxLength) {
        if (content == null) return "";
        if (content.length() <= maxLength) return content;
        return content.substring(0, maxLength) + "...";
    }

    /**
     * Returns true if this version was AI-generated.
     */
    public boolean isAiGenerated() {
        return "AI_GENERATED".equals(source);
    }

    /**
     * Returns true if this version was created by a user edit.
     */
    public boolean isUserEdited() {
        return "USER_EDITED".equals(source);
    }

    /**
     * Returns true if this version was created by reverting.
     */
    public boolean isReverted() {
        return "REVERTED".equals(source);
    }
}
