package com.email.writer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * REST controller for email deliverability tracking, sender reputation scoring,
 * blocklist monitoring, and domain health reporting.
 */
@RestController
@RequestMapping("/api/deliverability")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173", "*"})
@Slf4j
public class DeliverabilityController {

    private final DeliverabilityService deliverabilityService;

    // ─── Event Recording ───────────────────────────────────────────────

    /**
     * Record a deliverability event.
     */
    @PostMapping("/events")
    public ResponseEntity<DeliverabilityRecord> recordEvent(@RequestBody DeliverabilityRecordRequest request) {
        return ResponseEntity.ok(deliverabilityService.recordEvent(request));
    }

    /**
     * Record multiple events in bulk.
     */
    @PostMapping("/events/bulk")
    public ResponseEntity<Map<String, Integer>> recordEventsBulk(@RequestBody List<DeliverabilityRecordRequest> events) {
        int recorded = deliverabilityService.recordEventsBulk(events);
        return ResponseEntity.ok(Map.of("recorded", recorded, "total", events.size()));
    }

    // ─── Reputation ────────────────────────────────────────────────────

    /**
     * Get all sender reputations ranked by score.
     */
    @GetMapping("/reputations")
    public ResponseEntity<List<SenderReputation>> getAllReputations() {
        return ResponseEntity.ok(deliverabilityService.getAllReputations());
    }

    /**
     * Get reputation for a specific domain.
     */
    @GetMapping("/reputations/{domain}")
    public ResponseEntity<SenderReputation> getReputation(@PathVariable String domain) {
        return ResponseEntity.ok(deliverabilityService.getReputation(domain));
    }

    /**
     * Force recalculate reputation for a domain.
     */
    @PostMapping("/reputations/{domain}/recalculate")
    public ResponseEntity<SenderReputation> recalculateReputation(@PathVariable String domain) {
        return ResponseEntity.ok(deliverabilityService.updateSenderReputation(domain));
    }

    /**
     * Get domains at risk.
     */
    @GetMapping("/reputations/at-risk")
    public ResponseEntity<List<SenderReputation>> getAtRiskDomains() {
        return ResponseEntity.ok(deliverabilityService.getAtRiskDomains());
    }

    /**
     * Get domains with high spam complaint rates.
     */
    @GetMapping("/reputations/high-complaints")
    public ResponseEntity<List<SenderReputation>> getHighComplaintDomains(
            @RequestParam(defaultValue = "0.1") double threshold) {
        return ResponseEntity.ok(deliverabilityService.getHighComplaintDomains(threshold));
    }

    // ─── Domain Health ─────────────────────────────────────────────────

    /**
     * Get comprehensive health report for a domain.
     */
    @GetMapping("/health/{domain}")
    public ResponseEntity<DomainHealthReport> getDomainHealthReport(@PathVariable String domain) {
        return ResponseEntity.ok(deliverabilityService.getDomainHealthReport(domain));
    }

    /**
     * Get deliverability records for a domain within a time range.
     */
    @GetMapping("/records/{domain}")
    public ResponseEntity<List<DeliverabilityRecord>> getRecords(
            @PathVariable String domain,
            @RequestParam(required = false) LocalDateTime start,
            @RequestParam(required = false) LocalDateTime end) {
        if (start != null && end != null) {
            return ResponseEntity.ok(deliverabilityService.getRecordsInRange(domain, start, end));
        }
        return ResponseEntity.ok(deliverabilityService.getRecordsInRange(domain,
                LocalDateTime.now().minusDays(30), LocalDateTime.now()));
    }

    // ─── Blocklist ─────────────────────────────────────────────────────

    /**
     * Check and record blocklist status for a domain.
     */
    @PostMapping("/blocklist/check")
    public ResponseEntity<DomainBlocklistEntry> checkBlocklist(
            @RequestParam String domain,
            @RequestParam String blocklistName,
            @RequestParam boolean isListed) {
        return ResponseEntity.ok(deliverabilityService.checkBlocklist(domain, blocklistName, isListed));
    }

    /**
     * Get all active blocklist entries.
     */
    @GetMapping("/blocklist/active")
    public ResponseEntity<List<DomainBlocklistEntry>> getActiveBlocklistEntries() {
        return ResponseEntity.ok(deliverabilityService.getActiveBlocklistEntries());
    }

    /**
     * Get blocklist entries for a specific domain.
     */
    @GetMapping("/blocklist/{domain}")
    public ResponseEntity<List<DomainBlocklistEntry>> getDomainBlocklistEntries(@PathVariable String domain) {
        return ResponseEntity.ok(deliverabilityService.getDomainBlocklistEntries(domain));
    }

    // ─── Stats ─────────────────────────────────────────────────────────

    /**
     * Get system-wide deliverability and reputation statistics.
     */
    @GetMapping("/stats")
    public ResponseEntity<ReputationStats> getSystemStats() {
        return ResponseEntity.ok(deliverabilityService.getSystemStats());
    }
}
