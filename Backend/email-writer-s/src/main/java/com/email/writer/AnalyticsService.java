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
public class AnalyticsService {

    @Autowired
    private EmailEventRepository emailEventRepository;

    @Autowired
    private EngagementScoreRepository engagementScoreRepository;

    @Autowired
    private AudienceSegmentRepository audienceSegmentRepository;

    // ===== Event Tracking =====

    public EmailEvent trackEvent(TrackEventRequest request) {
        EmailEvent event = new EmailEvent(request.getEventType(), request.getUserId(), request.getRecipientEmail());
        event.setEmailId(request.getEmailId());
        event.setCampaignId(request.getCampaignId());
        event.setSenderEmail(request.getSenderEmail());
        event.setSubject(request.getSubject());
        event.setLinkUrl(request.getLinkUrl());
        event.setLinkText(request.getLinkText());
        event.setBounceType(request.getBounceType());
        event.setBounceReason(request.getBounceReason());
        event.setDeviceType(request.getDeviceType());
        event.setDeviceOs(request.getDeviceOs());
        event.setEmailClient(request.getEmailClient());
        event.setIpAddress(request.getIpAddress());
        event.setGeoCountry(request.getGeoCountry());
        event.setGeoCity(request.getGeoCity());
        event.setMetadataJson(request.getMetadataJson());
        if (request.getEventTimestamp() != null) {
            event.setEventTimestamp(request.getEventTimestamp());
        }

        // Calculate timing for engagement events
        if (event.isEngagementEvent() && request.getEmailId() != null) {
            calculateTiming(event, request.getUserId(), request.getRecipientEmail(), request.getEventTimestamp());
        }

        EmailEvent saved = emailEventRepository.save(event);

        // Update engagement score
        updateEngagementScore(request.getUserId(), request.getRecipientEmail());

        return saved;
    }

    public List<EmailEvent> batchTrackEvents(List<TrackEventRequest> requests) {
        List<EmailEvent> results = new ArrayList<>();
        Set<String> affectedEmails = new HashSet<>();
        for (TrackEventRequest request : requests) {
            results.add(trackEvent(request));
            affectedEmails.add(request.getRecipientEmail());
        }
        return results;
    }

    public List<EmailEvent> getUserEvents(String userId) {
        return emailEventRepository.findByUserId(userId);
    }

    public List<EmailEvent> getEventsByType(String userId, String eventType) {
        return emailEventRepository.findByUserIdAndEventType(userId, eventType);
    }

    public List<EmailEvent> getCampaignEvents(Long campaignId) {
        return emailEventRepository.findByCampaignId(campaignId);
    }

    public List<EmailEvent> getRecipientEvents(String email) {
        return emailEventRepository.findByRecipientEmail(email);
    }

    public List<EmailEvent> getEventsInDateRange(LocalDateTime start, LocalDateTime end) {
        return emailEventRepository.findByEventTimestampBetween(start, end);
    }

    // ===== Engagement Scoring =====

    public EngagementScore getEngagementScore(String email) {
        EngagementScore score = engagementScoreRepository.findByEmail(email);
        if (score == null) {
            score = new EngagementScore();
            score.setEmail(email);
            score.setUserId("SYSTEM");
        }
        return score;
    }

    public List<EngagementScore> getTopEngaged(String userId) {
        return engagementScoreRepository.findTopEngaged(userId);
    }

    public List<EngagementScore> getTopEngagedWithMinSends(String userId, long minSends) {
        return engagementScoreRepository.findTopEngagedWithMinSends(userId, minSends);
    }

    public List<EngagementScore> getRecipientsByTier(String userId, String tier) {
        return engagementScoreRepository.findByUserIdAndEngagementTier(userId, tier);
    }

    public List<EngagementScore> getAtRiskRecipients(String userId) {
        return engagementScoreRepository.findByIsAtRisk(true);
    }

    public List<EngagementScore> getVipRecipients(String userId) {
        return engagementScoreRepository.findByIsVip(true);
    }

    public List<EngagementScore> getInactiveRecipients(String userId, int days) {
        return engagementScoreRepository.findInactiveForDays(userId, days);
    }

    public List<EngagementScore> getBelowThresholdRecipients(String userId, double threshold) {
        return engagementScoreRepository.findBelowThreshold(userId, threshold);
    }

    public Map<String, Long> getTierBreakdown(String userId) {
        List<Object[]> raw = engagementScoreRepository.countByTierForUser(userId);
        Map<String, Long> map = new LinkedHashMap<>();
        for (Object[] row : raw) {
            map.put(String.valueOf(row[0]), ((Number) row[1]).longValue());
        }
        return map;
    }

