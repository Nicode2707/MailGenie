package com.email.writer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * REST Controller providing a unified email quality analysis API.
 * Combines readability scoring, spam compliance checking, and subject line
 * optimization into a single endpoint for the React Quality Analyzer dashboard.
 */
@RestController
@RequestMapping("/api/email/quality")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class EmailQualityAnalyzerController {

    @Autowired
    private EmailReadabilityAnalyzer readabilityAnalyzer;

    @Autowired
    private EmailSpamComplianceChecker spamChecker;

    @Autowired
    private EmailSubjectLineOptimizer subjectOptimizer;

    /**
     * Full quality analysis: readability, spam score, and subject line optimization
     * in a single request for the dashboard overview.
     */
    @PostMapping("/analyze")
    public ResponseEntity<Map<String, Object>> fullQualityAnalysis(@RequestBody Map<String, String> payload) {
        String content = payload.getOrDefault("content", "");
        String subjectLine = payload.getOrDefault("subjectLine", "");

        Map<String, Object> readability = readabilityAnalyzer.analyzeReadability(content);
        Map<String, Object> spam = spamChecker.checkSpamScore(content);
        Map<String, Object> subject = subjectOptimizer.optimizeSubjectLine(subjectLine);

        double readabilityScore = (Double) readability.getOrDefault("fleschKincaidScore", 0.0);
        double spamScore = (Double) spam.getOrDefault("spamScore", 0.0);
        double subjectScore = ((Number) subject.getOrDefault("subjectScore", 0)).doubleValue();

        double overallScore = Math.round(((readabilityScore * 0.4) + ((100.0 - spamScore) * 0.35) + (subjectScore * 0.25)) * 10.0) / 10.0;
        String grade = classifyOverallGrade(overallScore);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("readability", readability);
        result.put("spamCompliance", spam);
        result.put("subjectLine", subject);
        result.put("overallScore", overallScore);
        result.put("overallGrade", grade);
        result.put("analyzedAt", java.time.LocalDateTime.now().toString());

        return ResponseEntity.ok(result);
    }

    /**
     * Readability-only analysis endpoint.
     */
    @PostMapping("/readability")
    public ResponseEntity<Map<String, Object>> analyzeReadability(@RequestBody Map<String, String> payload) {
        String content = payload.getOrDefault("content", "");
        return ResponseEntity.ok(readabilityAnalyzer.analyzeReadability(content));
    }

    /**
     * Spam compliance check only.
     */
    @PostMapping("/spam")
    public ResponseEntity<Map<String, Object>> checkSpamCompliance(@RequestBody Map<String, String> payload) {
        String content = payload.getOrDefault("content", "");
        return ResponseEntity.ok(spamChecker.checkSpamScore(content));
    }

    /**
     * Subject line optimization only.
     */
    @PostMapping("/subject")
    public ResponseEntity<Map<String, Object>> optimizeSubjectLine(@RequestBody Map<String, String> payload) {
        String subjectLine = payload.getOrDefault("subjectLine", "");
        return ResponseEntity.ok(subjectOptimizer.optimizeSubjectLine(subjectLine));
    }

    /**
     * Classification of the combined overall quality score.
     */
    private String classifyOverallGrade(double score) {
        if (score >= 85) return "Excellent";
        if (score >= 70) return "Good";
        if (score >= 55) return "Fair";
        if (score >= 40) return "Needs Improvement";
        return "Poor";
    }
}
