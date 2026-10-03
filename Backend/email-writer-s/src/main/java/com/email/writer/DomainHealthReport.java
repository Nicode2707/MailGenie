package com.email.writer;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for comprehensive domain health and deliverability report.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DomainHealthReport {
    private String domain;
    private Double reputationScore;
    private String healthGrade;
    private String riskLevel;
    private Long totalSent;
    private Long totalDelivered;
    private Double deliverabilityRate;
    private Double bounceRate;
    private Double spamComplaintRate;
    private Double openRate;
    private Double clickRate;
    private Double avgSpamScore;
    private Double spfPassRate;
    private Double dkimPassRate;
    private Double dmarcPassRate;
    private Integer streakDays;
    private Integer activeBlocklistCount;
    private List<String> blocklistWarnings;
    private Map<String, Long> eventTypeBreakdown;
    private Map<String, Long> bounceBreakdown;
    private Map<String, Long> recipientDomainBreakdown;
    private Map<String, Double> dailyDeliverabilityTrend;
    private Map<String, Long> smtpCodeDistribution;
    private List<String> recommendations;
    private Boolean isHealthy;
    private Boolean isAtRisk;
}
