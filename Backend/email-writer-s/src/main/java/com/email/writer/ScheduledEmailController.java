package com.email.writer;

import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * REST controller exposing the full scheduling API.
 * All endpoints are prefixed with /api/schedule.
 */
@RestController
@RequestMapping("/api/schedule")
@AllArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class ScheduledEmailController {

    private final ScheduledEmailService scheduledEmailService;

    /**
     * POST /api/schedule — Create a new scheduled email.
     */
    @PostMapping
    public ResponseEntity<ScheduledEmail> createScheduledEmail(
            @RequestBody ScheduledEmailRequest request) {
        ScheduledEmail created = scheduledEmailService.createScheduledEmail(request);
        return ResponseEntity.ok(created);
    }

    /**
     * GET /api/schedule — List all scheduled emails.
     */
    @GetMapping
    public ResponseEntity<List<ScheduledEmail>> getAllScheduledEmails() {
        return ResponseEntity.ok(scheduledEmailService.getAllScheduledEmails());
    }

    /**
     * GET /api/schedule/{id} — Get a specific scheduled email.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ScheduledEmail> getScheduledEmail(@PathVariable Long id) {
        return ResponseEntity.ok(scheduledEmailService.getById(id));
    }

    /**
     * PUT /api/schedule/{id}/cancel — Cancel a pending email.
     */
    @PutMapping("/{id}/cancel")
    public ResponseEntity<ScheduledEmail> cancelEmail(@PathVariable Long id) {
        return ResponseEntity.ok(scheduledEmailService.cancelEmail(id));
    }

    /**
     * PUT /api/schedule/cancel-all — Cancel all pending emails.
     */
    @PutMapping("/cancel-all")
    public ResponseEntity<Map<String, Integer>> cancelAllPending() {
        int cancelled = scheduledEmailService.cancelAllPending();
        return ResponseEntity.ok(Map.of("cancelled", cancelled));
    }

    /**
     * PUT /api/schedule/{id}/reschedule — Reschedule to a new time.
     */
    @PutMapping("/{id}/reschedule")
    public ResponseEntity<ScheduledEmail> rescheduleEmail(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        LocalDateTime newTime = LocalDateTime.parse(body.get("scheduledAt"));
        return ResponseEntity.ok(scheduledEmailService.rescheduleEmail(id, newTime));
    }

    /**
     * PUT /api/schedule/{id}/label — Update the label.
     */
    @PutMapping("/{id}/label")
    public ResponseEntity<ScheduledEmail> updateLabel(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(scheduledEmailService.updateLabel(id, body.get("label")));
    }

    /**
     * GET /api/schedule/status — Summary of email counts by status.
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Long>> getStatusSummary() {
        return ResponseEntity.ok(scheduledEmailService.getStatusSummary());
    }

    /**
     * GET /api/schedule/upcoming — Next 7 days of pending emails.
     */
    @GetMapping("/upcoming")
    public ResponseEntity<List<ScheduledEmail>> getUpcoming() {
        return ResponseEntity.ok(scheduledEmailService.getUpcoming());
    }

    /**
     * GET /api/schedule/due — List emails ready for processing now.
     */
    @GetMapping("/due")
    public ResponseEntity<List<ScheduledEmail>> getDueEmails() {
        return ResponseEntity.ok(scheduledEmailService.findDueEmails());
    }

    /**
     * POST /api/schedule/process — Manually trigger processing of due emails.
     */
    @PostMapping("/process")
    public ResponseEntity<Map<String, Object>> processDueEmails() {
        List<ScheduledEmail> processed = scheduledEmailService.processDueEmails();
        return ResponseEntity.ok(Map.of(
                "processedCount", processed.size(),
                "processed", processed
        ));
    }

    /**
     * GET /api/schedule/label/{label} — Filter by label.
     */
    @GetMapping("/label/{label}")
    public ResponseEntity<List<ScheduledEmail>> getByLabel(@PathVariable String label) {
        return ResponseEntity.ok(scheduledEmailService.getByLabel(label));
    }

    /**
     * GET /api/schedule/status/{status} — Filter by status.
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<ScheduledEmail>> getByStatus(@PathVariable String status) {
        return ResponseEntity.ok(scheduledEmailService.getByStatus(status));
    }

    /**
     * DELETE /api/schedule/purge — Purge old completed emails.
     * Query param: olderThanDays (default 30).
     */
    @DeleteMapping("/purge")
    public ResponseEntity<Map<String, Integer>> purgeOldCompleted(
            @RequestParam(defaultValue = "30") int olderThanDays) {
        int purged = scheduledEmailService.purgeOldCompleted(olderThanDays);
        return ResponseEntity.ok(Map.of("purged", purged));
    }
}
