package com.email.writer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service managing email A/B split test campaigns.
 * Creates test campaigns, analyzes each variant through the existing
 * quality pipeline, scores them, and determines winners.
 */
@Service
public class EmailABTestService {

    @Autowired
    private EmailReadabilityAnalyzer readabilityAnalyzer;

    @Autowired
    private EmailSpamComplianceChecker spamChecker;

    @Autowired
    private EmailSubjectLineOptimizer subjectOptimizer;

    @Autowired
    private EmailToneSentimentService toneService;

    // In-memory campaign store (would be DB-backed in production)
    private final List<Map<String, Object>> campaigns = new ArrayList<>();

    /**
     * Create a new A/B test campaign with the given variants.
     */
    public Map<String, Object> createCampaign(String campaignName, String testType, List<EmailABTestVariant> variants) {
        String campaignId = "AB-" + UUID.randomUUID().toString().substring(0, 8);

        // Analyze each variant
        List<EmailABTestVariant> analyzed = variants.stream()
                .map(this::analyzeVariant)
                .collect(Collectors.toList());

        // Score and rank
        scoreAndRankVariants(analyzed);

        Map<String, Object> campaign = new LinkedHashMap<>();
        campaign.put("campaignId", campaignId);
        campaign.put("campaignName", campaignName);
        campaign.put("testType", testType); // "subject_line", "body_content", "full_email"
        campaign.put("variants", analyzed);
        campaign.put("variantCount", analyzed.size());
        campaign.put("createdAt", LocalDateTime.now().toString());
        campaign.put("status", "COMPLETED");

        campaigns.add(0, campaign);
        return campaign;
    }

    /**
     * Retrieve all campaigns sorted by creation date descending.
     */
    public List<Map<String, Object>> getAllCampaigns() {
        return new ArrayList<>(campaigns);
    }

    /**
     * Retrieve a single campaign by ID.
     */
    public Optional<Map<String, Object>> getCampaignById(String campaignId) {
        return campaigns.stream()
                .filter(c -> campaignId.equals(c.get("campaignId")))
                .findFirst();
    }

    /**
     * Delete a campaign by ID.
     */
    public boolean deleteCampaign(String campaignId) {
        return campaigns.removeIf(c -> campaignId.equals(c.get("campaignId")));
    }

    /**
     * Get summary statistics across all campaigns.
     */
    public Map<String, Object> getTestStatistics() {
        int totalCampaigns = campaigns.size();
        int totalVariants = campaigns.stream()
                .mapToInt(c -> (int) c.getOrDefault("variantCount", 0))
                .sum();

        double avgCompositeScore = campaigns.stream()
                .flatMap(c -> ((List<EmailABTestVariant>) c.get("variants")).stream())
                .mapToDouble(EmailABTestVariant::getCompositeScore)
                .average()
                .orElse(0.0);

        Map<String, Long> testTypeDistribution = campaigns.stream()
                .collect(Collectors.groupingBy(
                        c -> (String) c.getOrDefault("testType", "unknown"),
                        Collectors.counting()
                ));

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalCampaigns", totalCampaigns);
        stats.put("totalVariantsAnalyzed", totalVariants);
        stats.put("averageCompositeScore", Math.round(avgCompositeScore * 10.0) / 10.0);
        stats.put("testTypeDistribution", testTypeDistribution);
        stats.put("lastUpdated", LocalDateTime.now().toString());

        return stats;
    }

    // ── Private Helpers ─────────────────────────────────────

    /**
     * Analyze a single variant through the full quality pipeline.
     */
    private EmailABTestVariant analyzeVariant(EmailABTestVariant variant) {
        variant.setReadabilityMetrics(readabilityAnalyzer.analyzeReadability(variant.getBodyContent()));
        variant.setSpamMetrics(spamChecker.checkSpamScore(variant.getBodyContent()));
        variant.setSubjectMetrics(subjectOptimizer.optimizeSubjectLine(variant.getSubjectLine()));
        variant.setToneMetrics(toneService.analyzeEmail(variant.getBodyContent()));
        variant.setAnalyzedAt(LocalDateTime.now());
        return variant;
    }

    /**
     * Compute composite scores for all variants and assign verdicts.
     * Composite = 30% readability + 25% (100 - spam) + 25% subject + 20% tone sentiment
     */
    private void scoreAndRankVariants(List<EmailABTestVariant> variants) {
        for (EmailABTestVariant v : variants) {
            double readability = (double) v.getReadabilityMetrics().getOrDefault("fleschKincaidScore", 0.0);
            double spam = (double) v.getSpamMetrics().getOrDefault("spamScore", 0.0);
            double subject = ((Number) v.getSubjectMetrics().getOrDefault("subjectScore", 0)).doubleValue();
            double toneSentiment = 0.0;
            if (v.getToneMetrics() != null && v.getToneMetrics().containsKey("sentimentScore")) {
                toneSentiment = ((Number) v.getToneMetrics().get("sentimentScore")).doubleValue();
                // Sentiment is -1 to 1, normalize to 0-100
                toneSentiment = Math.max(0, (toneSentiment + 1.0) * 50.0);
            }

            double composite = Math.round(((readability * 0.30) + ((100.0 - spam) * 0.25) + (subject * 0.25) + (toneSentiment * 0.20)) * 10.0) / 10.0;
            v.setCompositeScore(composite);
        }

        // Sort descending by composite score
        variants.sort((a, b) -> Double.compare(b.getCompositeScore(), a.getCompositeScore()));

        // Assign verdicts
        for (int i = 0; i < variants.size(); i++) {
            if (i == 0) {
                variants.get(i).setVerdict("WINNER");
            } else if (i == 1 && variants.size() > 2) {
                variants.get(i).setVerdict("RUNNER_UP");
            } else {
                variants.get(i).setVerdict("UNDERPERFORMER");
            }
        }

        // If only 2 variants, second one is runner-up
        if (variants.size() == 2) {
            variants.get(1).setVerdict("RUNNER_UP");
        }
    }
}
