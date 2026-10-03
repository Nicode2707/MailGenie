package com.email.writer;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.Map;

/**
 * Response DTO for template versioning and collaboration statistics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateVersionStats {
    private long totalTemplates;
    private long totalVersions;
    private long totalReviews;
    private long pendingReviews;
    private long approvedVersions;
    private long rejectedVersions;
    private long draftVersions;
    private long lockedVersions;
    private Double avgReviewsPerVersion;
    private Double avgRatingAcrossAll;
    private Map<String, Long> authorVersionCounts;
    private Map<String, Long> reviewerCounts;
    private Map<String, Long> statusBreakdown;
    private long avgVersionCountPerTemplate;
}
