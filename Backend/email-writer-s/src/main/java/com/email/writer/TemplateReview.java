package com.email.writer;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;

/**
 * Entity representing a review entry in the template approval workflow.
 * Supports multi-reviewer approval with comments and ratings.
 */
@Entity
@Table(name = "template_reviews", indexes = {
        @Index(name = "idx_tr_version_id", columnList = "templateVersionId"),
        @Index(name = "idx_tr_reviewer", columnList = "reviewer"),
        @Index(name = "idx_tr_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplateReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Reference to the TemplateVersion being reviewed.
     */
    @Column(nullable = false)
    private Long templateVersionId;

    /**
     * Reference to the parent template.
     */
    @Column(nullable = false)
    private Long templateId;

    /**
     * Reviewer name/ID.
     */
    @Column(nullable = false, length = 100)
    private String reviewer;

    /**
     * Review status: PENDING, APPROVED, REJECTED, CHANGES_REQUESTED.
     */
    @Column(nullable = false, length = 25)
    @Builder.Default
    private String status = "PENDING";

    /**
     * Quality rating (1-5).
     */
    private Integer rating;

    /**
     * Reviewer's comments.
     */
    @Column(columnDefinition = "TEXT")
    private String comments;

    /**
     * Specific suggestions for improvements (JSON array format).
     */
    @Column(columnDefinition = "TEXT")
    private String suggestions;

    /**
     * Whether this review resolved all previous change requests.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean resolvedAllFeedback = false;

    /**
     * Time spent reviewing in seconds.
     */
    private Long reviewDurationSeconds;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
