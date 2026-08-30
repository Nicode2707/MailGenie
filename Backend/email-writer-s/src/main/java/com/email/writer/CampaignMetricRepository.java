package com.email.writer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for CampaignMetric entity with analytics queries.
 */
@Repository
public interface CampaignMetricRepository extends JpaRepository<CampaignMetric, Long> {

    List<CampaignMetric> findByCampaignIdAndVariantIdOrderByRecordedAtDesc(Long campaignId, Long variantId);

    long countByCampaignIdAndEventType(Long campaignId, String eventType);

    long countByVariantIdAndEventType(Long variantId, String eventType);

    @Query("SELECT m.eventType, COUNT(m) FROM CampaignMetric m WHERE m.campaignId = :campaignId GROUP BY m.eventType")
    List<Object[]> countByEventTypeForCampaign(@Param("campaignId") Long campaignId);

    @Query("SELECT m.eventType, COUNT(m) FROM CampaignMetric m WHERE m.variantId = :variantId GROUP BY m.eventType")
    List<Object[]> countByEventTypeForVariant(@Param("variantId") Long variantId);

    @Query("SELECT m.deviceType, COUNT(m) FROM CampaignMetric m WHERE m.campaignId = :campaignId AND m.deviceType IS NOT NULL GROUP BY m.deviceType")
    List<Object[]> countByDeviceType(@Param("campaignId") Long campaignId);

    @Query("SELECT AVG(m.timeToEventMs) FROM CampaignMetric m WHERE m.variantId = :variantId AND m.timeToEventMs IS NOT NULL AND m.eventType = :eventType")
    Double averageTimeToEvent(@Param("variantId") Long variantId, @Param("eventType") String eventType);

    List<CampaignMetric> findByRecordedAtBetweenOrderByRecordedAtDesc(LocalDateTime start, LocalDateTime end);

    @Query("SELECT COALESCE(SUM(m.revenue), 0.0) FROM CampaignMetric m WHERE m.campaignId = :campaignId")
    Double totalRevenueByCampaign(@Param("campaignId") Long campaignId);

    @Query("SELECT COALESCE(SUM(m.revenue), 0.0) FROM CampaignMetric m WHERE m.variantId = :variantId")
    Double totalRevenueByVariant(@Param("variantId") Long variantId);

    @Query("SELECT m.region, COUNT(m) FROM CampaignMetric m WHERE m.campaignId = :campaignId AND m.region IS NOT NULL GROUP BY m.region")
    List<Object[]> countByRegion(@Param("campaignId") Long campaignId);

    @Query("SELECT FUNCTION('DATE', m.recordedAt), m.eventType, COUNT(m) FROM CampaignMetric m " +
           "WHERE m.campaignId = :campaignId GROUP BY FUNCTION('DATE', m.recordedAt), m.eventType ORDER BY FUNCTION('DATE', m.recordedAt)")
    List<Object[]> dailyEventCounts(@Param("campaignId") Long campaignId);
}
