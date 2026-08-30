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
class CampaignAnalyticsServiceTest {

    @Mock private EmailCampaignRepository campaignRepository;
    @Mock private CampaignVariantRepository variantRepository;
    @Mock private CampaignMetricRepository metricRepository;

    @InjectMocks
    private CampaignAnalyticsService campaignService;

    private EmailCampaign sampleCampaign;
    private CampaignVariant variantA;
    private CampaignVariant variantB;
    private CampaignCreateRequest sampleRequest;

    @BeforeEach
    void setUp() {
        sampleCampaign = EmailCampaign.builder()
                .id(1L)
                .name("Spring Promotion A/B Test")
                .campaignType("AB_TEST")
                .status("DRAFT")
                .totalRecipients(500L)
                .variantCount(2)
                .significanceThreshold(0.05)
                .primaryMetric("CLICK_RATE")
                .minSampleSize(100)
                .testDurationHours(48)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        variantA = CampaignVariant.builder()
                .id(10L)
                .campaignId(1L)
                .label("A")
                .subject("Spring Sale - 20% Off Everything!")
                .body("Don't miss our biggest sale of the season.")
                .tone("urgent")
                .trafficPercent(50.0)
                .sentCount(250L)
                .openCount(120L)
                .clickCount(45L)
                .replyCount(5L)
                .conversionCount(10L)
                .unsubscribeCount(2L)
                .bounceCount(1L)
                .isWinner(false)
                .build();

        variantB = CampaignVariant.builder()
                .id(11L)
                .campaignId(1L)
                .label("B")
                .subject("A Special Offer Just For You")
                .body("We've curated something special for our valued customers.")
                .tone("friendly")
                .trafficPercent(50.0)
                .sentCount(250L)
                .openCount(130L)
                .clickCount(55L)
                .replyCount(8L)
                .conversionCount(12L)
                .unsubscribeCount(1L)
                .bounceCount(0L)
                .isWinner(false)
                .build();

        sampleRequest = new CampaignCreateRequest();
        sampleRequest.setName("Spring Promotion");
        sampleRequest.setCampaignType("AB_TEST");
        sampleRequest.setTotalRecipients(500L);
        sampleRequest.setCreatedBy("admin");
    }

    // ─── Campaign Creation ─────────────────────────────────────────────

    @Test
    void testCreateCampaign_withVariants() {
        when(campaignRepository.save(any(EmailCampaign.class))).thenAnswer(inv -> {
            EmailCampaign c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });
        when(variantRepository.findByCampaignIdOrderByLabelAsc(1L)).thenReturn(List.of(variantA, variantB));
        when(variantRepository.save(any(CampaignVariant.class))).thenAnswer(inv -> inv.getArgument(0));

        VariantRequest vr1 = new VariantRequest();
        vr1.setLabel("A");
        vr1.setSubject("Subject A");
        vr1.setBody("Body A");
        vr1.setTone("urgent");

        VariantRequest vr2 = new VariantRequest();
        vr2.setLabel("B");
        vr2.setSubject("Subject B");
        vr2.setBody("Body B");
        vr2.setTone("friendly");

        sampleRequest.setVariants(List.of(vr1, vr2));

        EmailCampaign result = campaignService.createCampaign(sampleRequest);

        assertNotNull(result);
        assertEquals("Spring Promotion", result.getName());
        verify(variantRepository, atLeast(2)).save(any(CampaignVariant.class));
    }

    @Test
    void testCreateCampaign_throwsOnMissingName() {
        sampleRequest.setName("");

        assertThrows(IllegalArgumentException.class, () -> campaignService.createCampaign(sampleRequest));
    }

    @Test
    void testCreateCampaign_defaults() {
        when(campaignRepository.save(any(EmailCampaign.class))).thenAnswer(inv -> {
            EmailCampaign c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });

        EmailCampaign result = campaignService.createCampaign(sampleRequest);

        assertEquals("AB_TEST", result.getCampaignType());
        assertEquals("DRAFT", result.getStatus());
        assertEquals(0.05, result.getSignificanceThreshold());
    }

