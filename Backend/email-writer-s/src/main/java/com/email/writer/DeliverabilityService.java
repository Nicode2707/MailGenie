package com.email.writer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Enterprise service for email deliverability tracking, sender reputation scoring,
 * blocklist monitoring, and domain health reporting.
 * Uses weighted scoring with configurable penalties for negative events.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeliverabilityService {

    private final DeliverabilityRecordRepository recordRepository;
    private final SenderReputationRepository reputationRepository;
    private final DomainBlocklistEntryRepository blocklistRepository;

    // Scoring weights for reputation calculation
    private static final double WEIGHT_DELIVERED = 1.0;
    private static final double WEIGHT_OPEN = 0.3;
    private static final double WEIGHT_CLICK = 0.5;
    private static final double PENALTY_HARD_BOUNCE = -5.0;
    private static final double PENALTY_SOFT_BOUNCE = -1.0;
    private static final double PENALTY_SPAM_COMPLAINT = -15.0;
    private static final double PENALTY_UNSUBSCRIBE = -3.0;
    private static final double PENALTY_BLOCKLIST = -20.0;
    private static final double BONUS_SPF_PASS = 0.5;
    private static final double BONUS_DKIM_PASS = 0.5;
    private static final double BONUS_DMARC_PASS = 1.0;

    // ─── Record Events ─────────────────────────────────────────────────

    /**
     * Record a deliverability event and update sender reputation.
     */
    @Transactional
    public DeliverabilityRecord recordEvent(DeliverabilityRecordRequest request) {
        if (request.getSenderEmail() == null || request.getSenderEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Sender email is required.");
        }
        if (request.getRecipientEmail() == null || request.getRecipientEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Recipient email is required.");
        }
        if (request.getEventType() == null || request.getEventType().trim().isEmpty()) {
            throw new IllegalArgumentException("Event type is required.");
        }

        String senderDomain = extractDomain(request.getSenderEmail());
        String recipientDomain = extractDomain(request.getRecipientEmail());

        DeliverabilityRecord record = DeliverabilityRecord.builder()
                .senderEmail(request.getSenderEmail().trim())
                .senderDomain(senderDomain)
                .recipientEmail(request.getRecipientEmail().trim())
                .recipientDomain(recipientDomain)
                .eventType(request.getEventType().toUpperCase())
                .bounceType(request.getBounceType())
                .smtpCode(request.getSmtpCode())
                .smtpDiagnostic(request.getSmtpDiagnostic())
                .isCampaignEmail(request.getCampaignId() != null)
                .campaignId(request.getCampaignId())
                .variantId(request.getVariantId())
                .sendingIp(request.getSendingIp())
                .contentType(request.getContentType() != null ? request.getContentType() : "html")
                .subjectLine(request.getSubjectLine())
                .spamScore(request.getSpamScore() != null ? request.getSpamScore() : 0.0)
                .spfPass(request.getSpfPass())
                .dkimPass(request.getDkimPass())
                .dmarcPass(request.getDmarcPass())
                .latencyMs(request.getLatencyMs())
                .sentAt(LocalDateTime.now())
                .build();

        DeliverabilityRecord saved = recordRepository.save(record);

        // Update sender reputation
        updateSenderReputation(senderDomain);

        return saved;
    }

    /**
     * Record multiple events in bulk.
     */
    @Transactional
    public int recordEventsBulk(List<DeliverabilityRecordRequest> events) {
        int recorded = 0;
        Set<String> domainsToUpdate = new HashSet<>();
        for (DeliverabilityRecordRequest event : events) {
            try {
                DeliverabilityRecord record = recordEvent(event);
                domainsToUpdate.add(record.getSenderDomain());
                recorded++;
            } catch (Exception e) {
                log.warn("Failed to record deliverability event: {}", e.getMessage());
            }
        }
        // Recalculate reputation for affected domains
        for (String domain : domainsToUpdate) {
            updateSenderReputation(domain);
        }
        return recorded;
    }

    // ─── Reputation Calculation ────────────────────────────────────────

    /**
     * Recalculate and update the reputation score for a domain.
     */
    @Transactional
    public SenderReputation updateSenderReputation(String domain) {
        SenderReputation reputation = reputationRepository.findByDomain(domain)
                .orElseGet(() -> SenderReputation.builder().domain(domain).build());

        // Gather counts
        long totalSent = recordRepository.countBySenderDomain(domain);
        long delivered = recordRepository.countBySenderDomainAndEventType(domain, "DELIVERED");
        long bounced = recordRepository.countBySenderDomainAndEventType(domain, "BOUNCED");
        long complaints = recordRepository.countBySenderDomainAndEventType(domain, "SPAM_COMPLAINT");
        long unsubscribes = recordRepository.countBySenderDomainAndEventType(domain, "UNSUBSCRIBED");
        long opens = recordRepository.countBySenderDomainAndEventType(domain, "OPENED");
        long clicks = recordRepository.countBySenderDomainAndEventType(domain, "CLICKED");

        // Update counts
        reputation.setTotalSent(totalSent);
        reputation.setTotalDelivered(delivered);
        reputation.setHardBounces(countByBounceType(domain, "HARD"));
        reputation.setSoftBounces(countByBounceType(domain, "SOFT"));
        reputation.setSpamComplaints(complaints);
        reputation.setUnsubscribes(unsubscribes);
        reputation.setTotalOpens(opens);
        reputation.setTotalClicks(clicks);

        // Calculate rates
        reputation.setDeliverabilityRate(totalSent > 0 ? round2((double) delivered / totalSent * 100) : 100.0);
        reputation.setBounceRate(totalSent > 0 ? round2((double) bounced / totalSent * 100) : 0.0);
        reputation.setSpamComplaintRate(totalSent > 0 ? round2((double) complaints / totalSent * 100) : 0.0);
        reputation.setOpenRate(totalSent > 0 ? round2((double) opens / totalSent * 100) : 0.0);
        reputation.setClickRate(totalSent > 0 ? round2((double) clicks / totalSent * 100) : 0.0);

        // Auth pass rates
        long totalWithSpf = totalSent; // Simplified
        long spfPass = recordRepository.spfPassCount(domain);
        long dkimPass = recordRepository.dkimPassCount(domain);
        long dmarcPass = recordRepository.dmarcPassCount(domain);
        reputation.setSpfPassRate(totalSent > 0 ? round2((double) spfPass / totalSent * 100) : 100.0);
        reputation.setDkimPassRate(totalSent > 0 ? round2((double) dkimPass / totalSent * 100) : 100.0);
        reputation.setDmarcPassRate(totalSent > 0 ? round2((double) dmarcPass / totalSent * 100) : 100.0);

        // Spam score
        Double avgSpam = recordRepository.averageSpamScoreByDomain(domain);
        reputation.setAvgSpamScore(avgSpam != null ? Math.round(avgSpam * 100.0) / 100.0 : 0.0);

        // Unique recipient domains
        List<Object[]> recipientDomains = recordRepository.countByRecipientDomain(domain);
        reputation.setUniqueRecipientDomains(recipientDomains.size());

        // Blocklist count
        long blocklistCount = blocklistRepository.countByDomainAndIsListedTrue(domain);
        List<String> warnings = blocklistRepository.findByDomainAndIsListedTrue(domain).stream()
                .map(DomainBlocklistEntry::getBlocklistName).collect(Collectors.toList());
        reputation.setBlocklistWarnings(warnings.isEmpty() ? null : String.join(",", warnings));

        // Calculate reputation score
        double score = calculateReputationScore(reputation);
        reputation.setReputationScore(Math.max(0, Math.min(100, score)));
        reputation.setHealthGrade(scoreToGrade(reputation.getReputationScore()));
        reputation.setRiskLevel(scoreToRiskLevel(reputation.getReputationScore()));
        reputation.setLastCalculatedAt(LocalDateTime.now());

        return reputationRepository.save(reputation);
    }

    /**
     * Calculate the reputation score using weighted metrics.
     */
    private double calculateReputationScore(SenderReputation r) {
        double score = 50.0; // Start at neutral

        if (r.getTotalSent() == 0) return score;

        // Positive signals
        score += r.getDeliverabilityRate() * 0.3;       // Max +30
        score += Math.min(r.getOpenRate() * 0.1, 10);   // Max +10
        score += Math.min(r.getClickRate() * 0.2, 10);  // Max +10

        // Auth bonuses
        score += (r.getSpfPassRate() / 100.0) * 3;     // Max +3
        score += (r.getDkimPassRate() / 100.0) * 3;     // Max +3
        score += (r.getDmarcPassRate() / 100.0) * 4;    // Max +4

        // Negative signals
        score += r.getBounceRate() * PENALTY_HARD_BOUNCE * 0.1;  // Up to -5
        score += r.getSpamComplaintRate() * PENALTY_SPAM_COMPLAINT * 0.5; // Up to -15
        score += Math.min(r.getUnsubscribes() / Math.max(r.getTotalSent(), 1.0) * 100, 5) * PENALTY_UNSUBSCRIBE * 0.1;

        // Blocklist penalty
        long blocklistCount = blocklistRepository.countByDomainAndIsListedTrue(r.getDomain());
        score += blocklistCount * PENALTY_BLOCKLIST;

        // Spam score penalty
        if (r.getAvgSpamScore() != null && r.getAvgSpamScore() > 3.0) {
            score -= (r.getAvgSpamScore() - 3.0) * 5;
        }

        return Math.max(0, Math.min(100, score));
    }

    private long countByBounceType(String domain, String type) {
        return recordRepository.findBySenderDomainOrderBySentAtDesc(domain).stream()
                .filter(r -> "BOUNCED".equals(r.getEventType()) && type.equalsIgnoreCase(r.getBounceType()))
                .count();
    }

    // ─── Domain Health Report ──────────────────────────────────────────

    /**
     * Generate a comprehensive health report for a domain.
     */
    public DomainHealthReport getDomainHealthReport(String domain) {
        SenderReputation reputation = reputationRepository.findByDomain(domain)
                .orElseGet(() -> SenderReputation.builder().domain(domain)
                        .reputationScore(50.0).healthGrade("C").riskLevel("LOW")
                        .totalSent(0L).totalDelivered(0L).build());

        // Event breakdown
        List<Object[]> eventBreakdown = recordRepository.countByEventTypeForDomain(domain);
        Map<String, Long> eventTypeMap = eventBreakdown.stream()
                .collect(Collectors.toMap(r -> (String) r[0], r -> (Long) r[1],
                        (a, b) -> a, LinkedHashMap::new));

        // Bounce breakdown
        List<Object[]> bounceBreakdown = recordRepository.bounceBreakdownByDomain(domain);
        Map<String, Long> bounceMap = bounceBreakdown.stream()
                .collect(Collectors.toMap(r -> (String) r[0], r -> (Long) r[1],
                        (a, b) -> a, LinkedHashMap::new));

        // Recipient domain breakdown
        List<Object[]> recipientBreakdown = recordRepository.countByRecipientDomain(domain);
        Map<String, Long> recipientMap = recipientBreakdown.stream()
                .collect(Collectors.toMap(r -> (String) r[0], r -> (Long) r[1],
                        (a, b) -> a, LinkedHashMap::new));

        // SMTP code distribution
        List<Object[]> smtpCodes = recordRepository.smtpCodeDistribution(domain);
        Map<String, Long> smtpMap = smtpCodes.stream()
                .collect(Collectors.toMap(r -> String.valueOf(r[0]), r -> (Long) r[1],
                        (a, b) -> a, LinkedHashMap::new));

        // Blocklist info
        List<DomainBlocklistEntry> blocklists = blocklistRepository.findByDomain(domain);
        List<String> activeBlocklists = blocklists.stream()
                .filter(DomainBlocklistEntry::getIsListed)
                .map(DomainBlocklistEntry::getBlocklistName)
                .collect(Collectors.toList());

        // Recommendations
        List<String> recommendations = generateRecommendations(reputation);

        return DomainHealthReport.builder()
                .domain(domain)
                .reputationScore(reputation.getReputationScore())
                .healthGrade(reputation.getHealthGrade())
                .riskLevel(reputation.getRiskLevel())
                .totalSent(reputation.getTotalSent())
                .totalDelivered(reputation.getTotalDelivered())
                .deliverabilityRate(reputation.getDeliverabilityRate())
                .bounceRate(reputation.getBounceRate())
                .spamComplaintRate(reputation.getSpamComplaintRate())
                .openRate(reputation.getOpenRate())
                .clickRate(reputation.getClickRate())
                .avgSpamScore(reputation.getAvgSpamScore())
                .spfPassRate(reputation.getSpfPassRate())
                .dkimPassRate(reputation.getDkimPassRate())
                .dmarcPassRate(reputation.getDmarcPassRate())
                .streakDays(reputation.getStreakDays())
                .activeBlocklistCount(activeBlocklists.size())
                .blocklistWarnings(activeBlocklists)
                .eventTypeBreakdown(eventTypeMap)
                .bounceBreakdown(bounceMap)
                .recipientDomainBreakdown(recipientMap)
                .smtpCodeDistribution(smtpMap)
                .recommendations(recommendations)
                .isHealthy(reputation.isHealthy())
                .isAtRisk(reputation.isAtRisk())
                .build();
    }

    // ─── Blocklist Management ──────────────────────────────────────────

    /**
     * Check and record blocklist status for a domain.
     */
    @Transactional
    public DomainBlocklistEntry checkBlocklist(String domain, String blocklistName, boolean isListed) {
        DomainBlocklistEntry entry = blocklistRepository.findByDomainAndBlocklistName(domain, blocklistName)
                .orElseGet(() -> DomainBlocklistEntry.builder()
                        .domain(domain)
                        .blocklistName(blocklistName)
                        .build());

        boolean wasListed = Boolean.TRUE.equals(entry.getIsListed());
        entry.setIsListed(isListed);
        entry.setLastCheckedAt(LocalDateTime.now());

        if (isListed && !wasListed) {
            entry.setListedAt(LocalDateTime.now());
            entry.setAlertSent(false);
            log.warn("Domain {} is NOW LISTED on blocklist {}", domain, blocklistName);
        } else if (!isListed && wasListed) {
            entry.setDelistedAt(LocalDateTime.now());
            log.info("Domain {} has been DE-LISTED from blocklist {}", domain, blocklistName);
        }

        return blocklistRepository.save(entry);
    }

    /**
     * Get all active blocklist entries across all domains.
     */
    public List<DomainBlocklistEntry> getActiveBlocklistEntries() {
        return blocklistRepository.findByIsListedTrue();
    }

    /**
     * Get blocklist entries for a specific domain.
     */
    public List<DomainBlocklistEntry> getDomainBlocklistEntries(String domain) {
        return blocklistRepository.findByDomain(domain);
    }

    // ─── Queries ───────────────────────────────────────────────────────

    /**
     * Get all tracked sender reputations ranked by score.
     */
    public List<SenderReputation> getAllReputations() {
        return reputationRepository.findAllByOrderByReputationScoreDesc();
    }

    /**
     * Get reputation for a specific domain.
     */
    public SenderReputation getReputation(String domain) {
        return reputationRepository.findByDomain(domain)
                .orElseThrow(() -> new NoSuchElementException("No reputation data for domain: " + domain));
    }

    /**
     * Get domains at risk.
     */
    public List<SenderReputation> getAtRiskDomains() {
        return reputationRepository.findByRiskLevelOrderByReputationScoreAsc("HIGH");
    }

    /**
     * Get domains with high spam complaint rates.
     */
    public List<SenderReputation> getHighComplaintDomains(double threshold) {
        return reputationRepository.findAboveComplaintRate(threshold);
    }

    /**
     * Get deliverability records for a domain within a time range.
     */
    public List<DeliverabilityRecord> getRecordsInRange(String domain, LocalDateTime start, LocalDateTime end) {
        return recordRepository.findBySenderDomainAndSentAtBetweenOrderBySentAtDesc(domain, start, end);
    }

    // ─── System Stats ──────────────────────────────────────────────────

    /**
     * Get system-wide deliverability and reputation statistics.
     */
    public ReputationStats getSystemStats() {
        List<SenderReputation> allReps = reputationRepository.findAll();
        List<DomainBlocklistEntry> allBlocklists = blocklistRepository.findAll();

        long totalDomains = allReps.size();
        long healthy = allReps.stream().filter(SenderReputation::isHealthy).count();
        long atRisk = allReps.stream().filter(SenderReputation::isAtRisk).count();
        long critical = allReps.stream().filter(r -> "F".equals(r.getHealthGrade())).count();

        long totalSent = allReps.stream().mapToLong(SenderReputation::getTotalSent).sum();
        long totalDelivered = allReps.stream().mapToLong(SenderReputation::getTotalDelivered).sum();
        long totalBounced = allReps.stream().mapToLong(r -> r.getHardBounces() + r.getSoftBounces()).sum();
        long totalComplaints = allReps.stream().mapToLong(SenderReputation::getSpamComplaints).sum();

        double sysDelivRate = totalSent > 0 ? round2((double) totalDelivered / totalSent * 100) : 100.0;
        double sysBounceRate = totalSent > 0 ? round2((double) totalBounced / totalSent * 100) : 0.0;
        double sysComplaintRate = totalSent > 0 ? round2((double) totalComplaints / totalSent * 100) : 0.0;

        Double avgScore = allReps.stream().mapToDouble(SenderReputation::getReputationScore).average().orElse(50.0);

        long blocklistCount = allBlocklists.stream().filter(DomainBlocklistEntry::getIsListed).count();

        Map<String, Long> gradeDist = allReps.stream()
                .collect(Collectors.groupingBy(SenderReputation::getHealthGrade, Collectors.counting()));
        Map<String, Long> riskDist = allReps.stream()
                .collect(Collectors.groupingBy(SenderReputation::getRiskLevel, Collectors.counting()));

        Map<String, Long> topBlocklists = allBlocklists.stream()
                .filter(DomainBlocklistEntry::getIsListed)
                .collect(Collectors.groupingBy(DomainBlocklistEntry::getBlocklistName, Collectors.counting()));

        return ReputationStats.builder()
                .totalDomainsTracked(totalDomains)
                .healthyDomains(healthy)
                .atRiskDomains(atRisk)
                .criticalDomains(critical)
                .totalEmailsTracked(totalSent)
                .totalDelivered(totalDelivered)
                .totalBounced(totalBounced)
                .totalSpamComplaints(totalComplaints)
                .systemWideDeliverabilityRate(sysDelivRate)
                .systemWideBounceRate(sysBounceRate)
                .systemWideSpamComplaintRate(sysComplaintRate)
                .avgReputationScore(Math.round(avgScore * 100.0) / 100.0)
                .totalBlocklistWarnings(blocklistCount)
                .healthGradeDistribution(gradeDist)
                .riskLevelDistribution(riskDist)
                .topBlocklists(topBlocklists)
                .build();
    }

    // ─── Helpers ───────────────────────────────────────────────────────

    private String extractDomain(String email) {
        if (email == null) return "unknown";
        int at = email.lastIndexOf('@');
        return at >= 0 ? email.substring(at + 1).toLowerCase().trim() : email.toLowerCase().trim();
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private String scoreToGrade(double score) {
        if (score >= 90) return "A";
        if (score >= 75) return "B";
        if (score >= 50) return "C";
        if (score >= 25) return "D";
        return "F";
    }

    private String scoreToRiskLevel(double score) {
        if (score >= 75) return "LOW";
        if (score >= 50) return "MEDIUM";
        if (score >= 25) return "HIGH";
        return "CRITICAL";
    }

    /**
     * Generate actionable recommendations based on reputation data.
     */
    private List<String> generateRecommendations(SenderReputation r) {
        List<String> recs = new ArrayList<>();

        if (r.getBounceRate() > 5.0) {
            recs.add("HIGH BOUNCE RATE (" + r.getBounceRate() + "%): Clean your mailing list. Remove invalid addresses and hard bounces immediately.");
        }
        if (r.getSpamComplaintRate() > 0.1) {
            recs.add("SPAM COMPLAINT RATE (" + r.getSpamComplaintRate() + "%) exceeds the 0.1% threshold. Review content quality, sending frequency, and opt-in process.");
        }
        if (r.getSpfPassRate() < 95.0) {
            recs.add("SPF PASS RATE (" + r.getSpfPassRate() + "%) is below 95%. Verify your DNS TXT records include all sending IPs.");
        }
        if (r.getDkimPassRate() < 95.0) {
            recs.add("DKIM PASS RATE (" + r.getDkimPassRate() + "%) is below 95%. Ensure DKIM keys are properly configured and not expired.");
        }
        if (r.getDmarcPassRate() < 95.0) {
            recs.add("DMARC PASS RATE (" + r.getDmarcPassRate() + "%) is below 95%. Review your DMARC policy and alignment settings.");
        }
        if (r.getAvgSpamScore() != null && r.getAvgSpamScore() > 3.0) {
            recs.add("AVERAGE SPAM SCORE (" + r.getAvgSpamScore() + ") is high. Review email content for spam triggers: excessive caps, spammy links, missing unsubscribe.");
        }
        long blocklistCount = blocklistRepository.countByDomainAndIsListedTrue(r.getDomain());
        if (blocklistCount > 0) {
            recs.add("DOMAIN IS ON " + blocklistCount + " BLOCKLIST(S): Take immediate action to request delisting and address the root cause.");
        }
        if (r.getDeliverabilityRate() < 95.0) {
            recs.add("DELIVERABILITY RATE (" + r.getDeliverabilityRate() + "%) is below 95%. Investigate deferrals and rejections with recipient ISPs.");
        }
        if (recs.isEmpty()) {
            recs.add("Domain health is good. Continue monitoring and maintain current practices.");
        }
        return recs;
    }
}
