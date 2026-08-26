package com.email.writer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for EmailContentIntelligenceController's underlying services:
 * EmailCallToActionExtractor, EmailCategoryClassifierEngine,
 * EmailEngagementPredictorService, EmailSignatureAutoGenerator,
 * and EmailPlaceholderAutoFiller.
 */
class EmailContentIntelligenceControllerTest {

    private EmailCallToActionExtractor ctaExtractor;
    private EmailCategoryClassifierEngine categoryClassifier;
    private EmailEngagementPredictorService engagementPredictor;
    private EmailSignatureAutoGenerator signatureGenerator;
    private EmailPlaceholderAutoFiller placeholderFiller;

    @BeforeEach
    void setUp() {
        ctaExtractor = new EmailCallToActionExtractor();
        categoryClassifier = new EmailCategoryClassifierEngine();
        engagementPredictor = new EmailEngagementPredictorService();
        signatureGenerator = new EmailSignatureAutoGenerator();
        placeholderFiller = new EmailPlaceholderAutoFiller();
    }

    // ── CTA Extraction Tests ────────────────────────────────

    @Test
    void testCTAWithActionVerbs() {
        var result = ctaExtractor.extractCTA("Please reply to confirm your availability for the meeting.");
        assertTrue((Boolean) result.get("hasClearCTA"));
        assertFalse(((java.util.List<?>) result.get("detectedActionVerbs")).isEmpty());
    }

    @Test
    void testCTANoActionVerbs() {
        var result = ctaExtractor.extractCTA("The weather is nice today.");
        assertFalse((Boolean) result.get("hasClearCTA"));
        assertTrue(((java.util.List<?>) result.get("detectedActionVerbs")).isEmpty());
    }

    @Test
    void testCTAEmptyContent() {
        var result = ctaExtractor.extractCTA("");
        assertFalse((Boolean) result.get("hasClearCTA"));
    }

    // ── Category Classification Tests ────────────────────────

    @Test
    void testCategorySalesOutreach() {
        var result = categoryClassifier.classifyCategory("We have a great demo and pricing discount for your team.");
        assertEquals("SALES_OUTREACH", result.get("detectedCategory"));
        assertTrue((double) result.get("confidenceScore") > 0.5);
    }

    @Test
    void testCategoryCustomerSupport() {
        var result = categoryClassifier.classifyCategory("I need help with a refund for a broken item. Please open a ticket.");
        assertEquals("CUSTOMER_SUPPORT", result.get("detectedCategory"));
    }

    @Test
    void testCategoryEmptyContent() {
        var result = categoryClassifier.classifyCategory("");
        assertEquals("GENERAL", result.get("detectedCategory"));
        assertEquals(0.50, result.get("confidenceScore"));
    }

    // ── Engagement Prediction Tests ──────────────────────────

    @Test
    void testEngagementBasic() {
        var result = engagementPredictor.predictEngagement("Quick question", "Hi there, wanted to follow up on our discussion.");
        assertNotNull(result.get("predictedOpenRate"));
        assertNotNull(result.get("predictedResponseRate"));
    }

    @Test
    void testEngagementWithQuestionInSubject() {
        var result = engagementPredictor.predictEngagement("Are you available?", "Let me know.");
        String openRate = (String) result.get("predictedOpenRate");
        assertTrue(Integer.parseInt(openRate.replace("%", "")) >= 50);
    }

    @Test
    void testEngagementEmpty() {
        var result = engagementPredictor.predictEngagement("", "");
        assertEquals("0%", result.get("predictedOpenRate"));
    }

    // ── Signature Generation Tests ───────────────────────────

    @Test
    void testSignatureFull() {
        String result = signatureGenerator.appendSignature("Thanks for reading.", "Jane Smith", "Engineer", "Acme Corp");
        assertTrue(result.contains("Jane Smith"));
        assertTrue(result.contains("Engineer"));
        assertTrue(result.contains("Acme Corp"));
        assertTrue(result.startsWith("Thanks for reading."));
    }

    @Test
    void testSignatureEmptyFields() {
        String result = signatureGenerator.appendSignature("Hello", "", "", "");
        assertEquals("Hello", result);
    }

    // ── Placeholder Auto-Fill Tests ──────────────────────────

    @Test
    void testPlaceholderFill() {
        var values = java.util.Map.of("Name", "John", "Id", "42");
        String result = placeholderFiller.populatePlaceholders("Hi {{Name}}, order {{Id}}.", values);
        assertEquals("Hi John, order 42.", result);
    }

    @Test
    void testPlaceholderFillEmpty() {
        String result = placeholderFiller.populatePlaceholders("", java.util.Map.of("a", "b"));
        assertEquals("", result);
    }

    @Test
    void testPlaceholderFillNoValues() {
        String result = placeholderFiller.populatePlaceholders("Hi {{Name}}", null);
        assertEquals("Hi {{Name}}", result);
    }
}
