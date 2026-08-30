package com.email.writer;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller exposing the Draft Version Management API.
 * Supports creating versions, browsing history, reverting, and diffing.
 */
@RestController
@RequestMapping("/api/drafts")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class DraftVersionController {

    private final DraftVersionService draftVersionService;

    public DraftVersionController(DraftVersionService draftVersionService) {
        this.draftVersionService = draftVersionService;
    }

    /**
     * POST /api/drafts — Save a new draft version (generic).
     */
    @PostMapping
    public ResponseEntity<DraftVersion> saveVersion(@RequestBody DraftVersion draft) {
        return ResponseEntity.ok(draftVersionService.saveVersion(draft));
    }

    /**
     * POST /api/drafts/ai — Save an AI-generated version.
     */
    @PostMapping("/ai")
    public ResponseEntity<DraftVersion> saveAiGeneration(@RequestBody DraftVersion draft) {
        DraftVersion saved = draftVersionService.saveAiGeneration(
                draft.getSessionId(), draft.getContent(),
                draft.getTone(), draft.getLanguage(), draft.getProvider(),
                draft.getOriginalEmailContent(), draft.getGeneratedReply(),
                draft.getGenerationTimeMs());
        return ResponseEntity.ok(saved);
    }

    /**
     * POST /api/drafts/user-edit — Save a user-edited version.
     */
    @PostMapping("/user-edit")
    public ResponseEntity<DraftVersion> saveUserEdit(@RequestBody Map<String, String> body) {
        DraftVersion saved = draftVersionService.saveUserEdit(
                body.get("sessionId"), body.get("content"), body.get("label"));
        return ResponseEntity.ok(saved);
    }

    /**
     * GET /api/drafts/sessions — List all session IDs.
     */
    @GetMapping("/sessions")
    public ResponseEntity<List<String>> getAllSessions() {
        return ResponseEntity.ok(draftVersionService.getAllSessionIds());
    }

    /**
     * GET /api/drafts/sessions/popular — Most active sessions by version count.
     */
    @GetMapping("/sessions/popular")
    public ResponseEntity<List<Map<String, Object>>> getPopularSessions() {
        return ResponseEntity.ok(draftVersionService.getMostActiveSessions());
    }

    /**
     * GET /api/drafts/sessions/{sessionId} — All versions in a session.
     */
    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<List<DraftVersion>> getSessionVersions(@PathVariable String sessionId) {
        return ResponseEntity.ok(draftVersionService.getSessionVersions(sessionId));
    }

    /**
     * GET /api/drafts/sessions/{sessionId}/active — Active version in a session.
     */
    @GetMapping("/sessions/{sessionId}/active")
    public ResponseEntity<DraftVersion> getActiveVersion(@PathVariable String sessionId) {
        return draftVersionService.getActiveVersion(sessionId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/drafts/sessions/{sessionId}/v/{version} — Specific version.
     */
    @GetMapping("/sessions/{sessionId}/v/{version}")
    public ResponseEntity<DraftVersion> getVersion(
            @PathVariable String sessionId, @PathVariable int version) {
        return draftVersionService.getVersion(sessionId, version)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * PUT /api/drafts/sessions/{sessionId}/revert/{version} — Revert to a version.
     */
    @PutMapping("/sessions/{sessionId}/revert/{version}")
    public ResponseEntity<DraftVersion> revertToVersion(
            @PathVariable String sessionId, @PathVariable int version) {
        return ResponseEntity.ok(draftVersionService.revertToVersion(sessionId, version));
    }

    /**
     * GET /api/drafts/sessions/{sessionId}/diff/{from}/{to} — Character diff.
     */
    @GetMapping("/sessions/{sessionId}/diff/{from}/{to}")
    public ResponseEntity<List<DraftVersionService.DiffOperation>> getDiff(
            @PathVariable String sessionId, @PathVariable int from, @PathVariable int to) {
        return ResponseEntity.ok(draftVersionService.computeDiff(sessionId, from, to));
    }

    /**
     * GET /api/drafts/sessions/{sessionId}/diff/{from}/{to}/lines — Line diff.
     */
    @GetMapping("/sessions/{sessionId}/diff/{from}/{to}/lines")
    public ResponseEntity<List<DraftVersionService.DiffLine>> getLineDiff(
            @PathVariable String sessionId, @PathVariable int from, @PathVariable int to) {
        return ResponseEntity.ok(draftVersionService.computeLineDiff(sessionId, from, to));
    }

    /**
     * GET /api/drafts/sessions/{sessionId}/diff/{from}/{to}/summary — Diff summary.
     */
    @GetMapping("/sessions/{sessionId}/diff/{from}/{to}/summary")
    public ResponseEntity<DraftVersionService.DiffSummary> getDiffSummary(
            @PathVariable String sessionId, @PathVariable int from, @PathVariable int to) {
        return ResponseEntity.ok(draftVersionService.getDiffSummary(sessionId, from, to));
    }

    /**
     * DELETE /api/drafts/sessions/{sessionId} — Delete all versions in a session.
     */
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Void> deleteSession(@PathVariable String sessionId) {
        if (draftVersionService.deleteSession(sessionId)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * GET /api/drafts/stats — Global stats.
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(Map.of(
                "totalVersions", draftVersionService.getTotalVersionCount(),
                "totalSessions", draftVersionService.getAllSessionIds().size()
        ));
    }

    /**
     * DELETE /api/drafts/purge — Purge old versions.
     */
    @DeleteMapping("/purge")
    public ResponseEntity<Map<String, Integer>> purgeOldVersions(
            @RequestParam(defaultValue = "30") int olderThanDays) {
        int purged = draftVersionService.purgeOldVersions(olderThanDays);
        return ResponseEntity.ok(Map.of("purged", purged));
    }
}
