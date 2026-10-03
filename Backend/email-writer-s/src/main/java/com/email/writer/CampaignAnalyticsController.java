package com.email.writer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for email campaign analytics and A/B testing endpoints.
 */
@RestController
@RequestMapping("/api/campaigns")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173", "*"})
@Slf4j
public class CampaignAnalyticsController {

    private final CampaignAnalyticsService campaignService;

    // ─── Campaign CRUD ─────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<EmailCampaign> createCampaign(@RequestBody CampaignCreateRequest request) {
        return ResponseEntity.ok(campaignService.createCampaign(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmailCampaign> getCampaign(@PathVariable Long id) {
        return ResponseEntity.ok(campaignService.getCampaign(id));
    }

    @GetMapping
    public ResponseEntity<List<EmailCampaign>> listCampaigns(@RequestParam(required = false) String status) {
        return ResponseEntity.ok(campaignService.listCampaigns(status));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Boolean>> deleteCampaign(@PathVariable Long id) {
        boolean deleted = campaignService.deleteCampaign(id);
        return ResponseEntity.ok(Map.of("deleted", deleted));
    }

    // ─── Lifecycle ─────────────────────────────────────────────────────

    @PostMapping("/{id}/start")
    public ResponseEntity<EmailCampaign> startCampaign(@PathVariable Long id) {
        return ResponseEntity.ok(campaignService.startCampaign(id));
    }

    @PostMapping("/{id}/pause")
    public ResponseEntity<EmailCampaign> pauseCampaign(@PathVariable Long id) {
        return ResponseEntity.ok(campaignService.pauseCampaign(id));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<EmailCampaign> completeCampaign(@PathVariable Long id) {
        return ResponseEntity.ok(campaignService.completeCampaign(id));
    }

    // ─── Metrics ───────────────────────────────────────────────────────

    @PostMapping("/{id}/metrics")
    public ResponseEntity<CampaignMetric> recordEvent(@PathVariable Long id, @RequestBody MetricEventRequest request) {
        return ResponseEntity.ok(campaignService.recordEvent(id, request));
    }

    @PostMapping("/{id}/metrics/bulk")
    public ResponseEntity<Map<String, Integer>> recordEventsBulk(
            @PathVariable Long id, @RequestBody List<MetricEventRequest> events) {
        int recorded = campaignService.recordEventsBulk(id, events);
        return ResponseEntity.ok(Map.of("recorded", recorded, "total", events.size()));
    }

    // ─── Winner Selection ──────────────────────────────────────────────

    @PostMapping("/{id}/winner/{variantId}")
    public ResponseEntity<CampaignVariant> selectWinner(
            @PathVariable Long id, @PathVariable Long variantId) {
        return ResponseEntity.ok(campaignService.selectWinnerManually(id, variantId));
    }

    // ─── Analytics ─────────────────────────────────────────────────────

    @GetMapping("/{id}/analytics")
    public ResponseEntity<CampaignAnalyticsResponse> getCampaignAnalytics(@PathVariable Long id) {
        return ResponseEntity.ok(campaignService.getCampaignAnalytics(id));
    }

    @GetMapping("/{id}/variants")
    public ResponseEntity<List<CampaignVariant>> getVariants(@PathVariable Long id) {
        return ResponseEntity.ok(campaignService.getVariants(id));
    }

    @GetMapping("/variant/{variantId}/metrics")
    public ResponseEntity<List<Object[]>> getVariantMetrics(@PathVariable Long variantId) {
        return ResponseEntity.ok(campaignService.getVariantMetrics(variantId));
    }

    @GetMapping("/variant/{variantId}/timing")
    public ResponseEntity<Map<String, Double>> getTimingAnalytics(@PathVariable Long variantId) {
        return ResponseEntity.ok(campaignService.getTimeToEventAnalytics(variantId));
    }

    @GetMapping("/stats")
    public ResponseEntity<CampaignStats> getSystemStats() {
        return ResponseEntity.ok(campaignService.getSystemStats());
    }
}
