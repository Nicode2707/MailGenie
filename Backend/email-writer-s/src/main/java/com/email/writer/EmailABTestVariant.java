package com.email.writer;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Model representing a single A/B test variant containing
 * subject line and body content with pre-computed quality metrics.
 */
public class EmailABTestVariant {

    private String variantId;
    private String label; // "A", "B", "C", etc.
    private String subjectLine;
    private String bodyContent;
    private Map<String, Object> readabilityMetrics;
    private Map<String, Object> spamMetrics;
    private Map<String, Object> subjectMetrics;
    private Map<String, Object> toneMetrics;
    private double compositeScore;
    private String verdict; // "WINNER", "RUNNER_UP", "UNDERPERFORMER"
    private LocalDateTime analyzedAt;

    public EmailABTestVariant() {}

    public EmailABTestVariant(String variantId, String label, String subjectLine, String bodyContent) {
        this.variantId = variantId;
        this.label = label;
        this.subjectLine = subjectLine;
        this.bodyContent = bodyContent;
        this.analyzedAt = LocalDateTime.now();
    }

    // ── Getters & Setters ───────────────────────────────────

    public String getVariantId() { return variantId; }
    public void setVariantId(String variantId) { this.variantId = variantId; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getSubjectLine() { return subjectLine; }
    public void setSubjectLine(String subjectLine) { this.subjectLine = subjectLine; }

    public String getBodyContent() { return bodyContent; }
    public void setBodyContent(String bodyContent) { this.bodyContent = bodyContent; }

    public Map<String, Object> getReadabilityMetrics() { return readabilityMetrics; }
    public void setReadabilityMetrics(Map<String, Object> readabilityMetrics) { this.readabilityMetrics = readabilityMetrics; }

    public Map<String, Object> getSpamMetrics() { return spamMetrics; }
    public void setSpamMetrics(Map<String, Object> spamMetrics) { this.spamMetrics = spamMetrics; }

    public Map<String, Object> getSubjectMetrics() { return subjectMetrics; }
    public void setSubjectMetrics(Map<String, Object> subjectMetrics) { this.subjectMetrics = subjectMetrics; }

    public Map<String, Object> getToneMetrics() { return toneMetrics; }
    public void setToneMetrics(Map<String, Object> toneMetrics) { this.toneMetrics = toneMetrics; }

    public double getCompositeScore() { return compositeScore; }
    public void setCompositeScore(double compositeScore) { this.compositeScore = compositeScore; }

    public String getVerdict() { return verdict; }
    public void setVerdict(String verdict) { this.verdict = verdict; }

    public LocalDateTime getAnalyzedAt() { return analyzedAt; }
    public void setAnalyzedAt(LocalDateTime analyzedAt) { this.analyzedAt = analyzedAt; }
}
