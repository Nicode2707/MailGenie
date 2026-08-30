package com.email.writer;

import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * REST controller exposing the full Webhook Management API.
 * Provides endpoints for webhook CRUD, event dispatch, delivery logs, and stats.
 */
@RestController
@RequestMapping("/api/webhooks")
@AllArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class WebhookController {

    private final WebhookConfigService webhookConfigService;

    // ---- Webhook CRUD ----

    /**
     * POST /api/webhooks — Create a new webhook.
     */
    @PostMapping
    public ResponseEntity<WebhookConfig> createWebhook(@RequestBody WebhookConfig config) {
        WebhookConfig created = webhookConfigService.createWebhook(config);
        return ResponseEntity.ok(created);
    }

    /**
     * GET /api/webhooks — List all webhooks.
     */
    @GetMapping
    public ResponseEntity<List<WebhookConfig>> getAllWebhooks() {
        return ResponseEntity.ok(webhookConfigService.getAllWebhooks());
    }

    /**
     * GET /api/webhooks/{id} — Get a specific webhook.
     */
    @GetMapping("/{id}")
    public ResponseEntity<WebhookConfig> getWebhook(@PathVariable Long id) {
        return ResponseEntity.ok(webhookConfigService.getById(id));
    }

    /**
     * PUT /api/webhooks/{id} — Update a webhook.
     */
    @PutMapping("/{id}")
    public ResponseEntity<WebhookConfig> updateWebhook(
            @PathVariable Long id,
            @RequestBody WebhookConfig updates) {
        return ResponseEntity.ok(webhookConfigService.updateWebhook(id, updates));
    }

    /**
     * DELETE /api/webhooks/{id} — Delete a webhook.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWebhook(@PathVariable Long id) {
        if (webhookConfigService.deleteWebhook(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * PUT /api/webhooks/{id}/toggle — Toggle active status.
     */
    @PutMapping("/{id}/toggle")
    public ResponseEntity<WebhookConfig> toggleActive(@PathVariable Long id) {
        return ResponseEntity.ok(webhookConfigService.toggleActive(id));
    }

    /**
     * PUT /api/webhooks/deactivate-all — Deactivate all webhooks.
     */
    @PutMapping("/deactivate-all")
    public ResponseEntity<Map<String, Integer>> deactivateAll() {
        int count = webhookConfigService.deactivateAll();
        return ResponseEntity.ok(Map.of("deactivated", count));
    }

    /**
     * GET /api/webhooks/search?name=... — Search by name.
     */
    @GetMapping("/search")
    public ResponseEntity<List<WebhookConfig>> searchByName(@RequestParam String name) {
        return ResponseEntity.ok(webhookConfigService.searchByName(name));
    }

    /**
     * GET /api/webhooks/unhealthy?threshold=0.3 — Webhooks with high failure rates.
     */
    @GetMapping("/unhealthy")
    public ResponseEntity<List<WebhookConfig>> getUnhealthyWebhooks(
            @RequestParam(defaultValue = "0.3") double threshold) {
        return ResponseEntity.ok(webhookConfigService.getUnhealthyWebhooks(threshold));
    }

    // ---- Event Dispatch ----

    /**
     * POST /api/webhooks/dispatch — Manually dispatch an event to matching webhooks.
     */
    @PostMapping("/dispatch")
    public ResponseEntity<Map<String, String>> dispatchEvent(@RequestBody Map<String, Object> body) {
        String eventType = (String) body.get("eventType");
        @SuppressWarnings("unchecked")
        Map<String, Object> eventData = (Map<String, Object>) body.getOrDefault("data", Map.of());
        webhookConfigService.dispatchEvent(eventType, eventData);
        return ResponseEntity.ok(Map.of("status", "dispatched", "event", eventType));
    }

    // ---- Delivery Logs ----

    /**
     * GET /api/webhooks/{id}/logs — Delivery logs for a specific webhook.
     */
    @GetMapping("/{id}/logs")
    public ResponseEntity<List<WebhookDeliveryLog>> getDeliveryLogs(@PathVariable Long id) {
        return ResponseEntity.ok(webhookConfigService.getDeliveryLogs(id));
    }

    /**
     * GET /api/webhooks/logs/status/{status} — Logs filtered by delivery status.
     */
    @GetMapping("/logs/status/{status}")
    public ResponseEntity<List<WebhookDeliveryLog>> getLogsByStatus(@PathVariable String status) {
        return ResponseEntity.ok(webhookConfigService.getDeliveryLogsByStatus(status));
    }

    /**
     * GET /api/webhooks/logs/recent-failures?limit=10 — Recent failed deliveries.
     */
    @GetMapping("/logs/recent-failures")
    public ResponseEntity<List<WebhookDeliveryLog>> getRecentFailures(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(webhookConfigService.getRecentFailures(limit));
    }

    // ---- Statistics ----

    /**
     * GET /api/webhooks/{id}/stats — Delivery statistics for a specific webhook.
     */
    @GetMapping("/{id}/stats")
    public ResponseEntity<Map<String, Long>> getWebhookStats(@PathVariable Long id) {
        return ResponseEntity.ok(webhookConfigService.getDeliveryStats(id));
    }

    /**
     * GET /api/webhooks/stats — Global delivery summary across all webhooks.
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getGlobalStats() {
        return ResponseEntity.ok(webhookConfigService.getGlobalDeliverySummary());
    }

    // ---- Maintenance ----

    /**
     * DELETE /api/webhooks/purge — Purge old delivery logs.
     */
    @DeleteMapping("/purge")
    public ResponseEntity<Map<String, Integer>> purgeLogs(
            @RequestParam(defaultValue = "30") int olderThanDays) {
        int purged = webhookConfigService.purgeOldLogs(olderThanDays);
        return ResponseEntity.ok(Map.of("purged", purged));
    }
}
