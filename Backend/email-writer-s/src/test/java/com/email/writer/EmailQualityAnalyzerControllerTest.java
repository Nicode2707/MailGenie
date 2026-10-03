package com.email.writer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for EmailQualityAnalyzerController's underlying services:
 * EmailReadabilityAnalyzer, EmailSpamComplianceChecker, and EmailSubjectLineOptimizer.
 */
class EmailQualityAnalyzerControllerTest {

    private EmailReadabilityAnalyzer readabilityAnalyzer;
    private EmailSpamComplianceChecker spamChecker;
    private EmailSubjectLineOptimizer subjectOptimizer;

    @BeforeEach
    void setUp() {
        readabilityAnalyzer = new EmailReadabilityAnalyzer();
        spamChecker = new EmailSpamComplianceChecker();
        subjectOptimizer = new EmailSubjectLineOptimizer();
    }

    // ── Readability Tests ────────────────────────────────────

    @Test
    void testReadabilityEmptyContent() {
        var result = readabilityAnalyzer.analyzeReadability("");
        assertEquals(100.0, result.get("fleschKincaidScore"));
        assertEquals(0, result.get("sentenceCount"));
    }

    @Test
    void testReadabilitySimpleContent() {
        var result = readabilityAnalyzer.analyzeReadability("The cat sat on the mat. The dog ran in the park.");
        assertNotNull(result.get("fleschKincaidScore"));
        assertNotNull(result.get("readingGradeLevel"));
        assertTrue((Double) result.get("fleschKincaidScore") > 50.0);
    }

    @Test
    void testReadabilityComplexContent() {
        var result = readabilityAnalyzer.analyzeReadability(
            "The implementation of sophisticated architectural paradigms necessitates " +
            "comprehensive understanding of multifaceted engineering principles and " +
            "methodologies that transcend conventional computational frameworks."
        );
        assertNotNull(result.get("fleschKincaidScore"));
        assertNotNull(result.get("averageSentenceLength"));
    }

    @Test
    void testReadabilityNullContent() {
        var result = readabilityAnalyzer.analyzeReadability(null);
        assertEquals(100.0, result.get("fleschKincaidScore"));
        assertEquals(0, result.get("sentenceCount"));
    }

    // ── Spam Compliance Tests ────────────────────────────────

    @Test
    void testSpamCleanEmail() {
        var result = spamChecker.checkSpamScore("Hi John, wanted to follow up on our meeting.");
        assertEquals(0.0, result.get("spamScore"));
        assertFalse((Boolean) result.get("isHighRiskSpam"));
        assertTrue(((java.util.List<?>) result.get("flaggedTriggerWords")).isEmpty());
    }

    @Test
    void testSpamTriggerWordsDetected() {
        var result = spamChecker.checkSpamScore(
            "Buy now and get 100% free access! Click here for an exclusive deal!"
        );
        assertTrue((Double) result.get("spamScore") > 0.0);
        assertFalse(((java.util.List<?>) result.get("flaggedTriggerWords")).isEmpty());
    }

    @Test
    void testSpamHighRisk() {
        var result = spamChecker.checkSpamScore(
            "Act now! Buy now! Click here! 100% free! Cash bonus! Guaranteed winner!"
        );
        assertTrue((Boolean) result.get("isHighRiskSpam"));
        assertTrue((Double) result.get("spamScore") >= 40.0);
    }

    @Test
    void testSpamEmptyContent() {
        var result = spamChecker.checkSpamScore("");
        assertEquals(0.0, result.get("spamScore"));
        assertFalse((Boolean) result.get("isHighRiskSpam"));
    }

    // ── Subject Line Tests ───────────────────────────────────

    @Test
    void testSubjectLineOptimal() {
        var result = subjectOptimizer.optimizeSubjectLine("Quick question about tomorrow's meeting");
        int score = (int) result.get("subjectScore");
        assertTrue(score >= 70);
        assertTrue((int) result.get("characterCount") > 20);
    }

    @Test
    void testSubjectLineTooShort() {
        var result = subjectOptimizer.optimizeSubjectLine("Hi");
        int score = (int) result.get("subjectScore");
        assertTrue(score < 70);
        assertFalse(((java.util.List<?>) result.get("suggestions")).isEmpty());
    }

    @Test
    void testSubjectLineTooLong() {
        var result = subjectOptimizer.optimizeSubjectLine(
            "This is a very long subject line that will definitely be truncated on mobile devices and email clients"
        );
        assertTrue((int) result.get("characterCount") > 60);
        assertFalse(((java.util.List<?>) result.get("suggestions")).isEmpty());
    }

    @Test
    void testSubjectLineAllCaps() {
        var result = subjectOptimizer.optimizeSubjectLine("URGENT ACTION REQUIRED NOW");
        int score = (int) result.get("subjectScore");
        assertTrue(score < 60);
    }

    @Test
    void testSubjectLineWithQuestion() {
        var result = subjectOptimizer.optimizeSubjectLine("Are you available for a call?");
        int score = (int) result.get("subjectScore");
        assertTrue(score >= 70);
    }

    @Test
    void testSubjectLineEmpty() {
        var result = subjectOptimizer.optimizeSubjectLine("");
        assertEquals(0, result.get("subjectScore"));
        assertEquals(0, result.get("characterCount"));
    }
}
