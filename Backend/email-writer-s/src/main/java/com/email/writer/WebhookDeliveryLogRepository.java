package com.email.writer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for WebhookDeliveryLog persistence and queries.
 */
@Repository
public interface WebhookDeliveryLogRepository extends JpaRepository<WebhookDeliveryLog, Long> {

    /**
     * Find all logs for a specific webhook config, most recent first.
     */
    List<WebhookDeliveryLog> findByWebhookConfigIdOrderByCreatedAtDesc(Long webhookConfigId);

    /**
     * Find all logs with a specific delivery status.
     */
    List<WebhookDeliveryLog> findByStatusOrderByCreatedAtDesc(String status);

    /**
     * Find logs for a specific event type.
     */
    List<WebhookDeliveryLog> findByEventTypeOrderByCreatedAtDesc(String eventType);

    /**
     * Find logs within a date range, most recent first.
     */
    List<WebhookDeliveryLog> findByCreatedAtBetweenOrderByCreatedAtDesc(
            LocalDateTime start, LocalDateTime end);

    /**
     * Count deliveries grouped by status.
     */
    @Query("SELECT dl.status, COUNT(dl) FROM WebhookDeliveryLog dl GROUP BY dl.status")
    List<Object[]> countByStatus();

    /**
     * Count deliveries for a specific webhook config.
     */
    long countByWebhookConfigId(Long webhookConfigId);

    /**
     * Find the most recent delivery log for a webhook config.
     */
    WebhookDeliveryLog findFirstByWebhookConfigIdOrderByCreatedAtDesc(Long webhookConfigId);

    /**
     * Find delivery logs that are retrying and have been waiting long enough.
     */
    @Query("SELECT dl FROM WebhookDeliveryLog dl WHERE dl.status = 'RETRYING' " +
            "AND dl.updatedAt <= :retryAfter ORDER BY dl.updatedAt ASC")
    List<WebhookDeliveryLog> findRetryable(@Param("retryAfter") LocalDateTime retryAfter);

    /**
     * Purge delivery logs older than the given threshold.
     */
    @Modifying
    @Query("DELETE FROM WebhookDeliveryLog dl WHERE dl.createdAt < :threshold")
    int purgeOlderThan(@Param("threshold") LocalDateTime threshold);

    /**
     * Get delivery statistics for a webhook config: total, success, failure counts.
     */
    @Query("SELECT dl.status, COUNT(dl) FROM WebhookDeliveryLog dl " +
            "WHERE dl.webhookConfigId = :webhookId GROUP BY dl.status")
    List<Object[]> statsByWebhookId(@Param("webhookId") Long webhookId);

    /**
     * Find the N most recent failed deliveries across all webhooks.
     */
    @Query(value = "SELECT * FROM webhook_delivery_logs " +
            "WHERE status IN ('FAILED', 'EXHAUSTED') " +
            "ORDER BY created_at DESC LIMIT :limit", nativeQuery = true)
    List<WebhookDeliveryLog> findRecentFailures(@Param("limit") int limit);
}
