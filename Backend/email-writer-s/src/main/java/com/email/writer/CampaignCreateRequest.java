package com.email.writer;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Request DTO for creating or updating an email campaign.
 */
@Data
public class CampaignCreateRequest {
    private String name;
    private String description;
    private String campaignType;
    private Long totalRecipients;
    private Double significanceThreshold;
    private String primaryMetric;
    private String targetSegment;
    private String provider;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer testDurationHours;
    private Integer minSampleSize;
    private String tags;
    private String createdBy;
    private List<VariantRequest> variants;
}
