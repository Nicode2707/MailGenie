package com.email.writer;


import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/email")
@AllArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class EmailGeneratorController {

    private final EmailGeneratorService emailGeneratorService;
    private final EmailHistoryService emailHistoryService;
    private final ApiRequestMetricService apiRequestMetricService;
    private final TelemetryService telemetryService;
    private final EmailCampaignRepository emailCampaignRepository;
    private final CampaignVariantRepository campaignVariantRepository;

    @PostMapping("/generate")
    public java.util.concurrent.CompletableFuture<ResponseEntity<String>> generateEmail(@RequestBody EmailRequest emailRequest) {
        long startTime = System.currentTimeMillis();
        String resolvedProvider = emailRequest.getProvider() != null ? emailRequest.getProvider() : "groq";
        String resolvedLanguage = emailRequest.getLanguage() != null ? emailRequest.getLanguage() : "English";
        String resolvedModel = emailRequest.getModel() != null && !emailRequest.getModel().trim().isEmpty() 
                ? emailRequest.getModel() : "default";

        return emailGeneratorService.generateEmailReplyAsync(emailRequest)
                .handle((response, throwable) -> {
                    long duration = System.currentTimeMillis() - startTime;
                    String status = (throwable == null) ? "SUCCESS" : "ERROR";
                    int charCount = (response != null) ? response.length() : 0;
                    String statusCode = null;

                    if (throwable != null) {
                        Throwable root = (throwable instanceof java.util.concurrent.CompletionException) ? throwable.getCause() : throwable;
                        if (root instanceof org.springframework.web.server.ResponseStatusException rse) {
                            statusCode = rse.getStatusCode().toString();
                        }
                    }

                    // Save metric record asynchronously
                    try {
                        ApiRequestMetric metric = ApiRequestMetric.builder()
                                .provider(resolvedProvider)
                                .model(resolvedModel)
                                .durationMs(duration)
                                .status(status)
                                .characterCount(charCount)
                                .build();
                        apiRequestMetricService.saveMetric(metric);

                        // Log to Enterprise Telemetry system as well
                        telemetryService.logTelemetry(resolvedProvider, "/email/generate", duration, status, 
                                statusCode, 
                                emailRequest.getEmailContent() != null ? emailRequest.getEmailContent().length() : 0);
                    } catch (Exception e) {
                        System.err.println("Failed to log API request metrics: " + e.getMessage());
                    }

                    if (throwable != null) {
                        Throwable root = (throwable instanceof java.util.concurrent.CompletionException) ? throwable.getCause() : throwable;
                        if (root instanceof org.springframework.web.server.ResponseStatusException rse) {
                            throw rse;
                        }
                        if (root instanceof RuntimeException re) {
                            throw re;
                        }
                        throw new RuntimeException(root);
                    }

                    // Auto-save generated email to history if successful
                    if (response != null) {
                        EmailHistory history = EmailHistory.builder()
                                .originalContent(emailRequest.getEmailContent())
                                .tone(emailRequest.getTone())
                                .generatedReply(response)
                                .provider(resolvedProvider)
                                .language(resolvedLanguage)
                                .build();
                        emailHistoryService.saveHistory(history);

                        // If A/B Testing Variants are requested
                        if (emailRequest.getVariantsCount() > 1) {
                            EmailCampaign campaign = EmailCampaign.builder()
                                    .name("A/B Test Campaign")
                                    .campaignType("AB_TEST")
                                    .status("RUNNING")
                                    .variantCount(emailRequest.getVariantsCount())
                                    .build();
                            emailCampaignRepository.save(campaign);

                            // Mock saving variants
                            for (int i = 0; i < emailRequest.getVariantsCount(); i++) {
                                CampaignVariant variant = CampaignVariant.builder()
                                        .campaignId(campaign.getId())
                                        .variantName("Variant " + (i + 1))
                                        .subjectLine(emailRequest.getSubject() != null ? emailRequest.getSubject() : "Test Subject")
                                        .content(response) // Simply reusing response for POC
                                        .sentCount(0)
                                        .openCount(0)
                                        .clickCount(0)
                                        .status("ACTIVE")
                                        .build();
                                campaignVariantRepository.save(variant);
                            }
                        }
                    }

                    return ResponseEntity.ok(response);
                });
    }

    @GetMapping("/config")
    public ResponseEntity<java.util.Map<String, Boolean>> getProviderConfig() {
        return ResponseEntity.ok(emailGeneratorService.getProviderConfigStatus());
    }
}