    // ─── Campaign Lifecycle ────────────────────────────────────────────

    @Test
    void testStartCampaign() {
        when(campaignRepository.findById(1L)).thenReturn(Optional.of(sampleCampaign));
        when(campaignRepository.save(any(EmailCampaign.class))).thenAnswer(inv -> inv.getArgument(0));

        EmailCampaign result = campaignService.startCampaign(1L);

        assertEquals("RUNNING", result.getStatus());
        assertNotNull(result.getStartDate());
    }

    @Test
    void testStartCampaign_throwsIfNotDraftOrPaused() {
        sampleCampaign.setStatus("RUNNING");
        when(campaignRepository.findById(1L)).thenReturn(Optional.of(sampleCampaign));

        assertThrows(IllegalStateException.class, () -> campaignService.startCampaign(1L));
    }

    @Test
    void testStartCampaign_throwsIfLessThanTwoVariants() {
        sampleCampaign.setVariantCount(1);
        when(campaignRepository.findById(1L)).thenReturn(Optional.of(sampleCampaign));

        assertThrows(IllegalStateException.class, () -> campaignService.startCampaign(1L));
    }

    @Test
    void testPauseCampaign() {
        sampleCampaign.setStatus("RUNNING");
        when(campaignRepository.findById(1L)).thenReturn(Optional.of(sampleCampaign));
        when(campaignRepository.save(any(EmailCampaign.class))).thenAnswer(inv -> inv.getArgument(0));

        EmailCampaign result = campaignService.pauseCampaign(1L);

        assertEquals("PAUSED", result.getStatus());
    }

    @Test
    void testPauseCampaign_throwsIfNotRunning() {
        sampleCampaign.setStatus("DRAFT");
        when(campaignRepository.findById(1L)).thenReturn(Optional.of(sampleCampaign));

        assertThrows(IllegalStateException.class, () -> campaignService.pauseCampaign(1L));
    }

    @Test
    void testCompleteCampaign() {
        sampleCampaign.setStatus("RUNNING");
        when(campaignRepository.findById(1L)).thenReturn(Optional.of(sampleCampaign));
        when(campaignRepository.save(any(EmailCampaign.class))).thenAnswer(inv -> inv.getArgument(0));
        when(variantRepository.findByCampaignIdOrderByLabelAsc(1L)).thenReturn(List.of(variantA, variantB));
        when(variantRepository.save(any(CampaignVariant.class))).thenAnswer(inv -> inv.getArgument(0));

        EmailCampaign result = campaignService.completeCampaign(1L);

        assertEquals("COMPLETED", result.getStatus());
        assertNotNull(result.getEndDate());
    }

    // ─── Metric Recording ──────────────────────────────────────────────

    @Test
    void testRecordEvent_open() {
        when(variantRepository.findById(10L)).thenReturn(Optional.of(variantA));
        when(metricRepository.save(any(CampaignMetric.class))).thenAnswer(inv -> inv.getArgument(0));
        when(variantRepository.save(any(CampaignVariant.class))).thenAnswer(inv -> inv.getArgument(0));

        MetricEventRequest request = new MetricEventRequest();
        request.setVariantId(10L);
        request.setEventType("OPEN");
        request.setDeviceType("mobile");

        CampaignMetric result = campaignService.recordEvent(1L, request);

        assertEquals("OPEN", result.getEventType());
        assertEquals(121L, variantA.getOpenCount()); // 120 + 1
    }

