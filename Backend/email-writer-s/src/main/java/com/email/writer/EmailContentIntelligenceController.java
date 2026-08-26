package com.email.writer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * REST Controller providing a unified email content intelligence API.
 * Combines Call-To-Action extraction, category classification, engagement
 * prediction, and signature generation into a single analysis endpoint.
 */
@RestController
@RequestMapping("/api/email/intelligence")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class EmailContentIntelligenceController {

    @Autowired
    private EmailCallToActionExtractor ctaExtractor;

    @Autowired
    private EmailCategoryClassifierEngine categoryClassifier;

    @Autowired
    private EmailEngagementPredictorService engagementPredictor;

    @Autowired
    private EmailSignatureAutoGenerator signatureGenerator;

    @Autowired
    private EmailPlaceholderAutoFiller placeholderFiller;

    /**
     * Full content intelligence analysis: CTA, category, engagement, and
     * combined intelligence score in a single request.
     */
    @PostMapping("/analyze")
    public ResponseEntity<Map<String, Object>> fullAnalysis(@RequestBody Map<String, String> payload) {
        String content = payload.getOrDefault("content", "");
        String subject = payload.getOrDefault("subject", "");

        Map<String, Object> cta = ctaExtractor.extractCTA(content);
        Map<String, Object> category = categoryClassifier.classifyCategory(content);
        Map<String, Object> engagement = engagementPredictor.predictEngagement(subject, content);

        double intelligenceScore = computeIntelligenceScore(cta, category, engagement);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ctaAnalysis", cta);
        result.put("categoryClassification", category);
        result.put("engagementPrediction", engagement);
        result.put("intelligenceScore", intelligenceScore);
        result.put("intelligenceGrade", classifyGrade(intelligenceScore));
        result.put("analyzedAt", LocalDateTime.now().toString());

        return ResponseEntity.ok(result);
    }

    /**
     * CTA extraction only.
     */
    @PostMapping("/cta")
    public ResponseEntity<Map<String, Object>> extractCTA(@RequestBody Map<String, String> payload) {
        return ResponseEntity.ok(ctaExtractor.extractCTA(payload.getOrDefault("content", "")));
    }

    /**
     * Category classification only.
     */
    @PostMapping("/category")
    public ResponseEntity<Map<String, Object>> classifyCategory(@RequestBody Map<String, String> payload) {
        return ResponseEntity.ok(categoryClassifier.classifyCategory(payload.getOrDefault("content", "")));
    }

    /**
     * Engagement prediction only.
     */
    @PostMapping("/engagement")
    public ResponseEntity<Map<String, Object>> predictEngagement(@RequestBody Map<String, String> payload) {
        return ResponseEntity.ok(engagementPredictor.predictEngagement(
                payload.getOrDefault("subject", ""),
                payload.getOrDefault("body", "")
        ));
    }

    /**
     * Signature generation.
     */
    @PostMapping("/signature")
    public ResponseEntity<Map<String, Object>> generateSignature(@RequestBody Map<String, String> payload) {
        String result = signatureGenerator.appendSignature(
                payload.getOrDefault("body", ""),
                payload.getOrDefault("senderName", ""),
                payload.getOrDefault("senderTitle", ""),
                payload.getOrDefault("company", "")
        );
        return ResponseEntity.ok(Map.of(
                "bodyWithSignature", result,
                "generatedAt", LocalDateTime.now().toString()
        ));
    }

    /**
     * Placeholder auto-fill.
     */
    @PostMapping("/fill-placeholders")
    public ResponseEntity<Map<String, Object>> fillPlaceholders(@RequestBody Map<String, Object> payload) {
        String template = (String) payload.getOrDefault("template", "");
        @SuppressWarnings("unchecked")
        java.util.Map<String, String> values = (java.util.Map<String, String>) payload.get("values");
        String result = placeholderFiller.populatePlaceholders(template, values);
        return ResponseEntity.ok(Map.of(
                "filledContent", result,
                "generatedAt", LocalDateTime.now().toString()
        ));
    }

    /**
     * Compute a weighted intelligence score from all sub-analyses.
     * CTA presence (30%) + category confidence (30%) + engagement (40%).
     */
    private double computeIntelligenceScore(Map<String, Object> cta, Map<String, Object> category, Map<String, Object> engagement) {
        double ctaScore = (Boolean) cta.getOrDefault("hasClearCTA", false) ? 85.0 : 35.0;
        double catConfidence = (double) category.getOrDefault("confidenceScore", 0.5);
        double catScore = catConfidence * 100.0;

        String openRateStr = (String) engagement.getOrDefault("predictedOpenRate", "0%");
        String respRateStr = (String) engagement.getOrDefault("predictedResponseRate", "0%");
        double openRate = parsePercent(openRateStr);
        double respRate = parsePercent(respRateStr);
        double engScore = (openRate * 0.5) + (respRate * 0.5);

        double composite = Math.round(((ctaScore * 0.30) + (catScore * 0.30) + (engScore * 0.40)) * 10.0) / 10.0;
        return Math.max(0, Math.min(100, composite));
    }

    private double parsePercent(String s) {
        if (s == null) return 0.0;
        return Double.parseDouble(s.replace("%", "").trim());
    }

    private String classifyGrade(double score) {
        if (score >= 80) return "Excellent";
        if (score >= 65) return "Good";
        if (score >= 50) return "Fair";
        if (score >= 35) return "Below Average";
        return "Poor";
    }
}
