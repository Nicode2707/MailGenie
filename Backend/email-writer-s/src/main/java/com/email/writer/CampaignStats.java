package com.email.writer;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.Map;

/**
 * Response DTO for system-wide campaign analytics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignStats {
    private long totalCampaigns;
    private long activeCampaigns;
    private long completedCampaigns;
    private long draftCampaigns;
    private long totalVariants;
    private long totalEvents;
    private long totalEmailsSent;
    private Double overallOpenRate;
    private Double overallClickRate;
    private Double overallReplyRate;
    private Double totalRevenue;
    private Map<String, Long> typeBreakdown;
    private Map<String, Long> statusBreakdown;
    private Map<String, Long> eventTypeTotals;
}