    @Test
    void testRecordEvent_click() {
        when(variantRepository.findById(10L)).thenReturn(Optional.of(variantA));
        when(metricRepository.save(any(CampaignMetric.class))).thenAnswer(inv -> inv.getArgument(0));
        when(variantRepository.save(any(CampaignVariant.class))).thenAnswer(inv -> inv.getArgument(0));

        MetricEventRequest request = new MetricEventRequest();
        request.setVariantId(10L);
        request.setEventType("CLICK");
        request.setRevenue(25.0);

        CampaignMetric result = campaignService.recordEvent(1L, request);

        assertEquals("CLICK", result.getEventType());
        assertEquals(25.0, result.getRevenue());
        assertEquals(46L, variantA.getClickCount());
    }

    @Test
    void testRecordEvent_throwsOnMissingVariant() {
        MetricEventRequest request = new MetricEventRequest();
        request.setEventType("OPEN");

        assertThrows(IllegalArgumentException.class, () -> campaignService.recordEvent(1L, request));
    }

    @Test
    void testRecordEvent_throwsOnMissingEventType() {
        MetricEventRequest request = new MetricEventRequest();
        request.setVariantId(10L);

        assertThrows(IllegalArgumentException.class, () -> campaignService.recordEvent(1L, request));
    }

    @Test
    void testRecordEventsBulk() {
        when(variantRepository.findById(10L)).thenReturn(Optional.of(variantA));
        when(metricRepository.save(any(CampaignMetric.class))).thenAnswer(inv -> inv.getArgument(0));
        when(variantRepository.save(any(CampaignVariant.class))).thenAnswer(inv -> inv.getArgument(0));

        MetricEventRequest r1 = new MetricEventRequest();
        r1.setVariantId(10L);
        r1.setEventType("SENT");

        MetricEventRequest r2 = new MetricEventRequest();
        r2.setVariantId(10L);
        r2.setEventType("OPEN");

        int recorded = campaignService.recordEventsBulk(1L, List.of(r1, r2));

        assertEquals(2, recorded);
    }

    // ─── Winner Selection ──────────────────────────────────────────────

    @Test
    void testSelectWinnerManually() {
        when(campaignRepository.findById(1L)).thenReturn(Optional.of(sampleCampaign));
        when(variantRepository.findByCampaignIdAndIsWinnerTrue(1L)).thenReturn(Collections.emptyList());
        when(variantRepository.findById(10L)).thenReturn(Optional.of(variantA));
        when(variantRepository.save(any(CampaignVariant.class))).thenAnswer(inv -> inv.getArgument(0));
        when(campaignRepository.save(any(EmailCampaign.class))).thenAnswer(inv -> inv.getArgument(0));

        CampaignVariant result = campaignService.selectWinnerManually(1L, 10L);

        assertTrue(result.getIsWinner());
        assertEquals(100.0, result.getConfidenceScore());
        assertEquals(10L, sampleCampaign.getWinningVariantId());
    }

    @Test
    void testSelectWinnerManually_variantNotFound() {
        when(campaignRepository.findById(1L)).thenReturn(Optional.of(sampleCampaign));
        when(variantRepository.findByCampaignIdAndIsWinnerTrue(1L)).thenReturn(Collections.emptyList());
        when(variantRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> campaignService.selectWinnerManually(1L, 99L));
    }

    // ─── Entity Method Tests ───────────────────────────────────────────

    @Test
    void testCampaignVariant_getClickRate() {
        double rate = variantA.getClickRate();
        assertEquals(18.0, rate); // 45/250 = 18%
    }

    @Test
    void testCampaignVariant_getOpenRate() {
        double rate = variantA.getOpenRate();
        assertEquals(48.0, rate); // 120/250 = 48%
    }

    @Test
    void testCampaignVariant_getReplyRate() {
        double rate = variantA.getReplyRate();
        assertEquals(2.0, rate); // 5/250 = 2%
    }

    @Test
    void testCampaignVariant_getConversionRate() {
        double rate = variantA.getConversionRate();
        assertEquals(4.0, rate); // 10/250 = 4%
    }

