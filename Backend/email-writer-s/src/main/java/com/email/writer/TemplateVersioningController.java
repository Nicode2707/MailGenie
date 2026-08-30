package com.email.writer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for template versioning and collaboration endpoints.
 * Provides version management, diff comparison, review workflows, locking, rollback, and analytics.
 */
@RestController
@RequestMapping("/api/templates/versions")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173", "*"})
@Slf4j
public class TemplateVersioningController {

    private final TemplateVersioningService versioningService;

    // ─── Version CRUD ──────────────────────────────────────────────────

    /**
     * Create a new version of a template.
     */
    @PostMapping
    public ResponseEntity<TemplateVersion> createVersion(@RequestBody TemplateVersionCreateRequest request) {
        return ResponseEntity.ok(versioningService.createVersion(request));
    }

    /**
     * Create a version from the current live template.
     */
    @PostMapping("/from-template/{templateId}")
    public ResponseEntity<TemplateVersion> createFromTemplate(
            @PathVariable Long templateId,
            @RequestParam(required = false) String changeDescription,
            @RequestParam(required = false) String author) {
        return ResponseEntity.ok(versioningService.createVersionFromTemplate(templateId, changeDescription, author));
    }

    /**
     * Get all versions for a template.
     */
    @GetMapping("/template/{templateId}")
    public ResponseEntity<List<TemplateVersion>> getVersions(@PathVariable Long templateId) {
        return ResponseEntity.ok(versioningService.getVersions(templateId));
    }

    /**
     * Get a specific version.
     */
    @GetMapping("/template/{templateId}/v/{versionNumber}")
    public ResponseEntity<TemplateVersion> getVersion(
            @PathVariable Long templateId, @PathVariable Integer versionNumber) {
        return ResponseEntity.ok(versioningService.getVersion(templateId, versionNumber));
    }

    /**
     * Get the current (published) version of a template.
     */
    @GetMapping("/template/{templateId}/current")
    public ResponseEntity<TemplateVersion> getCurrentVersion(@PathVariable Long templateId) {
        return ResponseEntity.ok(versioningService.getCurrentVersion(templateId));
    }

    // ─── Diff & Compare ────────────────────────────────────────────────

    /**
     * Calculate diff between two versions.
     */
    @GetMapping("/template/{templateId}/diff")
    public ResponseEntity<TemplateDiffResult> diffVersions(
            @PathVariable Long templateId,
            @RequestParam Integer from,
            @RequestParam Integer to) {
        return ResponseEntity.ok(versioningService.diffVersions(templateId, from, to));
    }

    // ─── Publishing & Rollback ─────────────────────────────────────────

    /**
     * Publish a version — make it the current active version.
     */
    @PostMapping("/template/{templateId}/v/{versionNumber}/publish")
    public ResponseEntity<TemplateVersion> publishVersion(
            @PathVariable Long templateId, @PathVariable Integer versionNumber) {
        return ResponseEntity.ok(versioningService.publishVersion(templateId, versionNumber));
    }

    /**
     * Rollback to a previous version (creates a new version with old content).
     */
    @PostMapping("/template/{templateId}/rollback/{targetVersion}")
    public ResponseEntity<TemplateVersion> rollbackVersion(
            @PathVariable Long templateId,
            @PathVariable Integer targetVersion,
            @RequestParam(required = false) String author) {
        return ResponseEntity.ok(versioningService.rollbackToVersion(templateId, targetVersion, author));
    }

    // ─── Locking ───────────────────────────────────────────────────────

    /**
     * Acquire an editing lock on a version.
     */
    @PostMapping("/{versionId}/lock")
    public ResponseEntity<Map<String, Object>> acquireLock(
            @PathVariable Long versionId,
            @RequestParam String lockToken,
            @RequestParam(defaultValue = "30") int durationMinutes) {
        boolean acquired = versioningService.acquireLock(versionId, lockToken, durationMinutes);
        return ResponseEntity.ok(Map.of("locked", acquired));
    }

    /**
     * Release an editing lock.
     */
    @PostMapping("/{versionId}/unlock")
    public ResponseEntity<Map<String, Boolean>> releaseLock(
            @PathVariable Long versionId, @RequestParam String lockToken) {
        boolean released = versioningService.releaseLock(versionId, lockToken);
        return ResponseEntity.ok(Map.of("unlocked", released));
    }

    /**
     * Get all currently locked versions.
     */
    @GetMapping("/locked")
    public ResponseEntity<List<TemplateVersion>> getLockedVersions() {
        return ResponseEntity.ok(versioningService.getLockedVersions());
    }

    // ─── Reviews ───────────────────────────────────────────────────────

    /**
     * Submit a review for a template version.
     */
    @PostMapping("/{versionId}/reviews")
    public ResponseEntity<TemplateReview> submitReview(
            @PathVariable Long versionId, @RequestBody TemplateReviewRequest request) {
        return ResponseEntity.ok(versioningService.submitReview(versionId, request));
    }

    /**
     * Get all reviews for a version.
     */
    @GetMapping("/{versionId}/reviews")
    public ResponseEntity<List<TemplateReview>> getReviews(@PathVariable Long versionId) {
        return ResponseEntity.ok(versioningService.getReviews(versionId));
    }

    /**
     * Get pending reviews for a specific reviewer.
     */
    @GetMapping("/reviews/pending/{reviewer}")
    public ResponseEntity<List<TemplateReview>> getPendingReviews(@PathVariable String reviewer) {
        return ResponseEntity.ok(versioningService.getPendingReviews(reviewer));
    }

    /**
     * Get all pending reviews.
     */
    @GetMapping("/reviews/pending")
    public ResponseEntity<List<TemplateReview>> getAllPendingReviews() {
        return ResponseEntity.ok(versioningService.getAllPendingReviews());
    }

    /**
     * Get average rating for a version.
     */
    @GetMapping("/{versionId}/rating")
    public ResponseEntity<Map<String, Double>> getAverageRating(@PathVariable Long versionId) {
        Double avg = versioningService.getAverageRating(versionId);
        return ResponseEntity.ok(Map.of("averageRating", avg != null ? avg : 0.0));
    }

    // ─── Search & Filter ───────────────────────────────────────────────

    /**
     * Find versions by tag.
     */
    @GetMapping("/tag/{tag}")
    public ResponseEntity<List<TemplateVersion>> getVersionsByTag(@PathVariable String tag) {
        return ResponseEntity.ok(versioningService.getVersionsByTag(tag));
    }

    /**
     * Find versions by author.
     */
    @GetMapping("/author/{author}")
    public ResponseEntity<List<TemplateVersion>> getVersionsByAuthor(@PathVariable String author) {
        return ResponseEntity.ok(versioningService.getVersionsByAuthor(author));
    }

    /**
     * Get version count for a template.
     */
    @GetMapping("/template/{templateId}/count")
    public ResponseEntity<Map<String, Long>> getVersionCount(@PathVariable Long templateId) {
        return ResponseEntity.ok(Map.of("versionCount", versioningService.getVersionCount(templateId)));
    }

    // ─── Analytics ─────────────────────────────────────────────────────

    /**
     * Get comprehensive versioning and collaboration statistics.
     */
    @GetMapping("/stats")
    public ResponseEntity<TemplateVersionStats> getStats() {
        return ResponseEntity.ok(versioningService.getStats());
    }
}
