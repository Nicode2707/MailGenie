package com.email.writer;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Initializer component to populate default email templates on application startup if none exist.
 */
@Component
@RequiredArgsConstructor
public class TemplateInitializer implements CommandLineRunner {

    private final EmailTemplateRepository repository;

    @Override
    public void run(String... args) throws Exception {
        if (repository.count() == 0) {
            List<EmailTemplate> defaultTemplates = List.of(
                EmailTemplate.builder()
                    .title("💼 Professional Follow-up")
                    .body("Dear {{name}},\n\nI wanted to follow up on our discussion regarding {{topic}}. Please let me know if you have had a chance to review the details.\n\nBest regards,\n{{sender}}")
                    .build(),
                EmailTemplate.builder()
                    .title("📅 Schedule Meeting")
                    .body("Hi {{name}},\n\nI would love to schedule a quick 15-minute call to align on our next steps. Please let me know your availability this week.\n\nThanks,\n{{sender}}")
                    .build(),
                EmailTemplate.builder()
                    .title("☕ Casual Check-in")
                    .body("Hey {{name}},\n\nHope you are doing well! Just wanted to check in and see how things are going with {{project}}. Let me know when you are free to catch up.\n\nCheers,\n{{sender}}")
                    .build(),
                EmailTemplate.builder()
                    .title("🙏 Thank You & Feedback")
                    .body("Dear {{name}},\n\nThank you for taking the time to speak with me today. I truly appreciate your feedback and look forward to working together.\n\nBest regards,\n{{sender}}")
                    .build()
            );
            repository.saveAll(defaultTemplates);
            System.out.println("MailGenie: Populated default templates into the database.");
        }
    }
}
