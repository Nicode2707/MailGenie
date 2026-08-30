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
public class AnalyticsServiceTest {

    @Mock
    private EmailEventRepository emailEventRepository;

    @Mock
    private EngagementScoreRepository engagementScoreRepository;

    @Mock
    private AudienceSegmentRepository audienceSegmentRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    private EmailEvent sampleEvent;
    private EngagementScore sampleScore;
    private AudienceSegment sampleSegment;

    @BeforeEach
    void setUp() {
        sampleEvent = new EmailEvent("OPENED", "user1", "recipient@test.com");
        sampleEvent.setId(1L);
        sampleEvent.setDeviceType("DESKTOP");
        sampleEvent.setEmailClient("GMAIL");
        sampleEvent.setGeoCountry("US");
        sampleEvent.setEventTimestamp(LocalDateTime.now());

        sampleScore = new EngagementScore("user1", "recipient@test.com");
        sampleScore.setId(1L);
        sampleScore.setTotalSent(100);
        sampleScore.setTotalDelivered(95);
        sampleScore.setTotalOpened(50);
        sampleScore.setTotalClicked(20);
        sampleScore.setTotalBounced(3);
        sampleScore.setTotalComplaints(1);
        sampleScore.setTotalUnsubscribed(1);
        sampleScore.setOpenRate(50.0);
        sampleScore.setClickRate(20.0);
        sampleScore.setBounceRate(3.0);
        sampleScore.setComplaintRate(1.0);
        sampleScore.setUnsubscribeRate(1.0);
        sampleScore.computeEngagementScore();

        sampleSegment = new AudienceSegment("user1", "Hot Leads", "ENGAGEMENT_BASED");
        sampleSegment.setId(1L);
        sampleSegment.setMemberCount(50);
        sampleSegment.setIsActive(true);
    }

    @Test
    void testTrackEvent() {
        when(emailEventRepository.save(any())).thenReturn(sampleEvent);
        when(emailEventRepository.findByUserIdAndEventType(anyString(), anyString())).thenReturn(List.of());
        when(engagementScoreRepository.findByEmail(anyString())).thenReturn(null);
        when(engagementScoreRepository.save(any())).thenReturn(sampleScore);
        TrackEventRequest request = new TrackEventRequest();
        request.setEventType("OPENED");
        request.setUserId("user1");
        request.setRecipientEmail("recipient@test.com");
        EmailEvent result = analyticsService.trackEvent(request);
        assertNotNull(result);
        verify(emailEventRepository).save(any());
        verify(engagementScoreRepository).save(any());
    }

    @Test
    void testTrackEventUpdatesExistingScore() {
        when(emailEventRepository.save(any())).thenReturn(sampleEvent);
        when(emailEventRepository.findByUserIdAndEventType(anyString(), anyString())).thenReturn(List.of());
        when(engagementScoreRepository.findByEmail(anyString())).thenReturn(sampleScore);
        when(engagementScoreRepository.save(any())).thenReturn(sampleScore);
        TrackEventRequest request = new TrackEventRequest();
        request.setEventType("CLICKED");
        request.setUserId("user1");
        request.setRecipientEmail("recipient@test.com");
        EmailEvent result = analyticsService.trackEvent(request);
        assertNotNull(result);
        verify(engagementScoreRepository).save(sampleScore);
    }

    @Test
    void testBatchTrackEvents() {
        when(emailEventRepository.save(any())).thenReturn(sampleEvent);
        when(emailEventRepository.findByUserIdAndEventType(anyString(), anyString())).thenReturn(List.of());
        when(engagementScoreRepository.findByEmail(anyString())).thenReturn(null);
        when(engagementScoreRepository.save(any())).thenReturn(sampleScore);
        TrackEventRequest req1 = new TrackEventRequest();
        req1.setEventType("OPENED");
        req1.setUserId("user1");
        req1.setRecipientEmail("a@test.com");
        TrackEventRequest req2 = new TrackEventRequest();
        req2.setEventType("CLICKED");
        req2.setUserId("user1");
        req2.setRecipientEmail("b@test.com");
        List<EmailEvent> results = analyticsService.batchTrackEvents(List.of(req1, req2));
        assertEquals(2, results.size());
    }

