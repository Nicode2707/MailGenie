package com.email.writer;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for comprehensive campaign analytics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignAnalyticsResponse {
    private Long campaignId;
    private String campaignName;
    private String status;
    private Long totalRecipients;
    private Integer variantCount;
    private Long winningVariantId;
    private Double confidenceLevel;
    private List<VariantAnalytics> variantAnalytics;
    private Map<String, Long> eventTypeBreakdown;
    private Map<String, Long> deviceBreakdown;
    private Map<String, Long> regionBreakdown;
    private Double totalRevenue;
    private Long totalEvents;
    private String recommendation;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VariantAnalytics {
        private Long variantId;
        private String label;
        private String subject;
        private String tone;
        private Double trafficPercent;
        private Long sentCount;
        private Long openCount;
        private Long clickCount;
        private Long replyCount;
        private Long conversionCount;
        private Long unsubscribeCount;
        private Double openRate;
        private Double clickRate;
        private Double replyRate;
        private Double conversionRate;
        private Double engagementRate;
        private Double revenuePerEmail;
        private Double totalRevenue;
        private Boolean isWinner;
        private Double confidenceScore;
    }
}