    @Test
    void testCampaignVariant_getUnsubscribeRate() {
        double rate = variantA.getUnsubscribeRate();
        assertEquals(0.8, rate); // 2/250 = 0.8%
    }

    @Test
    void testCampaignVariant_getBounceRate() {
        double rate = variantA.getBounceRate();
        assertEquals(0.4, rate); // 1/250 = 0.4%
    }

    @Test
    void testCampaignVariant_getTotalEngagement() {
        long total = variantA.getTotalEngagement();
        assertEquals(170L, total); // 120 + 45 + 5
    }

    @Test
    void testCampaignVariant_getEngagementRate() {
        double rate = variantA.getEngagementRate();
        assertEquals(68.0, rate); // 170/250 = 68%
    }

    @Test
    void testCampaignVariant_zeroSent() {
        CampaignVariant empty = CampaignVariant.builder()
                .sentCount(0L).openCount(0L).clickCount(0L).build();
        assertEquals(0.0, empty.getClickRate());
        assertEquals(0.0, empty.getOpenRate());
        assertEquals(0L, empty.getTotalEngagement());
    }

    @Test
    void testEmailCampaign_isTestPeriodComplete() {
        sampleCampaign.setEndDate(LocalDateTime.now().minusHours(1));
        assertTrue(sampleCampaign.isTestPeriodComplete());

        sampleCampaign.setEndDate(LocalDateTime.now().plusHours(1));
        assertFalse(sampleCampaign.isTestPeriodComplete());
    }

    @Test
    void testEmailCampaign_hasMinimumSampleSize() {
        sampleCampaign.setMinSampleSize(100);
        sampleCampaign.setTotalRecipients(500L);
        assertTrue(sampleCampaign.hasMinimumSampleSize());

        sampleCampaign.setTotalRecipients(50L);
        assertFalse(sampleCampaign.hasMinimumSampleSize());
    }

    // ─── Delete Campaign ───────────────────────────────────────────────

    @Test
    void testDeleteCampaign() {
        when(campaignRepository.existsById(1L)).thenReturn(true);
        when(variantRepository.findByCampaignIdOrderByLabelAsc(1L)).thenReturn(List.of(variantA, variantB));

        boolean result = campaignService.deleteCampaign(1L);

        assertTrue(result);
        verify(campaignRepository).deleteById(1L);
        verify(variantRepository).deleteAll(anyList());
    }

    @Test
    void testDeleteCampaign_notFound() {
        when(campaignRepository.existsById(99L)).thenReturn(false);

        boolean result = campaignService.deleteCampaign(99L);

        assertFalse(result);
    }

    // ─── CampaignAnalyticsResponse Builder ─────────────────────────────

    @Test
    void testCampaignAnalyticsResponse_build() {
        CampaignAnalyticsResponse.VariantAnalytics va = CampaignAnalyticsResponse.VariantAnalytics.builder()
                .variantId(10L)
                .label("A")
                .clickRate(18.0)
                .isWinner(false)
                .build();

        CampaignAnalyticsResponse response = CampaignAnalyticsResponse.builder()
                .campaignId(1L)
                .campaignName("Test")
                .variantAnalytics(List.of(va))
                .totalRevenue(250.0)
                .build();

        assertEquals(1L, response.getCampaignId());
        assertEquals(1, response.getVariantAnalytics().size());
        assertEquals(250.0, response.getTotalRevenue());
    }

    @Test
    void testCampaignStats_build() {
        Map<String, Long> statusMap = Map.of("RUNNING", 3L, "COMPLETED", 5L);
        CampaignStats stats = CampaignStats.builder()
                .totalCampaigns(8)
                .activeCampaigns(3)
                .completedCampaigns(5)
                .overallClickRate(15.5)
                .statusBreakdown(statusMap)
                .build();

        assertEquals(8, stats.getTotalCampaigns());
        assertEquals(15.5, stats.getOverallClickRate());
    }
}
