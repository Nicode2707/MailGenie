package com.email.writer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    @Autowired
    private AnalyticsService analyticsService;

    // ===== Event Tracking =====

    @PostMapping("/events")
    public ResponseEntity<EmailEvent> trackEvent(@RequestBody TrackEventRequest request) {
        return ResponseEntity.ok(analyticsService.trackEvent(request));
    }

    @PostMapping("/events/batch")
    public ResponseEntity<List<EmailEvent>> batchTrackEvents(@RequestBody List<TrackEventRequest> requests) {
        return ResponseEntity.ok(analyticsService.batchTrackEvents(requests));
    }

    @GetMapping("/events/user/{userId}")
    public ResponseEntity<List<EmailEvent>> getUserEvents(@PathVariable String userId) {
        return ResponseEntity.ok(analyticsService.getUserEvents(userId));
    }

    @GetMapping("/events/user/{userId}/type/{eventType}")
    public ResponseEntity<List<EmailEvent>> getEventsByType(@PathVariable String userId,
                                                             @PathVariable String eventType) {
        return ResponseEntity.ok(analyticsService.getEventsByType(userId, eventType));
    }

    @GetMapping("/events/campaign/{campaignId}")
    public ResponseEntity<List<EmailEvent>> getCampaignEvents(@PathVariable Long campaignId) {
        return ResponseEntity.ok(analyticsService.getCampaignEvents(campaignId));
    }

    @GetMapping("/events/recipient/{email}")
    public ResponseEntity<List<EmailEvent>> getRecipientEvents(@PathVariable String email) {
        return ResponseEntity.ok(analyticsService.getRecipientEvents(email));
    }

    @GetMapping("/events/range")
    public ResponseEntity<List<EmailEvent>> getEventsInDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(analyticsService.getEventsInDateRange(start, end));
    }

    // ===== Engagement Scoring =====

    @GetMapping("/engagement/{email}")
    public ResponseEntity<EngagementScore> getEngagementScore(@PathVariable String email) {
        return ResponseEntity.ok(analyticsService.getEngagementScore(email));
    }

    @PutMapping("/engagement/{email}/recalculate")
    public ResponseEntity<EngagementScore> recalculateScore(@PathVariable String email) {
        return ResponseEntity.ok(analyticsService.recalculateScore(email));
    }

    @GetMapping("/engagement/user/{userId}/top")
    public ResponseEntity<List<EngagementScore>> getTopEngaged(@PathVariable String userId) {
        return ResponseEntity.ok(analyticsService.getTopEngaged(userId));
    }

    @GetMapping("/engagement/user/{userId}/top/min-sends/{minSends}")
    public ResponseEntity<List<EngagementScore>> getTopEngagedWithMinSends(
            @PathVariable String userId, @PathVariable long minSends) {
        return ResponseEntity.ok(analyticsService.getTopEngagedWithMinSends(userId, minSends));
    }

    @GetMapping("/engagement/user/{userId}/tier/{tier}")
    public ResponseEntity<List<EngagementScore>> getRecipientsByTier(
            @PathVariable String userId, @PathVariable String tier) {
        return ResponseEntity.ok(analyticsService.getRecipientsByTier(userId, tier));
    }

    @GetMapping("/engagement/at-risk")
    public ResponseEntity<List<EngagementScore>> getAtRiskRecipients() {
        return ResponseEntity.ok(analyticsService.getAtRiskRecipients(null));
    }

    @GetMapping("/engagement/vip")
    public ResponseEntity<List<EngagementScore>> getVipRecipients() {
        return ResponseEntity.ok(analyticsService.getVipRecipients(null));
    }

    @GetMapping("/engagement/user/{userId}/inactive/days/{days}")
    public ResponseEntity<List<EngagementScore>> getInactiveRecipients(
            @PathVariable String userId, @PathVariable int days) {
        return ResponseEntity.ok(analyticsService.getInactiveRecipients(userId, days));
    }

    @GetMapping("/engagement/user/{userId}/below/{threshold}")
    public ResponseEntity<List<EngagementScore>> getBelowThreshold(
            @PathVariable String userId, @PathVariable double threshold) {
        return ResponseEntity.ok(analyticsService.getBelowThresholdRecipients(userId, threshold));
    }

    @GetMapping("/engagement/user/{userId}/tiers")
    public ResponseEntity<Map<String, Long>> getTierBreakdown(@PathVariable String userId) {
        return ResponseEntity.ok(analyticsService.getTierBreakdown(userId));
    }

    // ===== Audience Segments =====

    @PostMapping("/segments")
    public ResponseEntity<AudienceSegment> createSegment(@RequestBody SegmentRequest request) {
        return ResponseEntity.ok(analyticsService.createSegment(request));
    }

    @GetMapping("/segments/user/{userId}")
    public ResponseEntity<List<AudienceSegment>> getUserSegments(@PathVariable String userId) {
        return ResponseEntity.ok(analyticsService.getUserSegments(userId));
    }

    @GetMapping("/segments/user/{userId}/active")
    public ResponseEntity<List<AudienceSegment>> getActiveSegments(@PathVariable String userId) {
        return ResponseEntity.ok(analyticsService.getActiveSegments(userId));
    }

    @GetMapping("/segments/user/{userId}/non-empty")
    public ResponseEntity<List<AudienceSegment>> getNonEmptySegments(@PathVariable String userId) {
        return ResponseEntity.ok(analyticsService.getNonEmptySegments(userId));
    }

    @GetMapping("/segments/{segmentId}")
    public ResponseEntity<AudienceSegment> getSegmentById(@PathVariable Long segmentId) {
        return ResponseEntity.ok(analyticsService.getSegmentById(segmentId));
    }

    @PutMapping("/segments/{segmentId}/refresh")
    public ResponseEntity<AudienceSegment> refreshSegment(@PathVariable Long segmentId) {
        return ResponseEntity.ok(analyticsService.updateSegmentMemberCount(segmentId));
    }

    @DeleteMapping("/segments/{segmentId}")
    public ResponseEntity<Void> deleteSegment(@PathVariable Long segmentId) {
        analyticsService.deleteSegment(segmentId);
        return ResponseEntity.ok().build();
    }

    // ===== Dashboard =====

    @GetMapping("/dashboard/{userId}")
    public ResponseEntity<AnalyticsDashboard> getDashboard(@PathVariable String userId) {
        return ResponseEntity.ok(analyticsService.getDashboard(userId));
    }

    @GetMapping("/leaders/top-openers/{userId}")
    public ResponseEntity<List<Object[]>> getTopOpeners(@PathVariable String userId) {
        return ResponseEntity.ok(analyticsService.getTopOpeners(userId));
    }

    @GetMapping("/leaders/top-clickers/{userId}")
    public ResponseEntity<List<Object[]>> getTopClickers(@PathVariable String userId) {
        return ResponseEntity.ok(analyticsService.getTopClickers(userId));
    }
}
