package com.email.writer;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.Map;

/**
 * Response DTO for system-wide deliverability and reputation statistics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReputationStats {
    private long totalDomainsTracked;
    private long healthyDomains;
    private long atRiskDomains;
    private long criticalDomains;
    private long totalEmailsTracked;
    private long totalDelivered;
    private long totalBounced;
    private long totalSpamComplaints;
    private Double systemWideDeliverabilityRate;
    private Double systemWideBounceRate;
    private Double systemWideSpamComplaintRate;
    private Double avgReputationScore;
    private long totalBlocklistWarnings;
    private Map<String, Long> healthGradeDistribution;
    private Map<String, Long> riskLevelDistribution;
    private Map<String, Long> topBlocklists;
}
