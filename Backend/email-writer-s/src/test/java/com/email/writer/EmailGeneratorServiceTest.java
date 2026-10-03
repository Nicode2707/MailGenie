package com.email.writer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class EmailGeneratorServiceTest {

    private EmailGeneratorService emailGeneratorService;

    @BeforeEach
    void setUp() {
        emailGeneratorService = new EmailGeneratorService(WebClient.builder(), null, null);
    }

    @Test
    @DisplayName("Should build reply prompt with tone, language, and content")
    void testBuildPrompt_ReplyMode() {
        EmailRequest request = new EmailRequest();
        request.setComposeMode(false);
        request.setTone("professional");
        request.setLanguage("French");
        request.setEmailContent("Pouvez-vous m'envoyer le rapport financier?");

        String prompt = emailGeneratorService.buildPrompt(request);

        assertNotNull(prompt);
        assertTrue(prompt.contains("Generate an appropriate email reply"));
        assertTrue(prompt.contains("Use a professional tone."));
        assertTrue(prompt.contains("Write the response strictly in French."));
        assertTrue(prompt.contains("Original email:\nPouvez-vous m'envoyer le rapport financier?"));
    }

    @Test
    @DisplayName("Should build compose prompt with custom instructions")
    void testBuildPrompt_ComposeModeWithCustomInstructions() {
        EmailRequest request = new EmailRequest();
        request.setComposeMode(true);
        request.setTone("urgent");
        request.setLanguage("English");
        request.setCustomInstructions("Please include a deadline of Friday 5 PM.");
        request.setEmailContent("Draft an email announcing the quarterly roadmap.");

        String prompt = emailGeneratorService.buildPrompt(request);

        assertNotNull(prompt);
        assertTrue(prompt.contains("Write a complete email based on the following instructions."));
        assertTrue(prompt.contains("Use a urgent tone."));
        assertTrue(prompt.contains("Write the response strictly in English."));
        assertTrue(prompt.contains("Specific User Instructions / Context: Please include a deadline of Friday 5 PM."));
        assertTrue(prompt.contains("Instructions:\nDraft an email announcing the quarterly roadmap."));
    }

    @Test
    @DisplayName("Should correctly extract response content from OpenAI / Groq / Gemini JSON format")
    void testExtractResponseContent_OpenAiFormat() {
        String json = """
        {
          "id": "chatcmpl-123",
          "object": "chat.completion",
          "choices": [
            {
              "index": 0,
              "message": {
                "role": "assistant",
                "content": "Thank you for reaching out. I have attached the requested files."
              },
              "finish_reason": "stop"
            }
          ]
        }
        """;

        String extracted = emailGeneratorService.extractResponseContent(json, "groq");
        assertEquals("Thank you for reaching out. I have attached the requested files.", extracted);
    }

    @Test
    @DisplayName("Should correctly extract response content from Anthropic Claude JSON format")
    void testExtractResponseContent_ClaudeFormat() {
        String json = """
        {
          "id": "msg_01XyZ",
          "type": "message",
          "role": "assistant",
          "content": [
            {
              "type": "text",
              "text": "Hello, here is the drafted project scope for your review."
            }
          ]
        }
        """;

        String extracted = emailGeneratorService.extractResponseContent(json, "claude");
        assertEquals("Hello, here is the drafted project scope for your review.", extracted);
    }

    @Test
    @DisplayName("Should handle malformed response JSON gracefully")
    void testExtractResponseContent_MalformedJson() {
        String malformedJson = "Invalid JSON response from upstream gateway";
        String extracted = emailGeneratorService.extractResponseContent(malformedJson, "openai");

        assertNotNull(extracted);
        assertTrue(extracted.startsWith("Error processing API response"));
    }

    @Test
    @DisplayName("Should fallback to local reply generator when no API keys are configured")
    void testGenerateEmailReply_LocalFallback() {
        EmailRequest request = new EmailRequest();
        request.setTone("casual");
        request.setEmailContent("Hey, are you free for lunch?");

        String reply = emailGeneratorService.generateEmailReply(request);

        assertNotNull(reply);
        assertTrue(reply.contains("Hi there,"));
        assertTrue(reply.contains("Cheers,\n[Your Name]"));
    }

    @Test
    @DisplayName("Should fallback with custom instructions when no API keys are configured")
    void testGenerateEmailReply_LocalFallbackWithCustomInstructions() {
        EmailRequest request = new EmailRequest();
        request.setCustomInstructions("Confirm attendance at the board meeting.");
        request.setEmailContent("Please RSVP.");

        String reply = emailGeneratorService.generateEmailReply(request);

        assertNotNull(reply);
        assertTrue(reply.contains("Regarding your request: Confirm attendance at the board meeting."));
        assertTrue(reply.contains("Best regards,\n[Your Name]"));
    }

    @Test
    @DisplayName("Should return accurate provider config map")
    void testGetProviderConfigStatus() {
        ReflectionTestUtils.setField(emailGeneratorService, "groqApiKey", "gsk_valid_key_123");
        ReflectionTestUtils.setField(emailGeneratorService, "openaiApiKey", "sk_valid_openai_key");
        ReflectionTestUtils.setField(emailGeneratorService, "geminiApiKey", "");
        ReflectionTestUtils.setField(emailGeneratorService, "anthropicApiKey", null);

        Map<String, Boolean> status = emailGeneratorService.getProviderConfigStatus();

        assertNotNull(status);
        assertTrue(status.get("groq"));
        assertTrue(status.get("openai"));
        assertFalse(status.get("gemini"));
        assertFalse(status.get("claude"));
    }

    @Test
    @DisplayName("Should recognize default placeholder groq key as unconfigured")
    void testGetProviderConfigStatus_DefaultPlaceholderKey() {
        ReflectionTestUtils.setField(emailGeneratorService, "groqApiKey", "your_groq_api_key_here");
        ReflectionTestUtils.setField(emailGeneratorService, "openaiApiKey", "");
        ReflectionTestUtils.setField(emailGeneratorService, "geminiApiKey", "");
        ReflectionTestUtils.setField(emailGeneratorService, "anthropicApiKey", "");

        Map<String, Boolean> status = emailGeneratorService.getProviderConfigStatus();

        assertNotNull(status);
        assertFalse(status.get("groq"));
        assertFalse(status.get("openai"));
        assertFalse(status.get("gemini"));
        assertFalse(status.get("claude"));
    }

    @Test
    void testGenerateEmailReplyAsync_FallbackExecution() throws Exception {
        EmailRequest request = new EmailRequest();
        request.setEmailContent("Hello, can we review the plan tomorrow?");
        request.setTone("professional");
        request.setProvider("groq");
        request.setComposeMode(false);

        CompletableFuture<String> future = emailGeneratorService.generateEmailReplyAsync(request);
        assertNotNull(future);

        String result = future.get();
        assertNotNull(result);
        assertTrue(result.contains("Dear Recipient"));
        assertTrue(result.contains("I have reviewed your email"));
    }

    @Test
    void testGenerateEmailReplyAsync_CustomInstructionsFallback() throws Exception {
        EmailRequest request = new EmailRequest();
        request.setEmailContent("Meeting request");
        request.setCustomInstructions("Confirm availability for 3 PM");
        request.setProvider("openai");

        CompletableFuture<String> future = emailGeneratorService.generateEmailReplyAsync(request);
        assertNotNull(future);

        String result = future.get();
        assertNotNull(result);
        assertTrue(result.contains("Confirm availability for 3 PM"));
    }

    @Test
    void testBuildPrompt_IncludesSubjectContextWhenProvided() {
        EmailRequest request = new EmailRequest();
        request.setEmailContent("Are you available for a quick sync tomorrow?");
        request.setSubject("Q3 Architecture Review");
        request.setTone("professional");
        request.setLanguage("English");
        request.setComposeMode(false);

        String prompt = emailGeneratorService.buildPrompt(request);

        assertNotNull(prompt);
        assertTrue(prompt.contains("Email Subject / Topic: Q3 Architecture Review"));
        assertTrue(prompt.contains("Are you available for a quick sync tomorrow?"));
        assertTrue(prompt.contains("Use a professional tone."));
        assertTrue(prompt.contains("Write the response strictly in English."));
        assertTrue(prompt.contains("Generate an appropriate email reply for the following email content."));
    }

    @Test
    void testBuildPrompt_OmitsSubjectWhenNullOrEmpty() {
        EmailRequest request = new EmailRequest();
        request.setEmailContent("Please review the document.");
        request.setTone("casual");
        request.setLanguage("English");
        request.setComposeMode(false);

        String prompt = emailGeneratorService.buildPrompt(request);

        assertNotNull(prompt);
        assertFalse(prompt.contains("Email Subject / Topic:"));
        assertTrue(prompt.contains("Please review the document."));
    }

    @Test
    void testBuildPrompt_ComposeModeWithSubjectAndCustomInstructions() {
        EmailRequest request = new EmailRequest();
        request.setEmailContent("Introduce our new enterprise email writing platform to client.");
        request.setSubject("Product Launch: MailGenie Enterprise");
        request.setCustomInstructions("Mention 30-day free trial");
        request.setTone("enthusiastic");
        request.setLanguage("English");
        request.setComposeMode(true);

        String prompt = emailGeneratorService.buildPrompt(request);

        assertNotNull(prompt);
        assertTrue(prompt.contains("Write a complete email based on the following instructions."));
        assertTrue(prompt.contains("Email Subject / Topic: Product Launch: MailGenie Enterprise"));
        assertTrue(prompt.contains("Specific User Instructions / Context: Mention 30-day free trial"));
        assertTrue(prompt.contains("Use a enthusiastic tone."));
    }
}