    @Test
    void testGetEngagementScore() {
        when(engagementScoreRepository.findByEmail("recipient@test.com")).thenReturn(sampleScore);
        EngagementScore result = analyticsService.getEngagementScore("recipient@test.com");
        assertEquals("recipient@test.com", result.getEmail());
    }

    @Test
    void testGetEngagementScoreNotFound() {
        when(engagementScoreRepository.findByEmail("unknown@test.com")).thenReturn(null);
        EngagementScore result = analyticsService.getEngagementScore("unknown@test.com");
        assertNotNull(result);
        assertEquals("unknown@test.com", result.getEmail());
    }

    @Test
    void testGetTopEngaged() {
        when(engagementScoreRepository.findTopEngaged("user1")).thenReturn(List.of(sampleScore));
        List<EngagementScore> result = analyticsService.getTopEngaged("user1");
        assertEquals(1, result.size());
    }

    @Test
    void testGetRecipientsByTier() {
        when(engagementScoreRepository.findByUserIdAndEngagementTier("user1", "HOT")).thenReturn(List.of(sampleScore));
        List<EngagementScore> result = analyticsService.getRecipientsByTier("user1", "HOT");
        assertFalse(result.isEmpty());
    }

    @Test
    void testGetAtRiskRecipients() {
        when(engagementScoreRepository.findByIsAtRisk(true)).thenReturn(List.of(sampleScore));
        List<EngagementScore> result = analyticsService.getAtRiskRecipients(null);
        assertFalse(result.isEmpty());
    }

    @Test
    void testGetVipRecipients() {
        when(engagementScoreRepository.findByIsVip(true)).thenReturn(List.of(sampleScore));
        List<EngagementScore> result = analyticsService.getVipRecipients(null);
        assertFalse(result.isEmpty());
    }

    @Test
    void testGetInactiveRecipients() {
        when(engagementScoreRepository.findInactiveForDays("user1", 30)).thenReturn(List.of(sampleScore));
        List<EngagementScore> result = analyticsService.getInactiveRecipients("user1", 30);
        assertFalse(result.isEmpty());
    }

    @Test
    void testGetTierBreakdown() {
        when(engagementScoreRepository.countByTierForUser("user1"))
            .thenReturn(List.of(new Object[]{"HOT", 10L}, new Object[]{"WARM", 20L}));
        Map<String, Long> result = analyticsService.getTierBreakdown("user1");
        assertEquals(10L, result.get("HOT"));
        assertEquals(20L, result.get("WARM"));
    }

    @Test
    void testRecalculateScore() {
        when(engagementScoreRepository.findByEmail("recipient@test.com")).thenReturn(sampleScore);
        when(emailEventRepository.findByUserIdAndEventType(anyString(), anyString())).thenReturn(List.of());
        when(engagementScoreRepository.save(any())).thenReturn(sampleScore);
        EngagementScore result = analyticsService.recalculateScore("recipient@test.com");
        assertNotNull(result);
    }

    @Test
    void testCreateSegment() {
        when(audienceSegmentRepository.save(any())).thenReturn(sampleSegment);
        SegmentRequest request = new SegmentRequest();
        request.setUserId("user1");
        request.setName("Hot Leads");
        request.setSegmentType("ENGAGEMENT_BASED");
        AudienceSegment result = analyticsService.createSegment(request);
        assertNotNull(result);
        assertEquals("Hot Leads", result.getName());
    }

    @Test
    void testGetUserSegments() {
        when(audienceSegmentRepository.findByUserId("user1")).thenReturn(List.of(sampleSegment));
        List<AudienceSegment> result = analyticsService.getUserSegments("user1");
        assertEquals(1, result.size());
    }

    @Test
    void testGetActiveSegments() {
        when(audienceSegmentRepository.findByUserIdAndIsActive("user1", true)).thenReturn(List.of(sampleSegment));
        List<AudienceSegment> result = analyticsService.getActiveSegments("user1");
        assertFalse(result.isEmpty());
    }

