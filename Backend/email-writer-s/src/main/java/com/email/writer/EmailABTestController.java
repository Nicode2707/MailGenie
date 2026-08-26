package com.email.writer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * REST Controller for Email A/B Split Testing.
 * Provides endpoints for creating campaigns, retrieving results, and analytics.
 */
@RestController
@RequestMapping("/api/ab-test")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class EmailABTestController {

    @Autowired
    private EmailABTestService abTestService;

    /**
     * Create a new A/B test campaign.
     */
    @PostMapping("/campaigns")
    public ResponseEntity<Map<String, Object>> createCampaign(@RequestBody Map<String, Object> payload) {
        String campaignName = (String) payload.getOrDefault("campaignName", "Untitled Campaign");
        String testType = (String) payload.getOrDefault("testType", "full_email");

        List<Map<String, Object>> variantInputs = (List<Map<String, Object>>) payload.get("variants");
        if (variantInputs == null || variantInputs.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "At least one variant is required"));
        }

        String[] labels = {"A", "B", "C", "D", "E", "F"};
        List<EmailABTestVariant> variants = new ArrayList<>();
        for (int i = 0; i < variantInputs.size(); i++) {
            Map<String, Object> v = variantInputs.get(i);
            String label = i < labels.length ? labels[i] : "V" + (i + 1);
            variants.add(new EmailABTestVariant(
                    "VAR-" + UUID.randomUUID().toString().substring(0, 6),
                    label,
                    (String) v.getOrDefault("subjectLine", ""),
                    (String) v.getOrDefault("bodyContent", "")
            ));
        }

        Map<String, Object> campaign = abTestService.createCampaign(campaignName, testType, variants);
        return ResponseEntity.ok(campaign);
    }

    /**
     * Get all A/B test campaigns.
     */
    @GetMapping("/campaigns")
    public ResponseEntity<List<Map<String, Object>>> getAllCampaigns() {
        return ResponseEntity.ok(abTestService.getAllCampaigns());
    }

    /**
     * Get a specific campaign by ID.
     */
    @GetMapping("/campaigns/{campaignId}")
    public ResponseEntity<Map<String, Object>> getCampaign(@PathVariable String campaignId) {
        return abTestService.getCampaignById(campaignId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Delete a campaign by ID.
     */
    @DeleteMapping("/campaigns/{campaignId}")
    public ResponseEntity<Void> deleteCampaign(@PathVariable String campaignId) {
        if (abTestService.deleteCampaign(campaignId)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Get A/B test summary statistics.
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getTestStatistics() {
        return ResponseEntity.ok(abTestService.getTestStatistics());
    }
}
