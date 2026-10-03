package com.email.writer;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * REST controller exposing the Email Categorization & Tagging API.
 */
@RestController
@RequestMapping("/api/categorize")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class EmailCategorizationController {

    private final EmailCategorizationService categorizationService;

    public EmailCategorizationController(EmailCategorizationService categorizationService) {
        this.categorizationService = categorizationService;
    }

    /**
     * POST /api/categorize — Categorize email content (persists result).
     */
    @PostMapping
    public ResponseEntity<EmailCategorizationResult> categorize(@RequestBody Map<String, String> body) {
        String content = body.get("content");
        return ResponseEntity.ok(categorizationService.categorize(content));
    }

    /**
     * POST /api/categorize/dry-run — Categorize without persisting.
     */
    @PostMapping("/dry-run")
    public ResponseEntity<EmailCategorizationResult> categorizeDryRun(@RequestBody Map<String, String> body) {
        String content = body.get("content");
        return ResponseEntity.ok(categorizationService.categorizeDryRun(content));
    }

    /**
     * POST /api/categorize/batch — Categorize multiple emails at once.
     */
    @PostMapping("/batch")
    public ResponseEntity<List<EmailCategorizationResult>> categorizeBatch(
            @RequestBody Map<String, List<String>> body) {
        List<String> contents = body.get("contents");
        List<EmailCategorizationResult> results = contents.stream()
                .map(categorizationService::categorizeDryRun)
                .toList();
        return ResponseEntity.ok(results);
    }

    /**
     * GET /api/categorize — List all categorized emails.
     */
    @GetMapping
    public ResponseEntity<List<EmailCategory>> getAll() {
        return ResponseEntity.ok(categorizationService.getAllCategories());
    }

    /**
     * GET /api/categorize/category/{category} — Filter by category.
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<List<EmailCategory>> getByCategory(@PathVariable String category) {
        return ResponseEntity.ok(categorizationService.getByCategory(category));
    }

    /**
     * GET /api/categorize/sentiment/{sentiment} — Filter by sentiment.
     */
    @GetMapping("/sentiment/{sentiment}")
    public ResponseEntity<List<EmailCategory>> getBySentiment(@PathVariable String sentiment) {
        return ResponseEntity.ok(categorizationService.getBySentiment(sentiment));
    }

    /**
     * GET /api/categorize/urgency/{urgency} — Filter by urgency.
     */
    @GetMapping("/urgency/{urgency}")
    public ResponseEntity<List<EmailCategory>> getByUrgency(@PathVariable String urgency) {
        return ResponseEntity.ok(categorizationService.getByUrgency(urgency));
    }

    /**
     * GET /api/categorize/tag/{tag} — Filter by tag.
     */
    @GetMapping("/tag/{tag}")
    public ResponseEntity<List<EmailCategory>> getByTag(@PathVariable String tag) {
        return ResponseEntity.ok(categorizationService.getByTag(tag));
    }

    /**
     * GET /api/categorize/confident/{category} — High-confidence records for a category.
     */
    @GetMapping("/confident/{category}")
    public ResponseEntity<List<EmailCategory>> getHighConfidenceByCategory(@PathVariable String category) {
        return ResponseEntity.ok(categorizationService.getHighConfidenceByCategory(category));
    }

    /**
     * GET /api/categorize/recent — 10 most recent records.
     */
    @GetMapping("/recent")
    public ResponseEntity<List<EmailCategory>> getRecent() {
        return ResponseEntity.ok(categorizationService.getRecent());
    }

    /**
     * GET /api/categorize/labeled — Records with user labels.
     */
    @GetMapping("/labeled")
    public ResponseEntity<List<EmailCategory>> getLabeled() {
        return ResponseEntity.ok(categorizationService.getLabeled());
    }

    /**
     * GET /api/categorize/analytics — Full analytics summary.
     */
    @GetMapping("/analytics")
    public ResponseEntity<Map<String, Object>> getAnalytics() {
        return ResponseEntity.ok(categorizationService.getAnalytics());
    }

    /**
     * PUT /api/categorize/{id}/label — Update user label.
     */
    @PutMapping("/{id}/label")
    public ResponseEntity<EmailCategory> updateLabel(
            @PathVariable Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(categorizationService.updateLabel(id, body.get("label")));
    }

    /**
     * DELETE /api/categorize/{id} — Delete a record.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecord(@PathVariable Long id) {
        if (categorizationService.deleteRecord(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * DELETE /api/categorize/purge — Purge old records.
     */
    @DeleteMapping("/purge")
    public ResponseEntity<Map<String, Integer>> purge(
            @RequestParam(defaultValue = "30") int olderThanDays) {
        int purged = categorizationService.purgeOld(olderThanDays);
        return ResponseEntity.ok(Map.of("purged", purged));
    }
}
