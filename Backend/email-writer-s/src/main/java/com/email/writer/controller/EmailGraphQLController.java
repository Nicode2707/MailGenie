package com.email.writer.controller;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import lombok.RequiredArgsConstructor;
import com.email.writer.EmailGeneratorService;
import com.email.writer.EmailRequest;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * GraphQL Controller resolving queries and mutations for MailGenie.
 */
@Controller
@RequiredArgsConstructor
public class EmailGraphQLController {

    private final EmailGeneratorService emailGeneratorService;

    @QueryMapping
    public List<Map<String, Object>> getTemplates(@Argument String category) {
        return List.of(
            Map.of(
                "id", UUID.randomUUID().toString(),
                "title", "Professional Meeting Follow-up",
                "content", "Thank you for taking the time to meet today...",
                "tone", "Professional",
                "category", "Business",
                "isDefault", true,
                "createdAt", Instant.now().toString()
            ),
            Map.of(
                "id", UUID.randomUUID().toString(),
                "title", "Casual Catch-up",
                "content", "Hey! Hope you are doing well...",
                "tone", "Casual",
                "category", "Personal",
                "isDefault", true,
                "createdAt", Instant.now().toString()
            )
        );
    }

    @QueryMapping
    public List<Map<String, Object>> searchTemplates(@Argument String query) {
        return getTemplates(null).stream()
                .filter(t -> ((String) t.get("title")).toLowerCase().contains(query.toLowerCase()))
                .toList();
    }

    @MutationMapping
    public Map<String, Object> generateEmail(@Argument Map<String, Object> input) {
        long startTime = System.currentTimeMillis();
        String emailContent = (String) input.get("emailContent");
        String tone = (String) input.get("tone");
        String provider = (String) input.getOrDefault("provider", "GROQ");

        EmailRequest request = new EmailRequest();
        request.setEmailContent(emailContent);
        request.setTone(tone);
        request.setProvider(provider);

        String generatedReply = emailGeneratorService.generateEmailReply(request);

        return Map.of(
            "reply", generatedReply,
            "provider", provider,
            "tokensUsed", generatedReply.length() / 4,
            "executionTimeMs", System.currentTimeMillis() - startTime
        );
    }
}