    @Test
    void testGetNonEmptySegments() {
        when(audienceSegmentRepository.findNonEmptyByUser("user1")).thenReturn(List.of(sampleSegment));
        List<AudienceSegment> result = analyticsService.getNonEmptySegments("user1");
        assertFalse(result.isEmpty());
    }

    @Test
    void testGetSegmentById() {
        when(audienceSegmentRepository.findById(1L)).thenReturn(Optional.of(sampleSegment));
        AudienceSegment result = analyticsService.getSegmentById(1L);
        assertEquals("Hot Leads", result.getName());
    }

    @Test
    void testUpdateSegmentMemberCount() {
        when(audienceSegmentRepository.findById(1L)).thenReturn(Optional.of(sampleSegment));
        when(engagementScoreRepository.findByUserId("user1")).thenReturn(List.of(sampleScore));
        when(audienceSegmentRepository.save(any())).thenReturn(sampleSegment);
        AudienceSegment result = analyticsService.updateSegmentMemberCount(1L);
        assertNotNull(result);
        verify(audienceSegmentRepository).save(any());
    }

    @Test
    void testDeleteSegment() {
        doNothing().when(audienceSegmentRepository).deleteById(1L);
        analyticsService.deleteSegment(1L);
        verify(audienceSegmentRepository).deleteById(1L);
    }

    @Test
    void testGetDashboard() {
        when(emailEventRepository.countSent("user1")).thenReturn(100L);
        when(emailEventRepository.countDelivered("user1")).thenReturn(95L);
        when(emailEventRepository.countOpened("user1")).thenReturn(50L);
        when(emailEventRepository.countClicked("user1")).thenReturn(20L);
        when(emailEventRepository.countBounced("user1")).thenReturn(3L);
        when(emailEventRepository.countComplained("user1")).thenReturn(1L);
        when(emailEventRepository.countUnsubscribed("user1")).thenReturn(1L);
        when(emailEventRepository.avgTimeToOpen("user1")).thenReturn(300.0);
        when(emailEventRepository.avgTimeToClick("user1")).thenReturn(600.0);
        when(emailEventRepository.countOpensByDevice("user1")).thenReturn(List.of());
        when(emailEventRepository.countOpensByClient("user1")).thenReturn(List.of());
        when(emailEventRepository.countEngagementByCountry("user1")).thenReturn(List.of());
        when(emailEventRepository.countByEventTypeForUser("user1")).thenReturn(List.of());
        when(emailEventRepository.dailyEventTrend(eq("user1"), any())).thenReturn(List.of());
        when(engagementScoreRepository.countByTierForUser("user1")).thenReturn(List.of());
        when(engagementScoreRepository.findByIsAtRisk(true)).thenReturn(List.of());
        when(engagementScoreRepository.findByIsVip(true)).thenReturn(List.of());
        when(engagementScoreRepository.avgScoreForUser("user1")).thenReturn(65.0);
        when(engagementScoreRepository.findByUserId("user1")).thenReturn(List.of(sampleScore));
        when(audienceSegmentRepository.findByUserId("user1")).thenReturn(List.of(sampleSegment));

        AnalyticsDashboard result = analyticsService.getDashboard("user1");
        assertNotNull(result);
        assertEquals(100, result.getTotalSent());
        assertEquals(50, result.getTotalOpened());
        assertEquals(50.0, result.getOpenRate());
        assertEquals(20.0, result.getClickRate());
        assertEquals(65.0, result.getAvgEngagementScore());
        assertEquals(1, result.getTotalSegments());
    }

    @Test
    void testEmailEventEntityMethods() {
        EmailEvent event = new EmailEvent();
        event.setEventType("OPENED");
        assertTrue(event.isEngagementEvent());
        event.setEventType("CLICKED");
        assertTrue(event.isEngagementEvent());
        event.setEventType("BOUNCED");
        assertTrue(event.isNegativeEvent());
        event.setEventType("COMPLAINED");
        assertTrue(event.isNegativeEvent());
        event.setEventType("SENT");
        assertTrue(event.isDeliveryEvent());
        event.setTimeToOpenSeconds(300L);
        assertTrue(event.hasTimingData());
    }

