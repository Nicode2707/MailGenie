package com.email.writer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EmailEventRepository extends JpaRepository<EmailEvent, Long> {

    List<EmailEvent> findByUserId(String userId);

    List<EmailEvent> findByEventType(String eventType);

    List<EmailEvent> findByRecipientEmail(String email);

    List<EmailEvent> findByCampaignId(Long campaignId);

    List<EmailEvent> findByUserIdAndEventType(String userId, String eventType);

    List<EmailEvent> findByEmailId(Long emailId);

    List<EmailEvent> findByEventTimestampBetween(LocalDateTime start, LocalDateTime end);

    List<EmailEvent> findByDeviceType(String deviceType);

    List<EmailEvent> findByGeoCountry(String country);

    @Query("SELECT e.eventType, COUNT(e) FROM EmailEvent e WHERE e.userId = :userId GROUP BY e.eventType")
    List<Object[]> countByEventTypeForUser(@Param("userId") String userId);

    @Query("SELECT e.deviceType, COUNT(e) FROM EmailEvent e WHERE e.userId = :userId AND e.eventType = 'OPENED' GROUP BY e.deviceType")
    List<Object[]> countOpensByDevice(@Param("userId") String userId);

    @Query("SELECT e.emailClient, COUNT(e) FROM EmailEvent e WHERE e.userId = :userId AND e.eventType = 'OPENED' GROUP BY e.emailClient")
    List<Object[]> countOpensByClient(@Param("userId") String userId);

    @Query("SELECT e.geoCountry, COUNT(e) FROM EmailEvent e WHERE e.userId = :userId AND e.eventType IN ('OPENED','CLICKED') GROUP BY e.geoCountry")
    List<Object[]> countEngagementByCountry(@Param("userId") String userId);

    @Query("SELECT e.deviceOs, COUNT(e) FROM EmailEvent e WHERE e.userId = :userId GROUP BY e.deviceOs")
    List<Object[]> countByOs(@Param("userId") String userId);

    @Query("SELECT AVG(e.timeToOpenSeconds) FROM EmailEvent e WHERE e.userId = :userId AND e.timeToOpenSeconds IS NOT NULL")
    Double avgTimeToOpen(@Param("userId") String userId);

    @Query("SELECT AVG(e.timeToClickSeconds) FROM EmailEvent e WHERE e.userId = :userId AND e.timeToClickSeconds IS NOT NULL")
    Double avgTimeToClick(@Param("userId") String userId);

    @Query("SELECT COUNT(e) FROM EmailEvent e WHERE e.userId = :userId AND e.eventType = 'SENT'")
    long countSent(@Param("userId") String userId);

    @Query("SELECT COUNT(e) FROM EmailEvent e WHERE e.userId = :userId AND e.eventType = 'DELIVERED'")
    long countDelivered(@Param("userId") String userId);

    @Query("SELECT COUNT(e) FROM EmailEvent e WHERE e.userId = :userId AND e.eventType = 'OPENED'")
    long countOpened(@Param("userId") String userId);

    @Query("SELECT COUNT(e) FROM EmailEvent e WHERE e.userId = :userId AND e.eventType = 'CLICKED'")
    long countClicked(@Param("userId") String userId);

    @Query("SELECT COUNT(e) FROM EmailEvent e WHERE e.userId = :userId AND e.eventType = 'BOUNCED'")
    long countBounced(@Param("userId") String userId);

    @Query("SELECT COUNT(e) FROM EmailEvent e WHERE e.userId = :userId AND e.eventType = 'COMPLAINED'")
    long countComplained(@Param("userId") String userId);

    @Query("SELECT COUNT(e) FROM EmailEvent e WHERE e.userId = :userId AND e.eventType = 'UNSUBSCRIBED'")
    long countUnsubscribed(@Param("userId") String userId);

    @Query("SELECT e.recipientEmail, COUNT(e) as opens FROM EmailEvent e WHERE e.userId = :userId AND e.eventType = 'OPENED' GROUP BY e.recipientEmail ORDER BY opens DESC")
    List<Object[]> topOpeners(@Param("userId") String userId);

    @Query("SELECT e.recipientEmail, COUNT(e) as clicks FROM EmailEvent e WHERE e.userId = :userId AND e.eventType = 'CLICKED' GROUP BY e.recipientEmail ORDER BY clicks DESC")
    List<Object[]> topClickers(@Param("userId") String userId);

    @Query("SELECT DATE(e.eventTimestamp), COUNT(e) FROM EmailEvent e WHERE e.userId = :userId AND e.eventTimestamp >= :since GROUP BY DATE(e.eventTimestamp) ORDER BY DATE(e.eventTimestamp)")
    List<Object[]> dailyEventTrend(@Param("userId") String userId, @Param("since") LocalDateTime since);
}
