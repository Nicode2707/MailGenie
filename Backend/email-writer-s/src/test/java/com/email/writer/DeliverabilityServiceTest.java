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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliverabilityServiceTest {

    @Mock private DeliverabilityRecordRepository recordRepository;
    @Mock private SenderReputationRepository reputationRepository;
    @Mock private DomainBlocklistEntryRepository blocklistRepository;

    @InjectMocks
    private DeliverabilityService deliverabilityService;

    private SenderReputation goodReputation;
    private SenderReputation badReputation;

    @BeforeEach
    void setUp() {
        goodReputation = SenderReputation.builder()
                .id(1L)
                .domain("gooddomain.com")
                .reputationScore(92.0)
                .healthGrade("A")
                .riskLevel("LOW")
                .totalSent(10000L)
                .totalDelivered(9900L)
                .hardBounces(5L)
                .softBounces(20L)
                .spamComplaints(2L)
                .unsubscribes(15L)
                .totalOpens(4500L)
                .totalClicks(800L)
                .deliverabilityRate(99.0)
                .bounceRate(0.25)
                .spamComplaintRate(0.02)
                .openRate(45.0)
                .clickRate(8.0)
                .spfPassRate(99.5)
                .dkimPassRate(99.8)
                .dmarcPassRate(99.9)
                .avgSpamScore(1.2)
                .uniqueRecipientDomains(50)
                .streakDays(30)
                .build();

        badReputation = SenderReputation.builder()
                .id(2L)
                .domain("baddomain.com")
                .reputationScore(25.0)
                .healthGrade("D")
                .riskLevel("HIGH")
                .totalSent(5000L)
                .totalDelivered(4200L)
                .hardBounces(300L)
                .softBounces(150L)
                .spamComplaints(50L)
                .unsubscribes(80L)
                .totalOpens(800L)
                .totalClicks(50L)
                .deliverabilityRate(84.0)
                .bounceRate(9.0)
                .spamComplaintRate(1.0)
                .openRate(16.0)
                .clickRate(1.0)
                .spfPassRate(70.0)
                .dkimPassRate(65.0)
                .dmarcPassRate(60.0)
                .avgSpamScore(5.5)
                .uniqueRecipientDomains(20)
                .streakDays(0)
                .build();
    }

    // ─── Record Event Tests ────────────────────────────────────────────

    @Test
    void testRecordEvent_success() {
        when(recordRepository.save(any(DeliverabilityRecord.class))).thenAnswer(inv -> {
            DeliverabilityRecord r = inv.getArgument(0);
            r.setId(1L);
            return r;
        });
        when(reputationRepository.findByDomain("gmail.com")).thenReturn(Optional.of(goodReputation));
        when(reputationRepository.save(any(SenderReputation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(recordRepository.countBySenderDomain("gmail.com")).thenReturn(10000L);
        when(recordRepository.countBySenderDomainAndEventType("gmail.com", "DELIVERED")).thenReturn(9900L);
        when(recordRepository.countBySenderDomainAndEventType("gmail.com", "BOUNCED")).thenReturn(25L);
        when(recordRepository.countBySenderDomainAndEventType("gmail.com", "SPAM_COMPLAINT")).thenReturn(2L);
        when(recordRepository.countBySenderDomainAndEventType("gmail.com", "UNSUBSCRIBED")).thenReturn(15L);
        when(recordRepository.countBySenderDomainAndEventType("gmail.com", "OPENED")).thenReturn(4500L);
        when(recordRepository.countBySenderDomainAndEventType("gmail.com", "CLICKED")).thenReturn(800L);
        when(recordRepository.spfPassCount("gmail.com")).thenReturn(9950L);
        when(recordRepository.dkimPassCount("gmail.com")).thenReturn(9980L);
        when(recordRepository.dmarcPassCount("gmail.com")).thenReturn(9990L);
        when(recordRepository.averageSpamScoreByDomain("gmail.com")).thenReturn(1.2);
        when(recordRepository.countByRecipientDomain("gmail.com")).thenReturn(List.of(new Object[]{"outlook.com", 500L}));
        when(blocklistRepository.countByDomainAndIsListedTrue("gmail.com")).thenReturn(0L);

        DeliverabilityRecordRequest request = new DeliverabilityRecordRequest();
        request.setSenderEmail("sender@gooddomain.com");
        request.setRecipientEmail("user@gmail.com");
        request.setEventType("DELIVERED");
        request.setSpfPass(true);
        request.setDkimPass(true);

        DeliverabilityRecord result = deliverabilityService.recordEvent(request);

        assertNotNull(result);
        assertEquals("DELIVERED", result.getEventType());
        assertEquals("gooddomain.com", result.getSenderDomain());
        assertEquals("gmail.com", result.getRecipientDomain());
    }

    @Test
    void testRecordEvent_throwsOnMissingSender() {
        DeliverabilityRecordRequest request = new DeliverabilityRecordRequest();
        request.setRecipientEmail("user@test.com");
        request.setEventType("SENT");

        assertThrows(IllegalArgumentException.class, () -> deliverabilityService.recordEvent(request));
    }

    @Test
    void testRecordEvent_throwsOnMissingRecipient() {
        DeliverabilityRecordRequest request = new DeliverabilityRecordRequest();
        request.setSenderEmail("sender@test.com");
        request.setEventType("SENT");

        assertThrows(IllegalArgumentException.class, () -> deliverabilityService.recordEvent(request));
    }

    @Test
    void testRecordEvent_throwsOnMissingEventType() {
        DeliverabilityRecordRequest request = new DeliverabilityRecordRequest();
        request.setSenderEmail("sender@test.com");
        request.setRecipientEmail("user@test.com");

        assertThrows(IllegalArgumentException.class, () -> deliverabilityService.recordEvent(request));
    }

    @Test
    void testRecordEventsBulk() {
        when(recordRepository.save(any(DeliverabilityRecord.class))).thenAnswer(inv -> {
            DeliverabilityRecord r = inv.getArgument(0);
            r.setId(1L);
            return r;
        });
        when(reputationRepository.findByDomain("test.com")).thenReturn(Optional.of(goodReputation));
        when(reputationRepository.save(any(SenderReputation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(recordRepository.countBySenderDomain("test.com")).thenReturn(100L);
        when(recordRepository.countBySenderDomainAndEventType(anyString(), anyString())).thenReturn(0L);
        when(recordRepository.spfPassCount("test.com")).thenReturn(0L);
        when(recordRepository.dkimPassCount("test.com")).thenReturn(0L);
        when(recordRepository.dmarcPassCount("test.com")).thenReturn(0L);
        when(recordRepository.averageSpamScoreByDomain("test.com")).thenReturn(0.0);
        when(recordRepository.countByRecipientDomain("test.com")).thenReturn(List.of());
        when(blocklistRepository.countByDomainAndIsListedTrue("test.com")).thenReturn(0L);

        DeliverabilityRecordRequest r1 = new DeliverabilityRecordRequest();
        r1.setSenderEmail("a@test.com");
        r1.setRecipientEmail("b@test.com");
        r1.setEventType("SENT");

        int recorded = deliverabilityService.recordEventsBulk(List.of(r1));

        assertEquals(1, recorded);
    }

    // ─── Reputation Tests ──────────────────────────────────────────────

    @Test
    void testGetReputation_found() {
        when(reputationRepository.findByDomain("gooddomain.com")).thenReturn(Optional.of(goodReputation));

        SenderReputation result = deliverabilityService.getReputation("gooddomain.com");

        assertEquals("A", result.getHealthGrade());
        assertEquals(92.0, result.getReputationScore());
    }

    @Test
    void testGetReputation_notFound() {
        when(reputationRepository.findByDomain("unknown.com")).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> deliverabilityService.getReputation("unknown.com"));
    }

    @Test
    void testGetAllReputations() {
        when(reputationRepository.findAllByOrderByReputationScoreDesc())
                .thenReturn(List.of(goodReputation, badReputation));

        List<SenderReputation> result = deliverabilityService.getAllReputations();

        assertEquals(2, result.size());
        assertEquals("gooddomain.com", result.get(0).getDomain());
    }

    @Test
    void testGetAtRiskDomains() {
        when(reputationRepository.findByRiskLevelOrderByReputationScoreAsc("HIGH"))
                .thenReturn(List.of(badReputation));

        List<SenderReputation> result = deliverabilityService.getAtRiskDomains();

        assertEquals(1, result.size());
        assertEquals("D", result.get(0).getHealthGrade());
    }

    @Test
    void testGetHighComplaintDomains() {
        when(reputationRepository.findAboveComplaintRate(0.1))
                .thenReturn(List.of(badReputation));

        List<SenderReputation> result = deliverabilityService.getHighComplaintDomains(0.1);

        assertEquals(1, result.size());
    }

    // ─── Blocklist Tests ───────────────────────────────────────────────

    @Test
    void testCheckBlocklist_newListing() {
        when(blocklistRepository.findByDomainAndBlocklistName("test.com", "Spamhaus"))
                .thenReturn(Optional.empty());
        when(blocklistRepository.save(any(DomainBlocklistEntry.class))).thenAnswer(inv -> inv.getArgument(0));

        DomainBlocklistEntry result = deliverabilityService.checkBlocklist("test.com", "Spamhaus", true);

        assertTrue(result.getIsListed());
        assertNotNull(result.getListedAt());
        assertFalse(result.getAlertSent());
    }

    @Test
    void testCheckBlocklist_delisted() {
        DomainBlocklistEntry existing = DomainBlocklistEntry.builder()
                .domain("test.com")
                .blocklistName("Spamhaus")
                .isListed(true)
                .listedAt(LocalDateTime.now().minusDays(5))
                .build();

        when(blocklistRepository.findByDomainAndBlocklistName("test.com", "Spamhaus"))
                .thenReturn(Optional.of(existing));
        when(blocklistRepository.save(any(DomainBlocklistEntry.class))).thenAnswer(inv -> inv.getArgument(0));

        DomainBlocklistEntry result = deliverabilityService.checkBlocklist("test.com", "Spamhaus", false);

        assertFalse(result.getIsListed());
        assertNotNull(result.getDelistedAt());
    }

    @Test
    void testGetActiveBlocklistEntries() {
        when(blocklistRepository.findByIsListedTrue()).thenReturn(List.of(
                DomainBlocklistEntry.builder().domain("test.com").blocklistName("Spamhaus").isListed(true).build()
        ));

        List<DomainBlocklistEntry> result = deliverabilityService.getActiveBlocklistEntries();

        assertEquals(1, result.size());
    }

    // ─── Entity Method Tests ───────────────────────────────────────────

    @Test
    void testSenderReputation_isHealthy() {
        assertTrue(goodReputation.isHealthy());
        assertFalse(badReputation.isHealthy());
    }

    @Test
    void testSenderReputation_isAtRisk() {
        assertTrue(badReputation.isAtRisk());
        assertFalse(goodReputation.isAtRisk());
    }

    @Test
    void testDeliverabilityRecord_builder() {
        DeliverabilityRecord record = DeliverabilityRecord.builder()
                .id(1L)
                .senderEmail("test@test.com")
                .senderDomain("test.com")
                .recipientEmail("user@gmail.com")
                .recipientDomain("gmail.com")
                .eventType("SENT")
                .spamScore(0.5)
                .spfPass(true)
                .dkimPass(true)
                .sentAt(LocalDateTime.now())
                .build();

        assertEquals("test.com", record.getSenderDomain());
        assertEquals("gmail.com", record.getRecipientDomain());
        assertTrue(record.getSpfPass());
    }

    @Test
    void testDomainBlocklistEntry_builder() {
        DomainBlocklistEntry entry = DomainBlocklistEntry.builder()
                .id(1L)
                .domain("test.com")
                .blocklistName("Spamhaus")
                .isListed(true)
                .listedAt(LocalDateTime.now())
                .build();

        assertEquals("Spamhaus", entry.getBlocklistName());
        assertTrue(entry.getIsListed());
    }

    @Test
    void testSenderReputation_defaultValues() {
        SenderReputation rep = SenderReputation.builder().domain("new.com").build();
        assertEquals(50.0, rep.getReputationScore());
        assertEquals("C", rep.getHealthGrade());
        assertEquals("LOW", rep.getRiskLevel());
        assertEquals(0L, rep.getTotalSent());
    }

    // ─── Domain Health Report Tests ────────────────────────────────────

    @Test
    void testGetDomainHealthReport_withData() {
        when(reputationRepository.findByDomain("gooddomain.com")).thenReturn(Optional.of(goodReputation));
        when(recordRepository.countByEventTypeForDomain("gooddomain.com")).thenReturn(List.of(
                new Object[]{"DELIVERED", 9900L}, new Object[]{"BOUNCED", 25L}));
        when(recordRepository.bounceBreakdownByDomain("gooddomain.com")).thenReturn(List.of(
                new Object[]{"HARD", 5L}, new Object[]{"SOFT", 20L}));
        when(recordRepository.countByRecipientDomain("gooddomain.com")).thenReturn(List.of(
                new Object[]{"gmail.com", 5000L}));
        when(recordRepository.smtpCodeDistribution("gooddomain.com")).thenReturn(List.of(
                new Object[]{250, 9900L}));
        when(blocklistRepository.findByDomain("gooddomain.com")).thenReturn(Collections.emptyList());
        when(blocklistRepository.countByDomainAndIsListedTrue("gooddomain.com")).thenReturn(0L);

        DomainHealthReport report = deliverabilityService.getDomainHealthReport("gooddomain.com");

        assertNotNull(report);
        assertEquals("gooddomain.com", report.getDomain());
        assertEquals("A", report.getHealthGrade());
        assertEquals(92.0, report.getReputationScore());
        assertFalse(report.getIsAtRisk());
        assertTrue(report.getIsHealthy());
        assertNotNull(report.getRecommendations());
        assertTrue(report.getRecommendations().size() > 0);
    }

    @Test
    void testGetDomainHealthReport_noData() {
        when(reputationRepository.findByDomain("new.com")).thenReturn(Optional.empty());
        when(recordRepository.countByEventTypeForDomain("new.com")).thenReturn(Collections.emptyList());
        when(recordRepository.bounceBreakdownByDomain("new.com")).thenReturn(Collections.emptyList());
        when(recordRepository.countByRecipientDomain("new.com")).thenReturn(Collections.emptyList());
        when(recordRepository.smtpCodeDistribution("new.com")).thenReturn(Collections.emptyList());
        when(blocklistRepository.findByDomain("new.com")).thenReturn(Collections.emptyList());
        when(blocklistRepository.countByDomainAndIsListedTrue("new.com")).thenReturn(0L);

        DomainHealthReport report = deliverabilityService.getDomainHealthReport("new.com");

        assertNotNull(report);
        assertEquals("new.com", report.getDomain());
        assertEquals(0L, report.getTotalSent());
    }

    // ─── Reputation Stats Tests ────────────────────────────────────────

    @Test
    void testGetSystemStats() {
        when(reputationRepository.findAll()).thenReturn(List.of(goodReputation, badReputation));
        when(blocklistRepository.findAll()).thenReturn(Collections.emptyList());

        ReputationStats stats = deliverabilityService.getSystemStats();

        assertEquals(2, stats.getTotalDomainsTracked());
        assertEquals(1, stats.getHealthyDomains());
        assertEquals(1, stats.getAtRiskDomains());
        assertEquals(15000L, stats.getTotalEmailsTracked());
    }

    @Test
    void testGetSystemStats_empty() {
        when(reputationRepository.findAll()).thenReturn(Collections.emptyList());
        when(blocklistRepository.findAll()).thenReturn(Collections.emptyList());

        ReputationStats stats = deliverabilityService.getSystemStats();

        assertEquals(0, stats.getTotalDomainsTracked());
        assertEquals(0, stats.getTotalEmailsTracked());
        assertEquals(100.0, stats.getSystemWideDeliverabilityRate());
    }
}
