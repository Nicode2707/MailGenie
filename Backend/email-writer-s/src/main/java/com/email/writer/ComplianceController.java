package com.email.writer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/compliance")
public class ComplianceController {

    @Autowired
    private ComplianceService complianceService;

    // ===== Compliance Events =====

    @PostMapping("/events")
    public ResponseEntity<ComplianceEvent> createEvent(@RequestBody ComplianceEventRequest request) {
        return ResponseEntity.ok(complianceService.createComplianceEvent(request));
    }

    @GetMapping("/events/user/{userId}")
    public ResponseEntity<List<ComplianceEvent>> getEventsByUser(@PathVariable String userId) {
        return ResponseEntity.ok(complianceService.getEventsByUser(userId));
    }

    @GetMapping("/events/type/{eventType}")
    public ResponseEntity<List<ComplianceEvent>> getEventsByType(@PathVariable String eventType) {
        return ResponseEntity.ok(complianceService.getEventsByType(eventType));
    }

    @GetMapping("/events/pending")
    public ResponseEntity<List<ComplianceEvent>> getPendingEvents() {
        return ResponseEntity.ok(complianceService.getPendingEvents());
    }

    @GetMapping("/events/critical")
    public ResponseEntity<List<ComplianceEvent>> getCriticalEvents() {
        return ResponseEntity.ok(complianceService.getCriticalEvents());
    }

    @GetMapping("/events/overdue")
    public ResponseEntity<List<ComplianceEvent>> getOverdueEvents() {
        return ResponseEntity.ok(complianceService.getOverdueEvents());
    }

    @GetMapping("/events/search")
    public ResponseEntity<List<ComplianceEvent>> searchEvents(@RequestParam String keyword) {
        return ResponseEntity.ok(complianceService.searchEvents(keyword));
    }

    @PutMapping("/events/{eventId}/complete")
    public ResponseEntity<ComplianceEvent> completeEvent(@PathVariable Long eventId) {
        return ResponseEntity.ok(complianceService.completeEvent(eventId));
    }

    @PutMapping("/events/{eventId}/fail")
    public ResponseEntity<ComplianceEvent> failEvent(@PathVariable Long eventId,
                                                      @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(complianceService.failEvent(eventId, reason));
    }

    // ===== Consent Management =====

    @PostMapping("/consents")
    public ResponseEntity<ConsentRecord> grantConsent(@RequestBody ConsentRequest request) {
        return ResponseEntity.ok(complianceService.grantConsent(request));
    }

    @PutMapping("/consents/{consentId}/withdraw")
    public ResponseEntity<ConsentRecord> withdrawConsent(@PathVariable Long consentId,
                                                          @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(complianceService.withdrawConsent(consentId, reason));
    }

    @PutMapping("/consents/{consentId}/confirm")
    public ResponseEntity<ConsentRecord> confirmDoubleOptIn(@PathVariable Long consentId) {
        return ResponseEntity.ok(complianceService.confirmDoubleOptIn(consentId));
    }

    @GetMapping("/consents/user/{userId}")
    public ResponseEntity<List<ConsentRecord>> getUserConsents(@PathVariable String userId) {
        return ResponseEntity.ok(complianceService.getUserConsents(userId));
    }

    @GetMapping("/consents/expired")
    public ResponseEntity<List<ConsentRecord>> getExpiredConsents() {
        return ResponseEntity.ok(complianceService.getExpiredConsents());
    }

    @GetMapping("/consents/validate")
    public ResponseEntity<Boolean> hasValidConsent(@RequestParam String email,
                                                    @RequestParam String consentType) {
        return ResponseEntity.ok(complianceService.hasValidConsent(email, consentType));
    }

    @GetMapping("/consents/search")
    public ResponseEntity<List<ConsentRecord>> searchConsents(@RequestParam String keyword) {
        return ResponseEntity.ok(complianceService.searchConsents(keyword));
    }

    // ===== Audit Logs =====

    @GetMapping("/audit/recent")
    public ResponseEntity<List<AuditLog>> getRecentLogs() {
        return ResponseEntity.ok(complianceService.getRecentLogs());
    }

    @GetMapping("/audit/user/{userId}")
    public ResponseEntity<List<AuditLog>> getUserAuditLogs(@PathVariable String userId) {
        return ResponseEntity.ok(complianceService.getUserAuditLogs(userId));
    }

    @GetMapping("/audit/destructive")
    public ResponseEntity<List<AuditLog>> getDestructiveActions() {
        return ResponseEntity.ok(complianceService.getDestructiveActions());
    }

    @GetMapping("/audit/denied")
    public ResponseEntity<List<AuditLog>> getDeniedActions() {
        return ResponseEntity.ok(complianceService.getDeniedActions());
    }

    @GetMapping("/audit/search")
    public ResponseEntity<List<AuditLog>> searchAuditLogs(@RequestParam String keyword) {
        return ResponseEntity.ok(complianceService.searchAuditLogs(keyword));
    }

    @GetMapping("/audit/range")
    public ResponseEntity<List<AuditLog>> getLogsInDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(complianceService.getLogsInDateRange(start, end));
    }

    // ===== Data Subject Requests =====

    @GetMapping("/data-subject/{userId}")
    public ResponseEntity<DataSubjectReport> getDataSubjectReport(
            @PathVariable String userId,
            @RequestParam String email) {
        return ResponseEntity.ok(complianceService.generateDataSubjectReport(userId, email));
    }

    @PostMapping("/data-subject/{userId}/request")
    public ResponseEntity<ComplianceEvent> processDataSubjectRequest(
            @PathVariable String userId,
            @RequestParam String email,
            @RequestParam Integer requestType) {
        return ResponseEntity.ok(complianceService.processDataSubjectRequest(userId, email, requestType));
    }

    @DeleteMapping("/data-subject/{userId}/erase")
    public ResponseEntity<Map<String, Object>> processDataErasure(@PathVariable String userId) {
        return ResponseEntity.ok(complianceService.processDataErasure(userId));
    }

    // ===== Data Retention =====

    @PostMapping("/retention/enforce")
    public ResponseEntity<Map<String, Object>> enforceDataRetention(
            @RequestParam(defaultValue = "730") int retentionDays) {
        return ResponseEntity.ok(complianceService.enforceDataRetention(retentionDays));
    }

    // ===== Dashboard =====

    @GetMapping("/stats")
    public ResponseEntity<ComplianceStats> getComplianceStats() {
        return ResponseEntity.ok(complianceService.getComplianceStats());
    }
}
