package com.email.writer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for ScheduledEmail persistence operations.
 */
@Repository
public interface ScheduledEmailRepository extends JpaRepository<ScheduledEmail, Long> {

    /**
     * Find all scheduled emails that are pending and due for processing.
     */
    List<ScheduledEmail> findByStatusAndScheduledAtLessThanEqual(String status, LocalDateTime now);

    /**
     * Find all scheduled emails for a given status, ordered by scheduled time ascending.
     */
    List<ScheduledEmail> findByStatusOrderByScheduledAtAsc(String status);

    /**
     * Find all scheduled emails with a specific label.
     */
    List<ScheduledEmail> findByLabelOrderByScheduledAtDesc(String label);

    /**
     * Find all non-cancelled, non-completed emails that are past their scheduled time
     * and have remaining retry attempts.
     */
    @Query("SELECT se FROM ScheduledEmail se WHERE se.status IN ('PENDING', 'PROCESSING') " +
            "AND se.scheduledAt <= :now AND se.attempts < se.maxAttempts " +
            "ORDER BY se.scheduledAt ASC")
    List<ScheduledEmail> findDueForProcessing(@Param("now") LocalDateTime now);

    /**
     * Find all completed scheduled emails.
     */
    List<ScheduledEmail> findByStatusOrderByUpdatedAtDesc(String status);

    /**
     * Count scheduled emails grouped by status.
     */
    @Query("SELECT se.status, COUNT(se) FROM ScheduledEmail se GROUP BY se.status")
    List<Object[]> countByStatus();

    /**
     * Find scheduled emails that were created within a date range.
     */
    List<ScheduledEmail> findByCreatedAtBetweenOrderByCreatedAtDesc(
            LocalDateTime start, LocalDateTime end);

    /**
     * Bulk cancel all pending emails scheduled before a certain time.
     */
    @Modifying
    @Query("UPDATE ScheduledEmail se SET se.status = 'CANCELLED', se.updatedAt = :now " +
            "WHERE se.status = 'PENDING' AND se.scheduledAt < :deadline")
    int bulkCancelPending(@Param("deadline") LocalDateTime deadline, @Param("now") LocalDateTime now);

    /**
     * Delete all completed emails older than the given threshold.
     */
    @Modifying
    @Query("DELETE FROM ScheduledEmail se WHERE se.status = 'COMPLETED' AND se.updatedAt < :threshold")
    int purgeCompletedBefore(@Param("threshold") LocalDateTime threshold);

    /**
     * Find the next N pending emails ordered by scheduled time.
     */
    @Query(value = "SELECT * FROM scheduled_emails WHERE status = 'PENDING' " +
            "ORDER BY scheduled_at ASC LIMIT :limit", nativeQuery = true)
    List<ScheduledEmail> findNextPending(@Param("limit") int limit);
}
