package com.email.writer;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;

/**
 * Entity representing a specific version of an email template.
 * Tracks the full history of changes with metadata for audit and rollback.
 */
@Entity
@Table(name = "template_versions", indexes = {
        @Index(name = "idx_tv_template_id", columnList = "templateId"),
        @Index(name = "idx_tv_version_number", columnList = "templateId, versionNumber"),
        @Index(name = "idx_tv_author", columnList = "author"),
        @Index(name = "idx_tv_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplateVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Reference to the parent EmailTemplate.
     */
    @Column(nullable = false)
    private Long templateId;

    /**
     * Sequential version number (1, 2, 3, ...).
     */
    @Column(nullable = false)
    private Integer versionNumber;

    /**
     * Snapshot of the template title at this version.
     */
    @Column(nullable = false, length = 150)
    private String title;

    /**
     * Snapshot of the template body at this version.
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String body;

    /**
     * Optional description of what changed in this version.
     */
    @Column(columnDefinition = "TEXT")
    private String changeDescription;

    /**
     * Who authored this version.
     */
    @Column(length = 100)
    private String author;

    /**
     * Status: DRAFT, PENDING_REVIEW, APPROVED, REJECTED, PUBLISHED, ARCHIVED.
     */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "DRAFT";

    /**
     * Whether this is the currently active/published version.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean isCurrent = false;

    /**
     * Number of lines added compared to the previous version.
     */
    @Builder.Default
    private Integer linesAdded = 0;

    /**
     * Number of lines removed compared to the previous version.
     */
    @Builder.Default
    private Integer linesRemoved = 0;

    /**
     * Number of characters in the body content.
     */
    private Integer characterCount;

    /**
     * Tags/labels for categorization (comma-separated).
     */
    @Column(length = 500)
    private String tags;

    /**
     * Lock token if the template is being edited (optimistic locking).
     */
    @Column(length = 100)
    private String lockToken;

    /**
     * When the lock expires.
     */
    private LocalDateTime lockExpiresAt;

    /**
     * Who last reviewed this version.
     */
    @Column(length = 100)
    private String reviewedBy;

    /**
     * Reviewer's comments.
     */
    @Column(columnDefinition = "TEXT")
    private String reviewComments;

    /**
     * When the review was completed.
     */
    private LocalDateTime reviewedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (body != null) {
            characterCount = body.length();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        if (body != null) {
            characterCount = body.length();
        }
    }

    /**
     * Check if this version's lock is still active.
     */
    @Transient
    public boolean isLocked() {
        return lockToken != null && lockExpiresAt != null && lockExpiresAt.isAfter(LocalDateTime.now());
    }

    /**
     * Calculate a simple line-by-line diff summary against another version.
     */
    @Transient
    public TemplateDiffResult computeDiff(TemplateVersion previous) {
        if (previous == null) {
            int lineCount = body != null ? body.split("\n").length : 0;
            return TemplateDiffResult.builder()
                    .addedLines(lineCount)
                    .removedLines(0)
                    .modifiedLines(0)
                    .unchangedLines(0)
                    .similarityPercent(0.0)
                    .diffType("CREATED")
                    .build();
        }

        String[] currentLines = body != null ? body.split("\n") : new String[0];
        String[] previousLines = previous.getBody() != null ? previous.getBody().split("\n") : new String[0];

        java.util.Set<String> prevSet = new java.util.LinkedHashSet<>(java.util.Arrays.asList(previousLines));
        java.util.Set<String> currSet = new java.util.LinkedHashSet<>(java.util.Arrays.asList(currentLines));

        int added = 0, removed = 0, unchanged = 0;
        for (String line : currentLines) {
            if (prevSet.contains(line)) unchanged++;
            else added++;
        }
        for (String line : previousLines) {
            if (!currSet.contains(line)) removed++;
        }

        int total = Math.max(added + removed + unchanged, 1);
        double similarity = Math.round(((double) unchanged / total) * 10000.0) / 100.0;

        String diffType;
        if (added == 0 && removed == 0) diffType = "UNCHANGED";
        else if (removed == 0) diffType = "ADDITIVE";
        else if (added == 0) diffType = "REDUCTIVE";
        else diffType = "MODIFIED";

        return TemplateDiffResult.builder()
                .addedLines(added)
                .removedLines(removed)
                .modifiedLines(Math.min(added, removed))
                .unchangedLines(unchanged)
                .similarityPercent(similarity)
                .diffType(diffType)
                .build();
    }
}
