package com.email.writer;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Service for managing subscriptions and simulating webhook HTTP dispatches
 * with HMAC-SHA256 signature generation and latency auditing.
 */
@Service
@RequiredArgsConstructor
public class WebhookService {

    private final WebhookRepository webhookRepository;
    private final WebhookDeliveryLogRepository logRepository;

    public List<WebhookSubscription> getAllSubscriptions() {
        return webhookRepository.findAll();
    }

    @Transactional
    public WebhookSubscription registerSubscription(WebhookSubscription subscription) {
        if (subscription.getSecretKey() == null || subscription.getSecretKey().isEmpty()) {
            subscription.setSecretKey(UUID.randomUUID().toString().replace("-", ""));
        }
        return webhookRepository.save(subscription);
    }

    @Transactional
    public void deleteSubscription(Long id) {
        webhookRepository.deleteById(id);
    }

    public List<WebhookDeliveryLog> getDeliveryLogs() {
        return logRepository.findAllByOrderByCreatedAtDesc();
    }

    public Map<String, Object> getWebhookStats() {
        List<WebhookSubscription> subs = webhookRepository.findAll();
        List<WebhookDeliveryLog> logs = logRepository.findAll();

        long activeSubs = subs.stream().filter(WebhookSubscription::getIsActive).count();
        long totalDispatched = logs.size();
        long failedDeliveries = logs.stream().filter(l -> "FAILED".equals(l.getStatus())).count();

        double avgLatency = logs.stream().mapToLong(l -> l.getDurationMs() != null ? l.getDurationMs() : 0L).average().orElse(0.0);

        Map<String, Object> stats = new HashMap<>();
        stats.put("activeSubscriptions", activeSubs);
        stats.put("totalEndpoints", subs.size());
        stats.put("dispatchCount", totalDispatched);
        stats.put("failureRate", totalDispatched == 0 ? 0 : Math.round((failedDeliveries * 100.0) / totalDispatched));
        stats.put("averageLatencyMs", Math.round(avgLatency));

        return stats;
    }

    /**
     * Executes the Webhook simulation, generating cryptographic signatures matching
     * standard Stripe/GitHub formats
     * and injecting realistic network topologies/failures.
     */
    @Transactional
    public Map<String, Object> dispatchEvent(String eventType, String payload) {
        List<WebhookSubscription> targets = webhookRepository.findByEventTypeAndIsActiveTrue(eventType);
        Map<String, Object> dispatchResults = new HashMap<>();

        int failures = 0;
        int successes = 0;

        for (WebhookSubscription target : targets) {
            long startTime = System.currentTimeMillis();

            try {
                // Generate HMAC Signature
                Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
                SecretKeySpec secret_key = new SecretKeySpec(target.getSecretKey().getBytes(StandardCharsets.UTF_8),
                        "HmacSHA256");
                sha256_HMAC.init(secret_key);
                String signature = Base64.getEncoder()
                        .encodeToString(sha256_HMAC.doFinal(payload.getBytes(StandardCharsets.UTF_8)));

                // Simulate HTTP Latency (100ms - 900ms)
                long latency = (long) (Math.random() * 800) + 100;
                Thread.sleep(latency);

                // Simulate 15% Network Failure Rate on remote endpoints for realistic telemetry
                boolean isFailed = Math.random() < 0.15;

                long actualDuration = System.currentTimeMillis() - startTime;

                WebhookDeliveryLog log = WebhookDeliveryLog.builder()
                        .webhookConfigId(target.getId())
                        .eventType(eventType)
                        .targetUrl(target.getEndpointUrl())
                        .payload(payload.length() > 500 ? payload.substring(0, 500) + "..." : payload)
                        .status(isFailed ? "FAILED" : "DELIVERED")
                        .httpStatusCode(isFailed ? 503 : 200)
                        .responseBody(isFailed ? "Connection Refused / Gateway Timeout"
                                : "OK - Accepted Signature: " + signature.substring(0, 10) + "...")
                        .durationMs(actualDuration)
                        .build();

                logRepository.save(log);

                if (isFailed)
                    failures++;
                else
                    successes++;

            } catch (Exception e) {
                // Cryptography or internal failure
                long actualDuration = System.currentTimeMillis() - startTime;
                WebhookDeliveryLog log = WebhookDeliveryLog.builder()
                        .webhookConfigId(target.getId())
                        .eventType(eventType)
                        .targetUrl(target.getEndpointUrl())
                        .payload("ERROR GENERATING PAYLOAD")
                        .status("FAILED")
                        .httpStatusCode(500)
                        .responseBody(e.getMessage())
                        .durationMs(actualDuration)
                        .errorMessage(e.getMessage())
                        .build();
                logRepository.save(log);
                failures++;
            }
        }

        dispatchResults.put("eventType", eventType);
        dispatchResults.put("targetsCount", targets.size());
        dispatchResults.put("successfulDispatches", successes);
        dispatchResults.put("failedDispatches", failures);

        return dispatchResults;
    }
}
