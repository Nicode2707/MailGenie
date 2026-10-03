package com.email.writer;

import lombok.Data;

/**
 * Request DTO for submitting a template version review.
 */
@Data
public class TemplateReviewRequest {
    private String reviewer;
    private String status; // APPROVED, REJECTED, CHANGES_REQUESTED
    private Integer rating;
    private String comments;
    private String suggestions;
    private Boolean resolvedAllFeedback;
    private Long reviewDurationSeconds;
}
