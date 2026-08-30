package com.email.writer;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Service managing webhook configuration lifecycle and event dispatching.
 * When an email event occurs, this service resolves the matching webhooks
 * and delegates delivery to the WebhookDeliveryService.
 */
@Service
public class WebhookConfigService {

    private final WebhookConfigRepository configRepository;
    private final WebhookDeliveryLogRepository deliveryLogRepository;
    private final WebhookDeliveryService deliveryService;

    public WebhookConfigService(WebhookConfigRepository configRepository,
                                WebhookDeliveryLogRepository deliveryLogRepository,
                                WebhookDeliveryService deliveryService) {
        this.configRepository = configRepository;
        this.deliveryLogRepository = deliveryLogRepository;
        this.deliveryService = deliveryService;
    }

    // ---- CRUD Operations ----

    /**
     * Creates a new webhook configuration.
     */
    @Transactional
    public WebhookConfig createWebhook(WebhookConfig config) {
        if (config.getUrl() == null || config.getUrl().trim().isEmpty()) {
            throw new IllegalArgumentException("Webhook URL is required.");
        }
        if (config.getName() == null || config.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Webhook name is required.");
        }
        if (!config.getUrl().startsWith("http://") && !config.getUrl().startsWith("https://")) {
            throw new IllegalArgumentException("Webhook URL must start with http:// or https://");
        }
        return configRepository.save(config);
    }

    /**
     * Updates an existing webhook configuration.
     */
    @Transactional
    public WebhookConfig updateWebhook(Long id, WebhookConfig updates) {
        WebhookConfig existing = getById(id);
        if (updates.getName() != null) existing.setName(updates.getName());
        if (updates.getUrl() != null) existing.setUrl(updates.getUrl());
        if (updates.getSecret() != null) existing.setSecret(updates.getSecret());
        if (updates.getEventType() != null) existing.setEventType(updates.getEventType());
        existing.setActive(updates.isActive());
        existing.setMaxRetries(updates.getMaxRetries());
        existing.setTimeoutSeconds(updates.getTimeoutSeconds());
        return configRepository.save(existing);
    }

    /**
     * Retrieves a webhook config by ID.
     */
    public WebhookConfig getById(Long id) {
        return configRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(
                        "Webhook config not found with id: " + id));
    }

    /**
     * Returns all webhook configurations.
     */
    public List<WebhookConfig> getAllWebhooks() {
        return configRepository.findAll();
    }

    /**
     * Returns all active webhooks.
     */
    public List<WebhookConfig> getActiveWebhooks() {
        return configRepository.findByActiveTrue();
    }

    /**
     * Deletes a webhook configuration.
     */
    @Transactional
    public boolean deleteWebhook(Long id) {
        if (configRepository.existsById(id)) {
            configRepository.deleteById(id);
            return true;
        }
        return false;
    }

    /**
     * Toggles the active status of a webhook.
     */
    @Transactional
    public WebhookConfig toggleActive(Long id) {
        WebhookConfig config = getById(id);
        config.setActive(!config.isActive());
        return configRepository.save(config);
    }

    /**
     * Deactivates all webhooks.
     */
    @Transactional
    public int deactivateAll() {
        return configRepository.deactivateAll(LocalDateTime.now());
    }

    /**
     * Finds webhooks by name.
     */
    public List<WebhookConfig> searchByName(String name) {
        return configRepository.findByNameContainingIgnoreCase(name);
    }

    /**
     * Finds webhooks with high failure rates.
     */
    public List<WebhookConfig> getUnhealthyWebhooks(double failureRateThreshold) {
        return configRepository.findWithHighFailureRate(failureRateThreshold);
    }

    // ---- Event Dispatch ----

    /**
     * Dispatches an event to all matching active webhooks.
     * Each webhook is delivered independently; one failure does not block others.
     *
     * @param eventType The event type (e.g., EMAIL_GENERATED)
     * @param eventData The event payload data
     */
    public void dispatchEvent(String eventType, Map<String, Object> eventData) {
        List<WebhookConfig> matchingWebhooks = configRepository.findActiveByEventType(eventType);

        if (matchingWebhooks.isEmpty()) {
            return;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event", eventType);
        payload.put("timestamp", LocalDateTime.now().toString());
        payload.put("data", eventData);

        for (WebhookConfig webhook : matchingWebhooks) {
            try {
                deliveryService.deliver(webhook, eventType, payload);
            } catch (Exception e) {
                System.err.println("[Webhook] Delivery failed for webhook #" + webhook.getId()
                        + " (" + webhook.getName() + "): " + e.getMessage());
            }
        }
    }

    /**
     * Dispatches an EMAIL_GENERATED event.
     */
    public void dispatchEmailGenerated(String provider, String tone, int charCount) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("provider", provider);
        data.put("tone", tone);
        data.put("characterCount", charCount);
        dispatchEvent("EMAIL_GENERATED", data);
    }

    /**
     * Dispatches an EMAIL_SCHEDULED event.
     */
    public void dispatchEmailScheduled(Long scheduledEmailId, String recipient, String scheduledAt) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("scheduledEmailId", scheduledEmailId);
        data.put("recipient", recipient);
        data.put("scheduledAt", scheduledAt);
        dispatchEvent("EMAIL_SCHEDULED", data);
    }

    /**
     * Dispatches an EMAIL_FAILED event.
     */
    public void dispatchEmailFailed(String provider, String error) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("provider", provider);
        data.put("error", error);
        dispatchEvent("EMAIL_FAILED", data);
    }

    // ---- Delivery Logs ----

    /**
     * Returns delivery logs for a specific webhook.
     */
    public List<WebhookDeliveryLog> getDeliveryLogs(Long webhookId) {
        return deliveryLogRepository.findByWebhookConfigIdOrderByCreatedAtDesc(webhookId);
    }

    /**
     * Returns all delivery logs with a specific status.
     */
    public List<WebhookDeliveryLog> getDeliveryLogsByStatus(String status) {
        return deliveryLogRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    /**
     * Returns delivery logs within a date range.
     */
    public List<WebhookDeliveryLog> getDeliveryLogsByDateRange(LocalDateTime start, LocalDateTime end) {
        return deliveryLogRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(start, end);
    }

    /**
     * Returns the N most recent failed deliveries.
     */
    public List<WebhookDeliveryLog> getRecentFailures(int limit) {
        return deliveryLogRepository.findRecentFailures(limit);
    }

    /**
     * Returns delivery statistics for a specific webhook.
     */
    public Map<String, Long> getDeliveryStats(Long webhookId) {
        List<Object[]> stats = deliveryLogRepository.statsByWebhookId(webhookId);
        Map<String, Long> result = new LinkedHashMap<>();
        result.put("DELIVERED", 0L);
        result.put("FAILED", 0L);
        result.put("EXHAUSTED", 0L);
        result.put("RETRYING", 0L);
        for (Object[] row : stats) {
            result.put((String) row[0], (Long) row[1]);
        }
        return result;
    }

    /**
     * Returns a global summary of delivery status counts.
     */
    public Map<String, Long> getGlobalDeliverySummary() {
        List<Object[]> counts = deliveryLogRepository.countByStatus();
        Map<String, Long> summary = new LinkedHashMap<>();
        for (Object[] row : counts) {
            summary.put((String) row[0], (Long) row[1]);
        }
        return summary;
    }

    /**
     * Purges old delivery logs.
     */
    @Transactional
    public int purgeOldLogs(int olderThanDays) {
        LocalDateTime threshold = LocalDateTime.now().minusDays(olderThanDays);
        return deliveryLogRepository.purgeOlderThan(threshold);
    }
}
