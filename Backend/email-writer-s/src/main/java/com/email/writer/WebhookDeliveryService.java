package com.email.writer;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;

/**
 * Handles the actual HTTP delivery of webhook payloads to registered endpoints.
 * Includes HMAC-SHA256 payload signing, exponential backoff retry, and delivery logging.
 */
@Service
public class WebhookDeliveryService {

    private final WebhookDeliveryLogRepository deliveryLogRepository;
    private final WebClient webClient;
    private static final String SIGNATURE_HEADER = "X-Webhook-Signature";
    private static final String ALGORITHM = "HmacSHA256";

    public WebhookDeliveryService(WebhookDeliveryLogRepository deliveryLogRepository,
                                  WebClient.Builder webClientBuilder) {
        this.deliveryLogRepository = deliveryLogRepository;
        this.webClient = webClientBuilder.build();
    }

    /**
     * Delivers a payload to a webhook endpoint with retry and logging.
     * This method is transactional and synchronous — use dispatchAsync for background delivery.
     */
    @Transactional
    public WebhookDeliveryLog deliver(WebhookConfig webhook, String eventType, Map<String, Object> payload) {
        String payloadJson = toJson(payload);
        String signature = generateSignature(webhook.getSecret(), payloadJson);

        WebhookDeliveryLog log = WebhookDeliveryLog.builder()
                .webhookConfigId(webhook.getId())
                .eventType(eventType)
                .targetUrl(webhook.getUrl())
                .payload(payloadJson)
                .signature(signature)
                .status("PENDING")
                .attemptNumber(1)
                .build();
        log = deliveryLogRepository.save(log);

        attemptDelivery(webhook, log, payloadJson, signature);
        return log;
    }

    /**
     * Core delivery logic with retry loop.
     */
    private void attemptDelivery(WebhookConfig webhook, WebhookDeliveryLog log,
                                 String payloadJson, String signature) {
        int maxAttempts = Math.max(1, webhook.getMaxRetries());
        int timeoutSeconds = Math.max(1, Math.min(webhook.getTimeoutSeconds(), 30));

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            if (attempt > 1) {
                log.markRetrying(attempt);
                deliveryLogRepository.save(log);
                long delay = calculateBackoff(attempt);
                sleep(delay);
            }

            long startTime = System.currentTimeMillis();
            try {
                Integer httpStatus = webClient.post()
                        .uri(webhook.getUrl())
                        .header("Content-Type", "application/json")
                        .header("User-Agent", "MailGenie-Webhook/1.0")
                        .header(SIGNATURE_HEADER, signature)
                        .header("X-Event-Type", eventType)
                        .header("X-Delivery-Id", String.valueOf(log.getId()))
                        .bodyValue(payloadJson)
                        .retrieve()
                        .toBodilessEntity()
                        .block(java.time.Duration.ofSeconds(timeoutSeconds));

                long duration = System.currentTimeMillis() - startTime;
                int status = httpStatus != null ? httpStatus.getStatusCode().value() : 0;

                if (status >= 200 && status < 300) {
                    log.markDelivered(status, "OK", duration);
                    deliveryLogRepository.save(log);
                    webhook.recordSuccess();
                    return; // Success
                } else {
                    log.markFailed("HTTP " + status, duration);
                }

            } catch (WebClientResponseException e) {
                long duration = System.currentTimeMillis() - startTime;
                log.markFailed("HTTP " + e.getStatusCode() + ": " + e.getResponseBodyAsString(), duration);

            } catch (Exception e) {
                long duration = System.currentTimeMillis() - startTime;
                log.markFailed(e.getClass().getSimpleName() + ": " + e.getMessage(), duration);
            }

            deliveryLogRepository.save(log);
        }

        // All retries exhausted
        log.markExhausted(log.getErrorMessage());
        deliveryLogRepository.save(log);
        webhook.recordFailure();
    }

    /**
     * Calculates exponential backoff delay: 1s, 2s, 4s, 8s, ...
     */
    private long calculateBackoff(int attempt) {
        return (long) Math.pow(2, attempt - 1) * 1000;
    }

    /**
     * Sleeps for the given milliseconds, ignoring interruptions.
     */
    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Generates an HMAC-SHA256 signature for the given payload using the webhook secret.
     */
    public static String generateSignature(String secret, String payload) {
        if (secret == null || secret.trim().isEmpty()) {
            return "";
        }
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8), ALGORITHM);
            mac.init(keySpec);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Verifies that a received signature matches the expected HMAC-SHA256 signature.
     */
    public static boolean verifySignature(String secret, String payload, String receivedSignature) {
        if (secret == null || payload == null || receivedSignature == null) {
            return false;
        }
        String expected = generateSignature(secret, payload);
        return expected.equals(receivedSignature);
    }

    /**
     * Converts a payload map to a JSON string.
     * Simple hand-rolled serializer to avoid extra dependencies.
     */
    private static String toJson(Map<String, Object> payload) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : payload.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(escapeJson(entry.getKey())).append("\":");
            appendJsonValue(sb, entry.getValue());
        }
        sb.append("}");
        return sb.toString();
    }

    private static void appendJsonValue(StringBuilder sb, Object value) {
        if (value == null) {
            sb.append("null");
        } else if (value instanceof String) {
            sb.append("\"").append(escapeJson((String) value)).append("\"");
        } else if (value instanceof Number || value instanceof Boolean) {
            sb.append(value.toString());
        } else {
            sb.append("\"").append(escapeJson(String.valueOf(value))).append("\"");
        }
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
