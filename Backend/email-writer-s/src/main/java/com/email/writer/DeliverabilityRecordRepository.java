package com.email.writer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for DeliverabilityRecord entity with analytics queries.
 */
@Repository
public interface DeliverabilityRecordRepository extends JpaRepository<DeliverabilityRecord, Long> {

    List<DeliverabilityRecord> findBySenderDomainOrderBySentAtDesc(String domain);

    List<DeliverabilityRecord> findByRecipientDomainOrderBySentAtDesc(String domain);

    long countBySenderDomainAndEventType(String domain, String eventType);

    long countBySenderDomain(String domain);

    @Query("SELECT r.eventType, COUNT(r) FROM DeliverabilityRecord r WHERE r.senderDomain = :domain GROUP BY r.eventType")
    List<Object[]> countByEventTypeForDomain(@Param("domain") String domain);

    @Query("SELECT r.recipientDomain, COUNT(r) FROM DeliverabilityRecord r WHERE r.senderDomain = :domain GROUP BY r.recipientDomain")
    List<Object[]> countByRecipientDomain(@Param("domain") String domain);

    @Query("SELECT r.bounceType, COUNT(r) FROM DeliverabilityRecord r WHERE r.senderDomain = :domain AND r.eventType = 'BOUNCED' GROUP BY r.bounceType")
    List<Object[]> bounceBreakdownByDomain(@Param("domain") String domain);

    @Query("SELECT AVG(r.spamScore) FROM DeliverabilityRecord r WHERE r.senderDomain = :domain AND r.spamScore IS NOT NULL")
    Double averageSpamScoreByDomain(@Param("domain") String domain);

    @Query("SELECT COUNT(r) FROM DeliverabilityRecord r WHERE r.senderDomain = :domain AND r.spfPass = true")
    long spfPassCount(@Param("domain") String domain);

    @Query("SELECT COUNT(r) FROM DeliverabilityRecord r WHERE r.senderDomain = :domain AND r.dkimPass = true")
    long dkimPassCount(@Param("domain") String domain);

    @Query("SELECT COUNT(r) FROM DeliverabilityRecord r WHERE r.senderDomain = :domain AND r.dmarcPass = true")
    long dmarcPassCount(@Param("domain") String domain);

    long countBySenderDomainAndEventTypeAndSentAtAfter(String domain, String eventType, LocalDateTime since);

    @Query("SELECT FUNCTION('DATE', r.sentAt), r.eventType, COUNT(r) FROM DeliverabilityRecord r WHERE r.senderDomain = :domain GROUP BY FUNCTION('DATE', r.sentAt), r.eventType ORDER BY FUNCTION('DATE', r.sentAt)")
    List<Object[]> dailyEventTrend(@Param("domain") String domain);

    List<DeliverabilityRecord> findBySenderDomainAndSentAtBetweenOrderBySentAtDesc(
            String domain, LocalDateTime start, LocalDateTime end);

    @Query("SELECT r.sendingIp, COUNT(r) FROM DeliverabilityRecord r WHERE r.senderDomain = :domain AND r.sendingIp IS NOT NULL GROUP BY r.sendingIp")
    List<Object[]> countBySendingIp(@Param("domain") String domain);

    @Query("SELECT COUNT(r) FROM DeliverabilityRecord r WHERE r.senderDomain = :domain AND r.eventType IN ('BOUNCED','SPAM_COMPLAINT','REJECTED')")
    long totalNegativeEvents(@Param("domain") String domain);

    @Query("SELECT r.smtpCode, COUNT(r) FROM DeliverabilityRecord r WHERE r.senderDomain = :domain AND r.smtpCode IS NOT NULL GROUP BY r.smtpCode ORDER BY COUNT(r) DESC")
    List<Object[]> smtpCodeDistribution(@Param("domain") String domain);
}
