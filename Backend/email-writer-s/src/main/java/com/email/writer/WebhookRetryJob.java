package com.email.writer;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class WebhookRetryJob {

    private final WebhookEventRepository webhookEventRepository;
    private final EmailGeneratorService emailGeneratorService;
    private final ObjectMapper objectMapper;

    // Run every minute
    @Scheduled(fixedDelay = 60000)
    public void processPendingWebhooks() {
        List<WebhookEvent> events = webhookEventRepository.findEventsToProcess(LocalDateTime.now());
        
        for (WebhookEvent event : events) {
            try {
                event.setStatus("PROCESSING");
                webhookEventRepository.save(event);

                // Parse the JSON payload
                WebhookPayload payload = objectMapper.readValue(event.getPayloadJson(), WebhookPayload.class);
                
                // Map the variables to an EmailRequest
                EmailRequest emailRequest = new EmailRequest();
                emailRequest.setComposeMode(true);
                
                // Example payload mapping: extracting data and injecting into the prompt
                StringBuilder injectedContext = new StringBuilder();
                if (payload.getData() != null) {
                    payload.getData().forEach((key, value) -> 
                        injectedContext.append(key).append(": ").append(value).append("\n")
                    );
                }
                
                String customInstructions = "Context from " + event.getTriggerSource() + ":\n" + injectedContext.toString() + "\nPlease generate a personalized email for " + payload.getRecipientEmail() + ".";
                emailRequest.setCustomInstructions(customInstructions);
                emailRequest.setEmailContent("");

                // Dispatch to generator service
                emailGeneratorService.generateEmailReply(emailRequest);
                
                // If successful
                event.setStatus("COMPLETED");
                webhookEventRepository.save(event);
                
            } catch (Exception e) {
                event.setRetryCount(event.getRetryCount() + 1);
                event.setLastError(e.getMessage());
                if (event.getRetryCount() >= 5) {
                    event.setStatus("FAILED");
                } else {
                    event.setStatus("PENDING");
                    // Exponential backoff for next retry (e.g., 5 mins, 25 mins, etc.)
                    event.setNextRetryAt(LocalDateTime.now().plusMinutes(5L * event.getRetryCount()));
                }
                webhookEventRepository.save(event);
            }
        }
    }
}
