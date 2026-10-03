package com.email.writer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for WebhookConfig persistence.
 */
@Repository
public interface WebhookConfigRepository extends JpaRepository<WebhookConfig, Long> {

    /**
     * Find all active webhooks.
     */
    List<WebhookConfig> findByActiveTrue();

    /**
     * Find all active webhooks that subscribe to a specific event type.
     */
    @Query("SELECT w FROM WebhookConfig w WHERE w.active = true " +
            "AND (w.eventType = '*' OR w.eventType LIKE %:eventType%)")
    List<WebhookConfig> findActiveByEventType(@Param("eventType") String eventType);

    /**
     * Find webhooks by name (case-insensitive partial match).
     */
    List<WebhookConfig> findByNameContainingIgnoreCase(String name);

    /**
     * Deactivate all webhooks.
     */
    @Modifying
    @Query("UPDATE WebhookConfig w SET w.active = false, w.updatedAt = :now")
    int deactivateAll(@Param("now") LocalDateTime now);

    /**
     * Find webhooks with high failure rates (failure rate > threshold).
     */
    @Query("SELECT w FROM WebhookConfig w WHERE w.active = true " +
            "AND (w.successCount + w.failureCount) > 0 " +
            "AND (CAST(w.failureCount AS double) / (w.successCount + w.failureCount)) > :threshold")
    List<WebhookConfig> findWithHighFailureRate(@Param("threshold") double threshold);
}
