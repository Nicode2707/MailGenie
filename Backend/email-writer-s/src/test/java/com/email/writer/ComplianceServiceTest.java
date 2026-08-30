package com.email.writer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ComplianceServiceTest {

    @Mock
    private ComplianceEventRepository complianceEventRepository;

    @Mock
    private ConsentRecordRepository consentRecordRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private ComplianceService complianceService;

    private ComplianceEvent sampleEvent;
    private ConsentRecord sampleConsent;
    private AuditLog sampleAuditLog;

    @BeforeEach
    void setUp() {
        sampleEvent = new ComplianceEvent("GDPR_ACCESS", "user1", "GDPR");
        sampleEvent.setId(1L);
        sampleEvent.setDescription("Test event");
        sampleEvent.setSeverity("INFO");
        sampleEvent.setCompletionDeadline(LocalDateTime.now().plusDays(30));

        sampleConsent = new ConsentRecord("user1", "test@example.com", "MARKETING", "WEB_FORM");
        sampleConsent.setId(1L);
        sampleConsent.setStatus("GRANTED");
        sampleConsent.setIsActive(true);

        sampleAuditLog = new AuditLog("user1", "CREATE", "TEMPLATE", "SUCCESS");
        sampleAuditLog.setId(1L);
        sampleAuditLog.setTimestamp(LocalDateTime.now());
    }

    @Test
    void testCreateComplianceEvent() {
        when(complianceEventRepository.save(any())).thenReturn(sampleEvent);
        ComplianceEventRequest request = new ComplianceEventRequest();
        request.setEventType("GDPR_ACCESS");
        request.setUserId("user1");
        request.setRegulation("GDPR");
        request.setDeadlineDays(30);

        ComplianceEvent result = complianceService.createComplianceEvent(request);
        assertNotNull(result);
        verify(complianceEventRepository).save(any());
    }

    @Test
    void testCompleteEvent() {
        when(complianceEventRepository.findById(1L)).thenReturn(Optional.of(sampleEvent));
        when(complianceEventRepository.save(any())).thenReturn(sampleEvent);
        ComplianceEvent result = complianceService.completeEvent(1L);
        assertEquals("COMPLETED", result.getStatus());
    }

    @Test
    void testFailEvent() {
        when(complianceEventRepository.findById(1L)).thenReturn(Optional.of(sampleEvent));
        when(complianceEventRepository.save(any())).thenReturn(sampleEvent);
        ComplianceEvent result = complianceService.failEvent(1L, "Test failure");
        assertEquals("FAILED", result.getStatus());
    }

    @Test
    void testGrantConsent() {
        when(consentRecordRepository.save(any())).thenReturn(sampleConsent);
        ConsentRequest request = new ConsentRequest();
        request.setUserId("user1");
        request.setEmail("test@example.com");
        request.setConsentType("MARKETING");
        request.setSource("WEB_FORM");
        ConsentRecord result = complianceService.grantConsent(request);
        assertNotNull(result);
        assertEquals("GRANTED", result.getStatus());
    }

    @Test
    void testWithdrawConsent() {
        when(consentRecordRepository.findById(1L)).thenReturn(Optional.of(sampleConsent));
        when(consentRecordRepository.save(any())).thenReturn(sampleConsent);
        ConsentRecord result = complianceService.withdrawConsent(1L, "No longer interested");
        assertEquals("WITHDRAWN", result.getStatus());
        assertFalse(result.getIsActive());
    }

    @Test
    void testConfirmDoubleOptIn() {
        sampleConsent.setStatus("PENDING");
        sampleConsent.setDoubleOptInConfirmed(false);
        when(consentRecordRepository.findById(1L)).thenReturn(Optional.of(sampleConsent));
        when(consentRecordRepository.save(any())).thenReturn(sampleConsent);
        ConsentRecord result = complianceService.confirmDoubleOptIn(1L);
        assertTrue(result.getDoubleOptInConfirmed());
    }

    @Test
    void testHasValidConsentGranted() {
        when(consentRecordRepository.findValidConsentByEmailAndType("test@example.com", "MARKETING"))
            .thenReturn(sampleConsent);
        assertTrue(complianceService.hasValidConsent("test@example.com", "MARKETING"));
    }

    @Test
    void testHasValidConsentMissing() {
        when(consentRecordRepository.findValidConsentByEmailAndType("test@example.com", "MARKETING"))
            .thenReturn(null);
        assertFalse(complianceService.hasValidConsent("test@example.com", "MARKETING"));
    }

    @Test
    void testRecordAuditEntry() {
        when(auditLogRepository.save(any())).thenReturn(sampleAuditLog);
        AuditLog result = complianceService.recordAuditEntry("user1", "CREATE", "TEMPLATE", 1L, "My Template");
        assertNotNull(result);
        verify(auditLogRepository).save(any());
    }

    @Test
    void testRecordDetailedAudit() {
        when(auditLogRepository.save(any())).thenReturn(sampleAuditLog);
        AuditLogEntry entry = new AuditLogEntry();
        entry.setUserId("user1");
        entry.setAction("DELETE");
        entry.setResourceType("TEMPLATE");
        entry.setContainsSensitiveData(true);
        AuditLog result = complianceService.recordDetailedAudit(entry);
        assertNotNull(result);
        assertTrue(result.getRequiresReview());
    }

    @Test
    void testGetComplianceStats() {
        when(complianceEventRepository.count()).thenReturn(100L);
        when(complianceEventRepository.findByStatus("PENDING")).thenReturn(List.of());
        when(complianceEventRepository.findCriticalOpenEvents()).thenReturn(List.of());
        when(complianceEventRepository.findOverdueEvents(any())).thenReturn(List.of());
        when(complianceEventRepository.countByRegulationGrouped()).thenReturn(List.of());
        when(complianceEventRepository.countByEventTypeGrouped()).thenReturn(List.of());
        when(consentRecordRepository.count()).thenReturn(50L);
        when(consentRecordRepository.findByStatus("GRANTED")).thenReturn(List.of());
        when(consentRecordRepository.findByStatus("WITHDRAWN")).thenReturn(List.of());
        when(consentRecordRepository.findExpiredActiveConsents(any())).thenReturn(List.of());
        when(consentRecordRepository.countGrantedByTypeGrouped()).thenReturn(List.of());
        when(consentRecordRepository.countByStatusGrouped()).thenReturn(List.of());
        when(auditLogRepository.count()).thenReturn(200L);
        when(auditLogRepository.countFailures()).thenReturn(5L);
        when(auditLogRepository.findDeniedActions()).thenReturn(List.of());
        when(auditLogRepository.averageDurationMs()).thenReturn(150.0);
        when(auditLogRepository.countByActionGrouped()).thenReturn(List.of());

        ComplianceStats stats = complianceService.getComplianceStats();
        assertNotNull(stats);
        assertEquals(100, stats.getTotalComplianceEvents());
        assertEquals(50, stats.getTotalConsents());
        assertEquals(200, stats.getTotalAuditLogs());
    }

    @Test
    void testProcessDataSubjectRequest() {
        when(complianceEventRepository.save(any())).thenReturn(sampleEvent);
        ComplianceEvent result = complianceService.processDataSubjectRequest("user1", "test@example.com", 3);
        assertNotNull(result);
        assertEquals("GDPR_ERASURE", result.getEventType());
    }

    @Test
    void testProcessDataErasure() {
        when(consentRecordRepository.findByUserId("user1")).thenReturn(List.of(sampleConsent));
        when(consentRecordRepository.save(any())).thenReturn(sampleConsent);
        when(auditLogRepository.save(any())).thenReturn(sampleAuditLog);
        Map<String, Object> result = complianceService.processDataErasure("user1");
        assertNotNull(result);
        assertEquals(1, result.get("consentsWithdrawn"));
    }

    @Test
    void testGetEventsByUser() {
        when(complianceEventRepository.findByUserId("user1")).thenReturn(List.of(sampleEvent));
        List<ComplianceEvent> result = complianceService.getEventsByUser("user1");
        assertEquals(1, result.size());
    }

    @Test
    void testGetPendingEvents() {
        when(complianceEventRepository.findByStatus("PENDING")).thenReturn(List.of(sampleEvent));
        List<ComplianceEvent> result = complianceService.getPendingEvents();
        assertFalse(result.isEmpty());
    }

    @Test
    void testGetCriticalEvents() {
        when(complianceEventRepository.findCriticalOpenEvents()).thenReturn(List.of(sampleEvent));
        List<ComplianceEvent> result = complianceService.getCriticalEvents();
        assertFalse(result.isEmpty());
    }

    @Test
    void testGetOverdueEvents() {
        when(complianceEventRepository.findOverdueEvents(any())).thenReturn(List.of(sampleEvent));
        List<ComplianceEvent> result = complianceService.getOverdueEvents();
        assertFalse(result.isEmpty());
    }

    @Test
    void testSearchEvents() {
        when(complianceEventRepository.searchEvents("GDPR")).thenReturn(List.of(sampleEvent));
        List<ComplianceEvent> result = complianceService.searchEvents("GDPR");
        assertEquals(1, result.size());
    }

    @Test
    void testGetRecentLogs() {
        when(auditLogRepository.findTop50ByOrderByTimestampDesc()).thenReturn(List.of(sampleAuditLog));
        List<AuditLog> result = complianceService.getRecentLogs();
        assertFalse(result.isEmpty());
    }

    @Test
    void testGetDestructiveActions() {
        when(auditLogRepository.findDestructiveActions()).thenReturn(List.of(sampleAuditLog));
        List<AuditLog> result = complianceService.getDestructiveActions();
        assertFalse(result.isEmpty());
    }

    @Test
    void testGetDeniedActions() {
        when(auditLogRepository.findDeniedActions()).thenReturn(List.of(sampleAuditLog));
        List<AuditLog> result = complianceService.getDeniedActions();
        assertFalse(result.isEmpty());
    }

    @Test
    void testSearchAuditLogs() {
        when(auditLogRepository.searchLogs("delete")).thenReturn(List.of(sampleAuditLog));
        List<AuditLog> result = complianceService.searchAuditLogs("delete");
        assertEquals(1, result.size());
    }

    @Test
    void testGetLogsInDateRange() {
        LocalDateTime start = LocalDateTime.now().minusDays(7);
        LocalDateTime end = LocalDateTime.now();
        when(auditLogRepository.findByTimestampBetween(start, end)).thenReturn(List.of(sampleAuditLog));
        List<AuditLog> result = complianceService.getLogsInDateRange(start, end);
        assertFalse(result.isEmpty());
    }

    @Test
    void testGenerateDataSubjectReport() {
        when(consentRecordRepository.findByUserId("user1")).thenReturn(List.of(sampleConsent));
        when(complianceEventRepository.findByUserId("user1")).thenReturn(List.of(sampleEvent));
        when(auditLogRepository.findByUserId("user1")).thenReturn(List.of(sampleAuditLog));
        DataSubjectReport report = complianceService.generateDataSubjectReport("user1", "test@example.com");
        assertNotNull(report);
        assertEquals("user1", report.getUserId());
        assertNotNull(report.getDataCategories());
        assertFalse(report.getDataCategories().isEmpty());
    }

    @Test
    void testComplianceEventEntityMethods() {
        ComplianceEvent event = new ComplianceEvent();
        event.setStatus("COMPLETED");
        assertTrue(event.isCompleted());
        event.setStatus("PENDING");
        assertTrue(event.isPending());
        event.setStatus("FAILED");
        assertTrue(event.isFailed());
        event.setSeverity("CRITICAL");
        assertTrue(event.isCritical());
        event.setSeverity("EMERGENCY");
        assertTrue(event.isCritical());
        event.setCompletionDeadline(LocalDateTime.now().minusDays(1));
        assertTrue(event.isOverdue());
    }

    @Test
    void testConsentRecordEntityMethods() {
        ConsentRecord consent = new ConsentRecord();
        consent.setStatus("GRANTED");
        assertTrue(consent.isGranted());
        consent.setStatus("WITHDRAWN");
        assertTrue(consent.isWithdrawn());
        consent.setExpiryDate(LocalDateTime.now().minusDays(1));
        assertTrue(consent.isExpired());
        consent.setStatus("GRANTED");
        consent.setExpiryDate(LocalDateTime.now().plusDays(30));
        assertTrue(consent.isValid());
        consent.setDoubleOptInConfirmed(false);
        assertTrue(consent.requiresDoubleOptIn());
    }

    @Test
    void testAuditLogEntityMethods() {
        AuditLog log = new AuditLog();
        log.setAction("DELETE");
        assertTrue(log.isDestructive());
        log.setAction("CREATE");
        assertTrue(log.isWriteOperation());
        log.setAction("READ");
        assertFalse(log.isWriteOperation());
        log.setOutcome("SUCCESS");
        assertTrue(log.isSuccess());
        log.setOutcome("FAILURE");
        assertTrue(log.isFailure());
        log.setOutcome("DENIED");
        assertTrue(log.isDenied());
        log.setRequiresReview(true);
        assertTrue(log.needsReview());
        log.setContainsSensitiveData(true);
        assertTrue(log.hasSensitiveData());
    }
}
