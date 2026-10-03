package com.email.writer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class ComplianceService {

    @Autowired
    private ComplianceEventRepository complianceEventRepository;

    @Autowired
    private ConsentRecordRepository consentRecordRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    // ===== Compliance Events =====

    public ComplianceEvent createComplianceEvent(ComplianceEventRequest request) {
        ComplianceEvent event = new ComplianceEvent(
            request.getEventType(),
            request.getUserId(),
            request.getRegulation() != null ? request.getRegulation() : "GDPR"
        );
        event.setTargetUserId(request.getTargetUserId());
        event.setTargetEmail(request.getTargetEmail());
        event.setDescription(request.getDescription());
        event.setSeverity(request.getSeverity() != null ? request.getSeverity() : "INFO");
        event.setLawfulBasis(request.getLawfulBasis());
        event.setMetadataJson(request.getMetadataJson());
        event.setDataSubjectRequestType(request.getDataSubjectRequestType());

        if (request.getDeadlineDays() != null && request.getDeadlineDays() > 0) {
            event.setCompletionDeadline(LocalDateTime.now().plusDays(request.getDeadlineDays()));
        } else {
            // GDPR default: 30 days for data subject requests
            event.setCompletionDeadline(LocalDateTime.now().plusDays(30));
        }

        return complianceEventRepository.save(event);
    }

    public List<ComplianceEvent> getEventsByUser(String userId) {
        return complianceEventRepository.findByUserId(userId);
    }

    public List<ComplianceEvent> getEventsByType(String eventType) {
        return complianceEventRepository.findByEventType(eventType);
    }

    public List<ComplianceEvent> getPendingEvents() {
        return complianceEventRepository.findByStatus("PENDING");
    }

    public List<ComplianceEvent> getCriticalEvents() {
        return complianceEventRepository.findCriticalOpenEvents();
    }

    public List<ComplianceEvent> getOverdueEvents() {
        return complianceEventRepository.findOverdueEvents(LocalDateTime.now());
    }

    public List<ComplianceEvent> searchEvents(String keyword) {
        return complianceEventRepository.searchEvents(keyword);
    }

    public ComplianceEvent completeEvent(Long eventId) {
        ComplianceEvent event = complianceEventRepository.findById(eventId)
            .orElseThrow(() -> new RuntimeException("Compliance event not found: " + eventId));
        event.setStatus("COMPLETED");
        event.setCompletedAt(LocalDateTime.now());
        return complianceEventRepository.save(event);
    }

    public ComplianceEvent failEvent(Long eventId, String reason) {
        ComplianceEvent event = complianceEventRepository.findById(eventId)
            .orElseThrow(() -> new RuntimeException("Compliance event not found: " + eventId));
        event.setStatus("FAILED");
        event.setCompletedAt(LocalDateTime.now());
        if (reason != null) {
            event.setMetadataJson("{\"failureReason\":\"" + reason + "\"}");
        }
        return complianceEventRepository.save(event);
    }

    public long countEventsByUserAndType(String userId, String eventType) {
        return complianceEventRepository.countByUserIdAndEventType(userId, eventType);
    }

    // ===== Consent Management =====

    public ConsentRecord grantConsent(ConsentRequest request) {
        ConsentRecord record = new ConsentRecord(
            request.getUserId(),
            request.getEmail(),
            request.getConsentType(),
            request.getSource() != null ? request.getSource() : "API"
        );
        record.setPurpose(request.getPurpose());
        record.setEvidenceUrl(request.getEvidenceUrl());
        record.setIpAddress(request.getIpAddress());
        record.setUserAgent(request.getUserAgent());
        record.setStatus("GRANTED");
        record.setDoubleOptInConfirmed(false);

        if (request.getExpiryDays() != null && request.getExpiryDays() > 0) {
            record.setExpiryDate(LocalDateTime.now().plusDays(request.getExpiryDays()));
        }

        ConsentRecord saved = consentRecordRepository.save(record);
        recordAuditEntry(request.getUserId(), "CREATE", "CONSENT",
            saved.getId(), "Consent granted: " + request.getConsentType());
        return saved;
    }

    public ConsentRecord withdrawConsent(Long consentId, String reason) {
        ConsentRecord record = consentRecordRepository.findById(consentId)
            .orElseThrow(() -> new RuntimeException("Consent record not found: " + consentId));
        record.setStatus("WITHDRAWN");
        record.setIsActive(false);
        record.setWithdrawalDate(LocalDateTime.now());
        record.setWithdrawalReason(reason);
        record.setVersion(record.getVersion() + 1);

        ConsentRecord saved = consentRecordRepository.save(record);
        recordAuditEntry(record.getUserId(), "UPDATE", "CONSENT",
            consentId, "Consent withdrawn: " + record.getConsentType() + " Reason: " + reason);
        return saved;
    }

    public ConsentRecord confirmDoubleOptIn(Long consentId) {
        ConsentRecord record = consentRecordRepository.findById(consentId)
            .orElseThrow(() -> new RuntimeException("Consent record not found: " + consentId));
        record.setDoubleOptInConfirmed(true);
        record.setDoubleOptInDate(LocalDateTime.now());
        if ("PENDING".equals(record.getStatus())) {
            record.setStatus("GRANTED");
        }
        return consentRecordRepository.save(record);
    }

    public List<ConsentRecord> getUserConsents(String userId) {
        return consentRecordRepository.findByUserId(userId);
    }

    public List<ConsentRecord> getExpiredConsents() {
        return consentRecordRepository.findExpiredActiveConsents(LocalDateTime.now());
    }

    public boolean hasValidConsent(String email, String consentType) {
        ConsentRecord record = consentRecordRepository.findValidConsentByEmailAndType(email, consentType);
        return record != null && record.isValid();
    }

    public List<ConsentRecord> searchConsents(String keyword) {
        return consentRecordRepository.searchConsents(keyword);
    }

    public long countGrantedConsents(String userId) {
        return consentRecordRepository.countGrantedByUserId(userId);
    }

    // ===== Audit Logging =====

    public AuditLog recordAuditEntry(String userId, String action, String resourceType,
                                      Long resourceId, String resourceName) {
        AuditLog log = new AuditLog(userId, action, resourceType, "SUCCESS");
        log.setResourceId(resourceId);
        log.setResourceName(resourceName);
        log.setContainsSensitiveData(isSensitiveResource(resourceType));
        log.setRequiresReview(isDestructiveAction(action) || isSensitiveResource(resourceType));
        return auditLogRepository.save(log);
    }

    public AuditLog recordDetailedAudit(AuditLogEntry entry) {
        AuditLog log = new AuditLog(
            entry.getUserId(),
            entry.getAction(),
            entry.getResourceType(),
            entry.getOutcome() != null ? entry.getOutcome() : "SUCCESS"
        );
        log.setResourceId(entry.getResourceId());
        log.setResourceName(entry.getResourceName());
        log.setChangesJson(entry.getChangesJson());
        log.setFailureReason(entry.getFailureReason());
        log.setDurationMs(entry.getDurationMs());
        log.setRequestMetadata(entry.getRequestMetadata());
        log.setContainsSensitiveData(entry.getContainsSensitiveData());
        log.setIpAddress(entry.getIpAddress());
        log.setUserAgent(entry.getUserAgent());
        log.setRequiresReview(isDestructiveAction(entry.getAction()) || Boolean.TRUE.equals(entry.getContainsSensitiveData()));
        return auditLogRepository.save(log);
    }

    public List<AuditLog> getUserAuditLogs(String userId) {
        return auditLogRepository.findByUserId(userId);
    }

    public List<AuditLog> getRecentLogs() {
        return auditLogRepository.findTop50ByOrderByTimestampDesc();
    }

    public List<AuditLog> getDestructiveActions() {
        return auditLogRepository.findDestructiveActions();
    }

    public List<AuditLog> getDeniedActions() {
        return auditLogRepository.findDeniedActions();
    }

    public List<AuditLog> searchAuditLogs(String keyword) {
        return auditLogRepository.searchLogs(keyword);
    }

    public List<AuditLog> getLogsInDateRange(LocalDateTime start, LocalDateTime end) {
        return auditLogRepository.findByTimestampBetween(start, end);
    }

    // ===== Data Subject Requests (GDPR Articles 15-20) =====

    public DataSubjectReport generateDataSubjectReport(String userId, String email) {
        DataSubjectReport report = new DataSubjectReport();
        report.setUserId(userId);
        report.setEmail(email);
        report.setConsentRecords(consentRecordRepository.findByUserId(userId));
        report.setComplianceEvents(complianceEventRepository.findByUserId(userId));
        report.setAuditLogs(auditLogRepository.findByUserId(userId));
        report.setDataCategories(Arrays.asList(
            "Personal identifiers", "Email content", "Usage analytics",
            "Consent records", "Audit trail", "Device information", "IP addresses"
        ));
        report.setThirdPartiesWithAccess(Arrays.asList(
            "Email service providers", "Analytics platforms", "CRM integrations"
        ));
        report.setTotalDataSizeBytes(estimateDataSize(userId));
        return report;
    }

    public ComplianceEvent processDataSubjectRequest(String userId, String email, Integer requestType) {
        ComplianceEventRequest req = new ComplianceEventRequest();
        String[] typeNames = {"", "ACCESS", "RECTIFICATION", "ERASURE", "RESTRICTION", "PORTABILITY", "OBJECTION"};
        req.setEventType("GDPR_" + typeNames[requestType]);
        req.setUserId("SYSTEM");
        req.setTargetUserId(userId);
        req.setTargetEmail(email);
        req.setDescription("Data Subject Request type " + requestType + " for user " + userId);
        req.setSeverity("WARNING");
        req.setRegulation("GDPR");
        req.setLawfulBasis("legal_obligation");
        req.setDataSubjectRequestType(requestType);
        req.setDeadlineDays(30); // GDPR requires 30 days
        return createComplianceEvent(req);
    }

    public Map<String, Object> processDataErasure(String userId) {
        Map<String, Object> result = new HashMap<>();
        List<ConsentRecord> consents = consentRecordRepository.findByUserId(userId);
        int consentCount = 0;
        for (ConsentRecord consent : consents) {
            consent.setStatus("WITHDRAWN");
            consent.setIsActive(false);
            consent.setWithdrawalDate(LocalDateTime.now());
            consent.setWithdrawalReason("GDPR erasure request");
            consentRecordRepository.save(consent);
            consentCount++;
        }
        result.put("consentsWithdrawn", consentCount);
        result.put("userId", userId);
        result.put("erasedAt", LocalDateTime.now());

        recordAuditEntry("SYSTEM", "DELETE", "USER_DATA", null,
            "GDPR data erasure processed for user: " + userId);
        return result;
    }

    // ===== Data Retention =====

    public Map<String, Object> enforceDataRetention(int retentionDays) {
        Map<String, Object> result = new HashMap<>();
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);

        List<ComplianceEvent> completedEvents = complianceEventRepository.findByStatus("COMPLETED");
        long eventsPurged = 0;
        for (ComplianceEvent event : completedEvents) {
            if (event.getCompletedAt() != null && event.getCompletedAt().isBefore(cutoff)) {
                complianceEventRepository.delete(event);
                eventsPurged++;
            }
        }

        List<AuditLog> oldLogs = auditLogRepository.findByTimestampBetween(LocalDateTime.MIN, cutoff);
        long logsPurged = oldLogs.size();
        auditLogRepository.deleteAll(oldLogs);

        result.put("eventsPurged", eventsPurged);
        result.put("logsPurged", logsPurged);
        result.put("retentionCutoff", cutoff);
        result.put("executedAt", LocalDateTime.now());
        return result;
    }

    // ===== Compliance Dashboard =====

    public ComplianceStats getComplianceStats() {
        ComplianceStats stats = new ComplianceStats();
        stats.setTotalComplianceEvents(complianceEventRepository.count());
        stats.setPendingEvents(complianceEventRepository.findByStatus("PENDING").size());
        stats.setCriticalEvents(complianceEventRepository.findCriticalOpenEvents().size());
        stats.setOverdueEvents(complianceEventRepository.findOverdueEvents(LocalDateTime.now()).size());
        stats.setTotalConsents(consentRecordRepository.count());
        stats.setGrantedConsents(consentRecordRepository.findByStatus("GRANTED").size());
        stats.setWithdrawnConsents(consentRecordRepository.findByStatus("WITHDRAWN").size());
        stats.setExpiredConsents(consentRecordRepository.findExpiredActiveConsents(LocalDateTime.now()).size());
        stats.setTotalAuditLogs(auditLogRepository.count());
        stats.setFailedAuditLogs(auditLogRepository.countFailures());
        stats.setDeniedAccessAttempts((long) auditLogRepository.findDeniedActions().size());
        stats.setAverageAuditDurationMs(auditLogRepository.averageDurationMs());

        // Compute compliance score (0-100)
        stats.setComplianceScore(calculateComplianceScore(stats));

        // Breakdowns
        stats.setEventsByRegulation(toLongMap(complianceEventRepository.countByRegulationGrouped()));
        stats.setEventsByType(toLongMap(complianceEventRepository.countByEventTypeGrouped()));
        stats.setConsentsByType(toLongMap(consentRecordRepository.countGrantedByTypeGrouped()));
        stats.setConsentsByStatus(toLongMap(consentRecordRepository.countByStatusGrouped()));
        stats.setAuditActionsBreakdown(toLongMap(auditLogRepository.countByActionGrouped()));

        return stats;
    }

    // ===== Helper Methods =====

    private boolean isSensitiveResource(String resourceType) {
        return "USER".equals(resourceType) || "CONSENT".equals(resourceType)
            || "SETTINGS".equals(resourceType);
    }

    private boolean isDestructiveAction(String action) {
        return "DELETE".equals(action) || "EXPORT".equals(action);
    }

    private long estimateDataSize(String userId) {
        long size = 0;
        size += consentRecordRepository.findByUserId(userId).size() * 512L;
        size += complianceEventRepository.findByUserId(userId).size() * 1024L;
        size += auditLogRepository.findByUserId(userId).size() * 256L;
        return size;
    }

    private double calculateComplianceScore(ComplianceStats stats) {
        double score = 100.0;
        if (stats.getTotalComplianceEvents() > 0) {
            double overdueRate = (double) stats.getOverdueEvents() / stats.getTotalComplianceEvents();
            score -= overdueRate * 30;
        }
        if (stats.getTotalConsents() > 0) {
            double withdrawnRate = (double) stats.getWithdrawnConsents() / stats.getTotalConsents();
            score -= withdrawnRate * 10;
        }
        if (stats.getTotalAuditLogs() > 0) {
            double failureRate = (double) stats.getFailedAuditLogs() / stats.getTotalAuditLogs();
            score -= failureRate * 20;
        }
        if (stats.getDeniedAccessAttempts() > 0) {
            score -= Math.min(stats.getDeniedAccessAttempts() * 2, 20);
        }
        score -= stats.getCriticalEvents() * 5;
        return Math.max(0, Math.min(100, score));
    }

    private Map<String, Long> toLongMap(List<Object[]> grouped) {
        Map<String, Long> map = new LinkedHashMap<>();
        for (Object[] row : grouped) {
            map.put(String.valueOf(row[0]), ((Number) row[1]).longValue());
        }
        return map;
    }
}