    public EngagementScore recalculateScore(String email) {
        EngagementScore score = engagementScoreRepository.findByEmail(email);
        if (score == null) return null;
        recalculateMetrics(score);
        return engagementScoreRepository.save(score);
    }

    // ===== Audience Segments =====

    public AudienceSegment createSegment(SegmentRequest request) {
        AudienceSegment segment = new AudienceSegment(
            request.getUserId(),
            request.getName(),
            request.getSegmentType() != null ? request.getSegmentType() : "DYNAMIC"
        );
        segment.setDescription(request.getDescription());
        segment.setCriteriaJson(request.getCriteriaJson());
        segment.setTriggerEvent(request.getTriggerEvent());
        segment.setTriggerWindowDays(request.getTriggerWindowDays());
        segment.setTriggerMinCount(request.getTriggerMinCount());
        return audienceSegmentRepository.save(segment);
    }

    public List<AudienceSegment> getUserSegments(String userId) {
        return audienceSegmentRepository.findByUserId(userId);
    }

    public List<AudienceSegment> getActiveSegments(String userId) {
        return audienceSegmentRepository.findByUserIdAndIsActive(userId, true);
    }

    public List<AudienceSegment> getNonEmptySegments(String userId) {
        return audienceSegmentRepository.findNonEmptyByUser(userId);
    }

    public AudienceSegment getSegmentById(Long id) {
        return audienceSegmentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Segment not found: " + id));
    }

    public AudienceSegment updateSegmentMemberCount(Long segmentId) {
        AudienceSegment segment = getSegmentById(segmentId);
        // Count matching engagement scores based on segment type and criteria
        List<EngagementScore> allScores = engagementScoreRepository.findByUserId(segment.getUserId());
        long memberCount = 0;
        long activeCount = 0;
        double totalScore = 0;
        Map<String, Long> tierCounts = new HashMap<>();

        for (EngagementScore score : allScores) {
            if (matchesSegmentCriteria(score, segment)) {
                memberCount++;
                activeCount++;
                totalScore += score.getEngagementScore();
                tierCounts.merge(score.getEngagementTier(), 1L, Long::sum);
            }
        }

        segment.setMemberCount(memberCount);
        segment.setActiveMemberCount(activeCount);
        segment.setAvgEngagementScore(memberCount > 0 ? totalScore / memberCount : 0);
        segment.setDominantTier(tierCounts.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("UNKNOWN"));
        segment.setLastRefreshedAt(LocalDateTime.now());
        return audienceSegmentRepository.save(segment);
    }

    public void deleteSegment(Long segmentId) {
        audienceSegmentRepository.deleteById(segmentId);
    }

    // ===== Analytics Dashboard =====

    public AnalyticsDashboard getDashboard(String userId) {
        AnalyticsDashboard dashboard = new AnalyticsDashboard();

        long sent = emailEventRepository.countSent(userId);
        long delivered = emailEventRepository.countDelivered(userId);
        long opened = emailEventRepository.countOpened(userId);
        long clicked = emailEventRepository.countClicked(userId);
        long bounced = emailEventRepository.countBounced(userId);
        long complained = emailEventRepository.countComplained(userId);
        long unsubscribed = emailEventRepository.countUnsubscribed(userId);

        dashboard.setTotalSent(sent);
        dashboard.setTotalDelivered(delivered);
        dashboard.setTotalOpened(opened);
        dashboard.setTotalClicked(clicked);
        dashboard.setTotalBounced(bounced);
        dashboard.setTotalComplaints(complained);
        dashboard.setTotalUnsubscribed(unsubscribed);

        dashboard.setOpenRate(sent > 0 ? (double) opened / sent * 100 : 0);
        dashboard.setClickRate(sent > 0 ? (double) clicked / sent * 100 : 0);
        dashboard.setBounceRate(sent > 0 ? (double) bounced / sent * 100 : 0);
        dashboard.setComplaintRate(sent > 0 ? (double) complained / sent * 100 : 0);
        dashboard.setUnsubscribeRate(sent > 0 ? (double) unsubscribed / sent * 100 : 0);
        dashboard.setDeliveryRate(sent > 0 ? (double) delivered / sent * 100 : 0);
        dashboard.setClickToOpenRate(opened > 0 ? (double) clicked / opened * 100 : 0);

        Double avgOpen = emailEventRepository.avgTimeToOpen(userId);
        Double avgClick = emailEventRepository.avgTimeToClick(userId);
        dashboard.setAvgTimeToOpenSeconds(avgOpen);
        dashboard.setAvgTimeToClickSeconds(avgClick);

        dashboard.setOpensByDevice(toLongMap(emailEventRepository.countOpensByDevice(userId)));
        dashboard.setOpensByClient(toLongMap(emailEventRepository.countOpensByClient(userId)));
        dashboard.setEngagementByCountry(toLongMap(emailEventRepository.countEngagementByCountry(userId)));
        dashboard.setEventsByType(toLongMap(emailEventRepository.countByEventTypeForUser(userId)));
        dashboard.setDailyTrend(toLongMap(emailEventRepository.dailyEventTrend(userId, LocalDateTime.now().minusDays(30))));

        // Engagement breakdown
        Map<String, Long> tiers = getTierBreakdown(userId);
        dashboard.setHotRecipients(tiers.getOrDefault("HOT", 0L));
        dashboard.setWarmRecipients(tiers.getOrDefault("WARM", 0L));
        dashboard.setColdRecipients(tiers.getOrDefault("COLD", 0L));
        dashboard.setInactiveRecipients(tiers.getOrDefault("INACTIVE", 0L) + tiers.getOrDefault("UNENGAGED", 0L));

        List<EngagementScore> atRisk = engagementScoreRepository.findByIsAtRisk(true);
        dashboard.setAtRiskRecipients(atRisk.size());
        List<EngagementScore> vip = engagementScoreRepository.findByIsVip(true);
        dashboard.setVipRecipients(vip.size());

        Double avgScore = engagementScoreRepository.avgScoreForUser(userId);
        dashboard.setAvgEngagementScore(avgScore != null ? avgScore : 0);

        List<AudienceSegment> segments = audienceSegmentRepository.findByUserId(userId);
        dashboard.setTotalSegments(segments.size());
        dashboard.setTotalRecipients(engagementScoreRepository.findByUserId(userId).size());

        return dashboard;
    }

