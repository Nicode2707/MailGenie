package com.email.writer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for DraftVersion persistence and queries.
 */
@Repository
public interface DraftVersionRepository extends JpaRepository<DraftVersion, Long> {

    /**
     * Find all versions for a session, ordered by version number ascending.
     */
    List<DraftVersion> findBySessionIdOrderByVersionNumberAsc(String sessionId);

    /**
     * Find all versions for a session, ordered by version number descending.
     */
    List<DraftVersion> findBySessionIdOrderByVersionNumberDesc(String sessionId);

    /**
     * Find the currently active version for a session.
     */
    Optional<DraftVersion> findBySessionIdAndActiveTrue(String sessionId);

    /**
     * Find a specific version within a session.
     */
    Optional<DraftVersion> findBySessionIdAndVersionNumber(String sessionId, int versionNumber);

    /**
     * Get the next version number for a session.
     */
    @Query("SELECT COALESCE(MAX(d.versionNumber), 0) FROM DraftVersion d WHERE d.sessionId = :sessionId")
    int getMaxVersionNumber(@Param("sessionId") String sessionId);

    /**
     * Deactivate all versions in a session (before activating a new one).
     */
    @Modifying
    @Query("UPDATE DraftVersion d SET d.active = false WHERE d.sessionId = :sessionId")
    int deactivateAllInSession(@Param("sessionId") String sessionId);

    /**
     * Find all unique session IDs, most recent first.
     */
    @Query("SELECT DISTINCT d.sessionId FROM DraftVersion d ORDER BY d.createdAt DESC")
    List<String> findAllSessionIds();

    /**
     * Find sessions created within a date range.
     */
    @Query("SELECT DISTINCT d.sessionId FROM DraftVersion d WHERE d.createdAt BETWEEN :start AND :end ORDER BY d.createdAt DESC")
    List<String> findSessionIdsInRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Count total versions across all sessions.
     */
    long count();

    /**
     * Count versions in a specific session.
     */
    long countBySessionId(String sessionId);

    /**
     * Find all versions with a specific source type.
     */
    List<DraftVersion> findBySourceOrderByCreatedAtDesc(String source);

    /**
     * Delete all versions in a session.
     */
    @Modifying
    void deleteBySessionId(String sessionId);

    /**
     * Find sessions with the most versions (popular sessions).
     */
    @Query("SELECT d.sessionId, COUNT(d) as cnt FROM DraftVersion d GROUP BY d.sessionId ORDER BY cnt DESC")
    List<Object[]> findMostActiveSessions();

    /**
     * Purge draft versions older than the given threshold.
     */
    @Modifying
    @Query("DELETE FROM DraftVersion d WHERE d.createdAt < :threshold")
    int purgeOlderThan(@Param("threshold") LocalDateTime threshold);
}
