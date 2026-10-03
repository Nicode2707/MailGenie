package com.email.writer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Enterprise service for email campaign management, A/B testing,
 * metric tracking, winner selection, and comprehensive analytics.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CampaignAnalyticsService {

    private final EmailCampaignRepository campaignRepository;
    private final CampaignVariantRepository variantRepository;
    private final CampaignMetricRepository metricRepository;

    // ─── Campaign CRUD ─────────────────────────────────────────────────

    /**
     * Create a new campaign with variants.
     */
    @Transactional
    public EmailCampaign createCampaign(CampaignCreateRequest request) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Campaign name is required.");
        }

        EmailCampaign campaign = EmailCampaign.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .campaignType(request.getCampaignType() != null ? request.getCampaignType() : "AB_TEST")
                .status("DRAFT")
                .totalRecipients(request.getTotalRecipients() != null ? request.getTotalRecipients() : 0L)
                .significanceThreshold(request.getSignificanceThreshold() != null ? request.getSignificanceThreshold() : 0.05)
                .primaryMetric(request.getPrimaryMetric() != null ? request.getPrimaryMetric() : "CLICK_RATE")
                .targetSegment(request.getTargetSegment() != null ? request.getTargetSegment() : "all")
                .provider(request.getProvider())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .testDurationHours(request.getTestDurationHours() != null ? request.getTestDurationHours() : 48)
                .minSampleSize(request.getMinSampleSize() != null ? request.getMinSampleSize() : 100)
                .tags(request.getTags())
                .createdBy(request.getCreatedBy())
                .build();

        EmailCampaign saved = campaignRepository.save(campaign);

        // Create variants
        if (request.getVariants() != null) {
            for (VariantRequest vr : request.getVariants()) {
                CampaignVariant variant = CampaignVariant.builder()
                        .campaignId(saved.getId())
                        .label(vr.getLabel() != null ? vr.getLabel() : "Variant-" + (variantRepository.findByCampaignIdOrderByLabelAsc(saved.getId()).size() + 1))
                        .subject(vr.getSubject() != null ? vr.getSubject() : "")
                        .body(vr.getBody() != null ? vr.getBody() : "")
                        .tone(vr.getTone())
                        .language(vr.getLanguage() != null ? vr.getLanguage() : "English")
                        .trafficPercent(vr.getTrafficPercent() != null ? vr.getTrafficPercent() : 50.0)
                        .notes(vr.getNotes())
                        .generationPrompt(vr.getGenerationPrompt())
                        .build();
                variantRepository.save(variant);
            }
            saved.setVariantCount(variantRepository.findByCampaignIdOrderByLabelAsc(saved.getId()).size());
            campaignRepository.save(saved);
        }

        log.info("Created campaign '{}' with {} variants", saved.getName(), saved.getVariantCount());
        return saved;
    }

    /**
     * Get a campaign by ID.
     */
    public EmailCampaign getCampaign(Long id) {
        return campaignRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Campaign not found: " + id));
    }

    /**
     * List campaigns by status.
     */
    public List<EmailCampaign> listCampaigns(String status) {
        if (status != null && !status.trim().isEmpty()) {
            return campaignRepository.findByStatusOrderByCreatedAtDesc(status.trim());
        }
        return campaignRepository.findAll(org.springframework.data.domain.Sort.by("createdAt").descending());
    }

    /**
     * Delete a campaign and its variants.
     */
    @Transactional
    public boolean deleteCampaign(Long id) {
        if (!campaignRepository.existsById(id)) return false;
        List<CampaignVariant> variants = variantRepository.findByCampaignIdOrderByLabelAsc(id);
        variantRepository.deleteAll(variants);
        campaignRepository.deleteById(id);
        log.info("Deleted campaign {} and {} variants", id, variants.size());
        return true;
    }

    // ─── Campaign Lifecycle ────────────────────────────────────────────

    /**
     * Start a campaign — transition from DRAFT to RUNNING.
     */
    @Transactional
    public EmailCampaign startCampaign(Long id) {
        EmailCampaign campaign = getCampaign(id);
        if (!"DRAFT".equals(campaign.getStatus()) && !"PAUSED".equals(campaign.getStatus())) {
            throw new IllegalStateException("Campaign can only be started from DRAFT or PAUSED status.");
        }
        if (campaign.getVariantCount() < 2) {
            throw new IllegalStateException("A/B test campaigns require at least 2 variants.");
        }
        campaign.setStatus("RUNNING");
        if (campaign.getStartDate() == null) {
            campaign.setStartDate(LocalDateTime.now());
        }
        if (campaign.getEndDate() == null && campaign.getTestDurationHours() != null) {
            campaign.setEndDate(campaign.getStartDate().plusHours(campaign.getTestDurationHours()));
        }
        return campaignRepository.save(campaign);
    }

    /**
     * Pause a running campaign.
     */
    @Transactional
    public EmailCampaign pauseCampaign(Long id) {
        EmailCampaign campaign = getCampaign(id);
        if (!"RUNNING".equals(campaign.getStatus())) {
            throw new IllegalStateException("Only RUNNING campaigns can be paused.");
        }
        campaign.setStatus("PAUSED");
        return campaignRepository.save(campaign);
    }

    /**
     * Complete a campaign and auto-select a winner.
     */
    @Transactional
    public EmailCampaign completeCampaign(Long id) {
        EmailCampaign campaign = getCampaign(id);
        campaign.setStatus("COMPLETED");
        campaign.setEndDate(LocalDateTime.now());

        // Auto-select winner
        selectWinner(campaign);

        return campaignRepository.save(campaign);
    }

    // ─── Metric Recording ──────────────────────────────────────────────

    /**
     * Record a metric event for a campaign variant.
     */
    @Transactional
    public CampaignMetric recordEvent(Long campaignId, MetricEventRequest request) {
        if (request.getVariantId() == null) {
            throw new IllegalArgumentException("Variant ID is required.");
        }
        if (request.getEventType() == null || request.getEventType().trim().isEmpty()) {
            throw new IllegalArgumentException("Event type is required.");
        }

        CampaignVariant variant = variantRepository.findById(request.getVariantId())
                .orElseThrow(() -> new NoSuchElementException("Variant not found: " + request.getVariantId()));

        CampaignMetric metric = CampaignMetric.builder()
                .campaignId(campaignId)
                .variantId(request.getVariantId())
                .eventType(request.getEventType().toUpperCase())
                .recipientEmail(request.getRecipientEmail())
                .deviceType(request.getDeviceType())
                .region(request.getRegion())
                .timeToEventMs(request.getTimeToEventMs())
                .revenue(request.getRevenue() != null ? request.getRevenue() : 0.0)
                .metadata(request.getMetadata())
                .build();

        CampaignMetric saved = metricRepository.save(metric);

        // Update variant counters
        switch (saved.getEventType()) {
            case "SENT": variant.setSentCount(variant.getSentCount() + 1); break;
            case "OPEN": variant.setOpenCount(variant.getOpenCount() + 1); break;
            case "CLICK": variant.setClickCount(variant.getClickCount() + 1); break;
            case "REPLY": variant.setReplyCount(variant.getReplyCount() + 1); break;
            case "CONVERSION": variant.setConversionCount(variant.getConversionCount() + 1); break;
            case "UNSUBSCRIBE": variant.setUnsubscribeCount(variant.getUnsubscribeCount() + 1); break;
            case "BOUNCE": variant.setBounceCount(variant.getBounceCount() + 1); break;
        }
        variantRepository.save(variant);

        return saved;
    }

    /**
     * Record multiple events in bulk.
     */
    @Transactional
    public int recordEventsBulk(Long campaignId, List<MetricEventRequest> events) {
        int recorded = 0;
        for (MetricEventRequest event : events) {
            try {
                recordEvent(campaignId, event);
                recorded++;
            } catch (Exception e) {
                log.warn("Failed to record metric event: {}", e.getMessage());
            }
        }
        return recorded;
    }

    // ─── A/B Test Winner Selection ─────────────────────────────────────

    /**
     * Manually select a winning variant.
     */
    @Transactional
    public CampaignVariant selectWinnerManually(Long campaignId, Long variantId) {
        EmailCampaign campaign = getCampaign(campaignId);

        // Clear existing winner
        List<CampaignVariant> currentWinners = variantRepository.findByCampaignIdAndIsWinnerTrue(campaignId);
        for (CampaignVariant w : currentWinners) {
            w.setIsWinner(false);
            variantRepository.save(w);
        }

        CampaignVariant winner = variantRepository.findById(variantId)
                .orElseThrow(() -> new NoSuchElementException("Variant not found: " + variantId));

        winner.setIsWinner(true);
        winner.setConfidenceScore(100.0);
        variantRepository.save(winner);

        campaign.setWinningVariantId(variantId);
        campaign.setConfidenceLevel(100.0);
        campaignRepository.save(campaign);

        log.info("Manually selected variant {} as winner for campaign {}", variantId, campaignId);
        return winner;
    }

    /**
     * Auto-select the winning variant based on the primary metric.
     * Uses a simplified statistical comparison.
     */
    @Transactional
    public void selectWinner(EmailCampaign campaign) {
        List<CampaignVariant> variants = variantRepository.findByCampaignIdOrderByLabelAsc(campaign.getId());
        if (variants.isEmpty()) return;

        String metric = campaign.getPrimaryMetric() != null ? campaign.getPrimaryMetric() : "CLICK_RATE";

        CampaignVariant best = variants.stream()
                .max(Comparator.comparingDouble(v -> getMetricValue(v, metric)))
                .orElse(variants.get(0));

        double bestRate = getMetricValue(best, metric);

        // Calculate confidence using simplified z-test
        double confidence = calculateConfidence(variants, metric);

        best.setIsWinner(true);
        best.setConfidenceScore(confidence);
        variantRepository.save(best);

        campaign.setWinningVariantId(best.getId());
        campaign.setConfidenceLevel(confidence);
        campaignRepository.save(campaign);

        log.info("Auto-selected variant '{}' (id={}) as winner for campaign {} with confidence {:.2f}%",
                best.getLabel(), best.getId(), campaign.getId(), confidence);
    }

    // ─── Analytics ─────────────────────────────────────────────────────

    /**
     * Get comprehensive analytics for a campaign.
     */
    public CampaignAnalyticsResponse getCampaignAnalytics(Long campaignId) {
        EmailCampaign campaign = getCampaign(campaignId);
        List<CampaignVariant> variants = variantRepository.findByCampaignIdOrderByLabelAsc(campaignId);

        // Build variant analytics
        List<CampaignAnalyticsResponse.VariantAnalytics> variantAnalytics = variants.stream()
                .map(this::buildVariantAnalytics)
                .collect(Collectors.toList());

        // Event type breakdown
        List<Object[]> eventBreakdown = metricRepository.countByEventTypeForCampaign(campaignId);
        Map<String, Long> eventTypeMap = eventBreakdown.stream()
                .collect(Collectors.toMap(r -> (String) r[0], r -> (Long) r[1],
                        (a, b) -> a, LinkedHashMap::new));

        // Device breakdown
        List<Object[]> deviceBreakdown = metricRepository.countByDeviceType(campaignId);
        Map<String, Long> deviceMap = deviceBreakdown.stream()
                .collect(Collectors.toMap(r -> (String) r[0], r -> (Long) r[1],
                        (a, b) -> a, LinkedHashMap::new));

        // Region breakdown
        List<Object[]> regionBreakdown = metricRepository.countByRegion(campaignId);
        Map<String, Long> regionMap = regionBreakdown.stream()
                .collect(Collectors.toMap(r -> (String) r[0], r -> (Long) r[1],
                        (a, b) -> a, LinkedHashMap::new));

        Double totalRevenue = metricRepository.totalRevenueByCampaign(campaignId);
        long totalEvents = metricRepository.countByCampaignIdAndEventType(campaignId, "SENT")
                + metricRepository.countByCampaignIdAndEventType(campaignId, "OPEN")
                + metricRepository.countByCampaignIdAndEventType(campaignId, "CLICK")
                + metricRepository.countByCampaignIdAndEventType(campaignId, "REPLY")
                + metricRepository.countByCampaignIdAndEventType(campaignId, "CONVERSION");

        String recommendation = generateRecommendation(campaign, variants);

        return CampaignAnalyticsResponse.builder()
                .campaignId(campaign.getId())
                .campaignName(campaign.getName())
                .status(campaign.getStatus())
                .totalRecipients(campaign.getTotalRecipients())
                .variantCount(campaign.getVariantCount())
                .winningVariantId(campaign.getWinningVariantId())
                .confidenceLevel(campaign.getConfidenceLevel())
                .variantAnalytics(variantAnalytics)
                .eventTypeBreakdown(eventTypeMap)
                .deviceBreakdown(deviceMap)
                .regionBreakdown(regionMap)
                .totalRevenue(totalRevenue != null ? totalRevenue : 0.0)
                .totalEvents(totalEvents)
                .recommendation(recommendation)
                .build();
    }

    /**
     * Get system-wide campaign statistics.
     */
    public CampaignStats getSystemStats() {
        List<EmailCampaign> allCampaigns = campaignRepository.findAll();
        List<CampaignVariant> allVariants = variantRepository.findAll();
        List<CampaignMetric> allMetrics = metricRepository.findAll();

        long totalCampaigns = allCampaigns.size();
        long activeCampaigns = allCampaigns.stream().filter(c -> "RUNNING".equals(c.getStatus())).count();
        long completedCampaigns = allCampaigns.stream().filter(c -> "COMPLETED".equals(c.getStatus())).count();
        long draftCampaigns = allCampaigns.stream().filter(c -> "DRAFT".equals(c.getStatus())).count();

        long totalVariants = allVariants.size();
        long totalEvents = allMetrics.size();
        long totalEmailsSent = allVariants.stream().mapToLong(CampaignVariant::getSentCount).sum();

        long totalOpens = allVariants.stream().mapToLong(CampaignVariant::getOpenCount).sum();
        long totalClicks = allVariants.stream().mapToLong(CampaignVariant::getClickCount).sum();
        long totalReplies = allVariants.stream().mapToLong(CampaignVariant::getReplyCount).sum();

        double overallOpenRate = totalEmailsSent > 0 ? Math.round(((double) totalOpens / totalEmailsSent) * 10000.0) / 100.0 : 0.0;
        double overallClickRate = totalEmailsSent > 0 ? Math.round(((double) totalClicks / totalEmailsSent) * 10000.0) / 100.0 : 0.0;
        double overallReplyRate = totalEmailsSent > 0 ? Math.round(((double) totalReplies / totalEmailsSent) * 10000.0) / 100.0 : 0.0;

        Double totalRevenue = allMetrics.stream().mapToDouble(m -> m.getRevenue() != null ? m.getRevenue() : 0.0).sum();

        Map<String, Long> typeBreakdown = allCampaigns.stream()
                .collect(Collectors.groupingBy(EmailCampaign::getCampaignType, Collectors.counting()));
        Map<String, Long> statusBreakdown = allCampaigns.stream()
                .collect(Collectors.groupingBy(EmailCampaign::getStatus, Collectors.counting()));
        Map<String, Long> eventTypeTotals = allMetrics.stream()
                .collect(Collectors.groupingBy(CampaignMetric::getEventType, Collectors.counting()));

        return CampaignStats.builder()
                .totalCampaigns(totalCampaigns)
                .activeCampaigns(activeCampaigns)
                .completedCampaigns(completedCampaigns)
                .draftCampaigns(draftCampaigns)
                .totalVariants(totalVariants)
                .totalEvents(totalEvents)
                .totalEmailsSent(totalEmailsSent)
                .overallOpenRate(overallOpenRate)
                .overallClickRate(overallClickRate)
                .overallReplyRate(overallReplyRate)
                .totalRevenue(totalRevenue)
                .typeBreakdown(typeBreakdown)
                .statusBreakdown(statusBreakdown)
                .eventTypeTotals(eventTypeTotals)
                .build();
    }

    /**
     * Get all variants for a campaign.
     */
    public List<CampaignVariant> getVariants(Long campaignId) {
        return variantRepository.findByCampaignIdOrderByLabelAsc(campaignId);
    }

    /**
     * Get variant-level metrics.
     */
    public List<Object[]> getVariantMetrics(Long variantId) {
        return metricRepository.countByEventTypeForVariant(variantId);
    }

    /**
     * Get time-to-event analytics for a variant.
     */
    public Map<String, Double> getTimeToEventAnalytics(Long variantId) {
        Map<String, Double> result = new LinkedHashMap<>();
        for (String event : List.of("OPEN", "CLICK", "REPLY")) {
            Double avg = metricRepository.averageTimeToEvent(variantId, event);
            result.put("avg" + event + "TimeMs", avg != null ? Math.round(avg * 100.0) / 100.0 : 0.0);
        }
        return result;
    }

    // ─── Internal Helpers ──────────────────────────────────────────────

    private CampaignAnalyticsResponse.VariantAnalytics buildVariantAnalytics(CampaignVariant variant) {
        Double revenue = metricRepository.totalRevenueByVariant(variant.getId());
        return CampaignAnalyticsResponse.VariantAnalytics.builder()
                .variantId(variant.getId())
                .label(variant.getLabel())
                .subject(variant.getSubject())
                .tone(variant.getTone())
                .trafficPercent(variant.getTrafficPercent())
                .sentCount(variant.getSentCount())
                .openCount(variant.getOpenCount())
                .clickCount(variant.getClickCount())
                .replyCount(variant.getReplyCount())
                .conversionCount(variant.getConversionCount())
                .unsubscribeCount(variant.getUnsubscribeCount())
                .openRate(variant.getOpenRate())
                .clickRate(variant.getClickRate())
                .replyRate(variant.getReplyRate())
                .conversionRate(variant.getConversionRate())
                .engagementRate(variant.getEngagementRate())
                .revenuePerEmail(variant.getRevenuePerEmail())
                .totalRevenue(revenue != null ? revenue : 0.0)
                .isWinner(variant.getIsWinner())
                .confidenceScore(variant.getConfidenceScore())
                .build();
    }

    private double getMetricValue(CampaignVariant v, String metric) {
        return switch (metric.toUpperCase()) {
            case "OPEN_RATE" -> v.getOpenRate();
            case "CLICK_RATE" -> v.getClickRate();
            case "REPLY_RATE" -> v.getReplyRate();
            case "CONVERSION_RATE" -> v.getConversionRate();
            case "ENGAGEMENT_RATE" -> v.getEngagementRate();
            default -> v.getClickRate();
        };
    }

    /**
     * Simplified z-test for comparing two proportions.
     */
    private double calculateConfidence(List<CampaignVariant> variants, String metric) {
        if (variants.size() < 2) return 0.0;

        // Sort by metric descending
        CampaignVariant best = variants.stream()
                .max(Comparator.comparingDouble(v -> getMetricValue(v, metric)))
                .orElse(variants.get(0));
        CampaignVariant second = variants.stream()
                .filter(v -> !v.getId().equals(best.getId()))
                .max(Comparator.comparingDouble(v -> getMetricValue(v, metric)))
                .orElse(null);

        if (second == null || best.getSentCount() == 0 || second.getSentCount() == 0) return 0.0;

        double p1 = getMetricValue(best, metric) / 100.0;
        double p2 = getMetricValue(second, metric) / 100.0;
        long n1 = best.getSentCount();
        long n2 = second.getSentCount();

        double pPool = ((p1 * n1) + (p2 * n2)) / (n1 + n2);
        if (pPool == 0 || pPool == 1) return 0.0;

        double se = Math.sqrt(pPool * (1 - pPool) * (1.0 / n1 + 1.0 / n2));
        if (se == 0) return 0.0;

        double z = Math.abs(p1 - p2) / se;

        // Approximate confidence from z-score
        double confidence;
        if (z >= 3.0) confidence = 99.7;
        else if (z >= 2.576) confidence = 99.0;
        else if (z >= 1.96) confidence = 95.0;
        else if (z >= 1.645) confidence = 90.0;
        else if (z >= 1.28) confidence = 80.0;
        else if (z >= 1.0) confidence = 68.0;
        else confidence = z * 68.0;

        return Math.round(confidence * 100.0) / 100.0;
    }

    /**
     * Generate a recommendation based on current campaign state.
     */
    private String generateRecommendation(EmailCampaign campaign, List<CampaignVariant> variants) {
        if ("COMPLETED".equals(campaign.getStatus())) {
            if (campaign.getWinningVariantId() != null) {
                CampaignVariant winner = variants.stream()
                        .filter(v -> v.getId().equals(campaign.getWinningVariantId()))
                        .findFirst().orElse(null);
                if (winner != null) {
                    return "Winner selected: '" + winner.getLabel() + "' with " +
                            campaign.getConfidenceLevel() + "% confidence. " +
                            "Primary metric score: " + getMetricValue(winner, campaign.getPrimaryMetric()) + "%.";
                }
            }
            return "Campaign completed but no clear winner was determined.";
        }

        if ("RUNNING".equals(campaign.getStatus())) {
            if (!campaign.hasMinimumSampleSize()) {
                return "Collecting data. " + campaign.getTotalRecipients() + "/" +
                        campaign.getMinSampleSize() + " minimum samples reached.";
            }
            if (campaign.isTestPeriodComplete()) {
                return "Test period complete with sufficient data. Consider completing the campaign to select a winner.";
            }
            return "Campaign is running. Test period ends " + campaign.getEndDate() + ".";
        }

        return "Campaign is in " + campaign.getStatus() + " status.";
    }
}