    public List<Object[]> getTopOpeners(String userId) {
        return emailEventRepository.topOpeners(userId);
    }

    public List<Object[]> getTopClickers(String userId) {
        return emailEventRepository.topClickers(userId);
    }

    // ===== Helper Methods =====

    private void calculateTiming(EmailEvent event, String userId, String email, LocalDateTime eventTime) {
        List<EmailEvent> sentEvents = emailEventRepository.findByUserIdAndEventType(userId, "SENT");
        Optional<EmailEvent> lastSent = sentEvents.stream()
            .filter(e -> email.equals(e.getRecipientEmail()))
            .max(Comparator.comparing(EmailEvent::getEventTimestamp));

        if (lastSent.isPresent() && eventTime != null) {
            long seconds = ChronoUnit.SECONDS.between(lastSent.get().getEventTimestamp(), eventTime);
            if (seconds > 0) {
                if ("OPENED".equals(event.getEventType())) {
                    event.setTimeToOpenSeconds(seconds);
                    event.setIsFirstOpen(emailEventRepository.findByUserIdAndEventType(userId, "OPENED").stream()
                        .noneMatch(e -> email.equals(e.getRecipientEmail())));
                } else if ("CLICKED".equals(event.getEventType())) {
                    event.setTimeToClickSeconds(seconds);
                    event.setIsFirstClick(emailEventRepository.findByUserIdAndEventType(userId, "CLICKED").stream()
                        .noneMatch(e -> email.equals(e.getRecipientEmail())));
                }
            }
        }
    }

    private void updateEngagementScore(String userId, String email) {
        EngagementScore score = engagementScoreRepository.findByEmail(email);
        if (score == null) {
            score = new EngagementScore(userId, email);
        }
        recalculateMetrics(score);
        engagementScoreRepository.save(score);
    }