    @Test
    void testEngagementScoreComputeScore() {
        EngagementScore score = new EngagementScore();
        score.setOpenRate(80.0);
        score.setClickRate(40.0);
        score.setReplyRate(30.0);
        score.setBounceRate(2.0);
        score.setComplaintRate(0.5);
        score.setUnsubscribeRate(0.5);
        score.computeEngagementScore();
        assertTrue(score.getEngagementScore() > 0);
        assertEquals("HOT", score.getEngagementTier());
        assertTrue(score.isHot());
        assertFalse(score.isCold());
    }

    @Test
    void testEngagementScoreTiers() {
        EngagementScore score = new EngagementScore();
        score.setEngagementScore(60.0);
        score.computeTier();
        assertEquals("WARM", score.getEngagementTier());
        assertTrue(score.isWarm());

        score.setEngagementScore(30.0);
        score.computeTier();
        assertEquals("COLD", score.getEngagementTier());
        assertTrue(score.isAtRisk());

        score.setEngagementScore(10.0);
        score.computeTier();
        assertEquals("INACTIVE", score.getEngagementTier());
        assertTrue(score.isInactive());

        score.setEngagementScore(0.0);
        score.computeTier();
        assertEquals("UNENGAGED", score.getEngagementTier());
    }

    @Test
    void testAudienceSegmentEntityMethods() {
        AudienceSegment segment = new AudienceSegment();
        segment.setSegmentType("DYNAMIC");
        assertTrue(segment.isDynamic());
        segment.setSegmentType("STATIC");
        assertTrue(segment.isStatic());
        segment.setSegmentType("BEHAVIORAL");
        assertTrue(segment.isBehavioral());
        segment.setSegmentType("ENGAGEMENT_BASED");
        assertTrue(segment.isEngagementBased());
        segment.setMemberCount(0);
        assertTrue(segment.isEmpty());
        segment.setMemberCount(10);
        assertFalse(segment.isEmpty());
        segment.setAvgEngagementScore(80.0);
        assertTrue(segment.hasHighEngagement());
        segment.setLastRefreshedAt(LocalDateTime.now().minusDays(2));
        assertTrue(segment.needsRefresh());
    }

    @Test
    void testGetEventsByType() {
        when(emailEventRepository.findByUserIdAndEventType("user1", "OPENED")).thenReturn(List.of(sampleEvent));
        List<EmailEvent> result = analyticsService.getEventsByType("user1", "OPENED");
        assertEquals(1, result.size());
    }

    @Test
    void testGetCampaignEvents() {
        when(emailEventRepository.findByCampaignId(1L)).thenReturn(List.of(sampleEvent));
        List<EmailEvent> result = analyticsService.getCampaignEvents(1L);
        assertFalse(result.isEmpty());
    }

    @Test
    void testGetRecipientEvents() {
        when(emailEventRepository.findByRecipientEmail("recipient@test.com")).thenReturn(List.of(sampleEvent));
        List<EmailEvent> result = analyticsService.getRecipientEvents("recipient@test.com");
        assertEquals(1, result.size());
    }

    @Test
    void testGetEventsInDateRange() {
        LocalDateTime start = LocalDateTime.now().minusDays(7);
        LocalDateTime end = LocalDateTime.now();
        when(emailEventRepository.findByTimestampBetween(start, end)).thenReturn(List.of(sampleEvent));
        List<EmailEvent> result = analyticsService.getEventsInDateRange(start, end);
        assertFalse(result.isEmpty());
    }

    @Test
    void testEngagementScoreVipComputation() {
        EngagementScore score = new EngagementScore();
        score.setEngagementScore(85.0);
        score.computeTier();
        assertTrue(score.isVip());
        assertFalse(score.isAtRisk());
    }
}