    private void recalculateMetrics(EngagementScore score) {
        String email = score.getEmail();
        long sent = emailEventRepository.findByUserIdAndEventType(score.getUserId(), "SENT").stream()
            .filter(e -> email.equals(e.getRecipientEmail())).count();
        long delivered = emailEventRepository.findByUserIdAndEventType(score.getUserId(), "DELIVERED").stream()
            .filter(e -> email.equals(e.getRecipientEmail())).count();
        long opened = emailEventRepository.findByUserIdAndEventType(score.getUserId(), "OPENED").stream()
            .filter(e -> email.equals(e.getRecipientEmail())).count();
        long clicked = emailEventRepository.findByUserIdAndEventType(score.getUserId(), "CLICKED").stream()
            .filter(e -> email.equals(e.getRecipientEmail())).count();
        long bounced = emailEventRepository.findByUserIdAndEventType(score.getUserId(), "BOUNCED").stream()
            .filter(e -> email.equals(e.getRecipientEmail())).count();
        long complained = emailEventRepository.findByUserIdAndEventType(score.getUserId(), "COMPLAINED").stream()
            .filter(e -> email.equals(e.getRecipientEmail())).count();
        long unsubscribed = emailEventRepository.findByUserIdAndEventType(score.getUserId(), "UNSUBSCRIBED").stream()
            .filter(e -> email.equals(e.getRecipientEmail())).count();

        score.setTotalSent(sent);
        score.setTotalDelivered(delivered);
        score.setTotalOpened(opened);
        score.setTotalClicked(clicked);
        score.setTotalBounced(bounced);
        score.setTotalComplaints(complained);
        score.setTotalUnsubscribed(unsubscribed);

        score.setOpenRate(sent > 0 ? (double) opened / sent * 100 : 0);
        score.setClickRate(sent > 0 ? (double) clicked / sent * 100 : 0);
        score.setBounceRate(sent > 0 ? (double) bounced / sent * 100 : 0);
        score.setComplaintRate(sent > 0 ? (double) complained / sent * 100 : 0);
        score.setUnsubscribeRate(sent > 0 ? (double) unsubscribed / sent * 100 : 0);

        // Update timing
        List<EmailEvent> openEvents = emailEventRepository.findByUserIdAndEventType(score.getUserId(), "OPENED").stream()
            .filter(e -> email.equals(e.getRecipientEmail()) && e.getTimeToOpenSeconds() != null)
            .collect(Collectors.toList());
        if (!openEvents.isEmpty()) {
            score.setAvgTimeToOpenSeconds((long) openEvents.stream().mapToLong(EmailEvent::getTimeToOpenSeconds).average().orElse(0));
            score.setMaxTimeToOpenSeconds(openEvents.stream().mapToLong(EmailEvent::getTimeToOpenSeconds).max().orElse(0));
        }

        List<EmailEvent> clickEvents = emailEventRepository.findByUserIdAndEventType(score.getUserId(), "CLICKED").stream()
            .filter(e -> email.equals(e.getRecipientEmail()) && e.getTimeToClickSeconds() != null)
            .collect(Collectors.toList());
        if (!clickEvents.isEmpty()) {
            score.setAvgTimeToClickSeconds((long) clickEvents.stream().mapToLong(EmailEvent::getTimeToClickSeconds).average().orElse(0));
            score.setMaxTimeToClickSeconds(clickEvents.stream().mapToLong(EmailEvent::getTimeToClickSeconds).max().orElse(0));
        }

        // Update last engagement timestamps
        emailEventRepository.findByUserIdAndEventType(score.getUserId(), "OPENED").stream()
            .filter(e -> email.equals(e.getRecipientEmail()))
            .max(Comparator.comparing(EmailEvent::getEventTimestamp))
            .ifPresent(e -> {
                score.setLastOpenAt(e.getEventTimestamp());
                score.setLastEngagementAt(e.getEventTimestamp());
            });

        emailEventRepository.findByUserIdAndEventType(score.getUserId(), "CLICKED").stream()
            .filter(e -> email.equals(e.getRecipientEmail()))
            .max(Comparator.comparing(EmailEvent::getEventTimestamp))
            .ifPresent(score::setLastClickAt);

        emailEventRepository.findByUserIdAndEventType(score.getUserId(), "SENT").stream()
            .filter(e -> email.equals(e.getRecipientEmail()))
            .max(Comparator.comparing(EmailEvent::getEventTimestamp))
            .ifPresent(e -> score.setLastSentAt(e.getEventTimestamp()));

        if (score.getLastEngagementAt() != null) {
            score.setDaysSinceLastEngagement((int) ChronoUnit.DAYS.between(score.getLastEngagementAt(), LocalDateTime.now()));
        }

        score.computeEngagementScore();
        score.setCalculatedAt(LocalDateTime.now());
    }

    private boolean matchesSegmentCriteria(EngagementScore score, AudienceSegment segment) {
        if ("ENGAGEMENT_BASED".equals(segment.getSegmentType())) {
            if (score.getEngagementTier() == null) return false;
            String tier = score.getEngagementTier();
            if ("HOT".equals(tier) || "WARM".equals(tier)) return true;
            return false;
        }
        if ("BEHAVIORAL".equals(segment.getSegmentType())) {
            if (segment.getTriggerMinCount() != null && score.getTotalOpened() < segment.getTriggerMinCount()) {
                return false;
            }
            return true;
        }
        return true; // static segments include all
    }

    private Map<String, Long> toLongMap(List<Object[]> grouped) {
        Map<String, Long> map = new LinkedHashMap<>();
        for (Object[] row : grouped) {
            map.put(String.valueOf(row[0]), ((Number) row[1]).longValue());
        }
        return map;
    }
}
