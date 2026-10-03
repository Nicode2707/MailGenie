package com.email.writer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for EmailHistory search and analytics queries.
 */
@Repository
public interface EmailHistorySearchRepository extends JpaRepository<EmailHistory, Long> {

    /**
     * Full-text search across originalContent and generatedReply.
     */
    @Query("SELECT h FROM EmailHistory h WHERE " +
            "LOWER(h.originalContent) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(h.generatedReply) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<EmailHistory> searchByQuery(@Param("query") String query, Pageable pageable);

    /**
     * Filter by tone.
     */
    Page<EmailHistory> findByToneIgnoreCase(String tone, Pageable pageable);

    /**
     * Filter by provider.
     */
    Page<EmailHistory> findByProviderIgnoreCase(String provider, Pageable pageable);

    /**
     * Filter by language.
     */
    Page<EmailHistory> findByLanguageIgnoreCase(String language, Pageable pageable);

    /**
     * Search with multiple optional filters combined.
     */
    @Query("SELECT h FROM EmailHistory h WHERE " +
            "(:query IS NULL OR LOWER(h.originalContent) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "   OR LOWER(h.generatedReply) LIKE LOWER(CONCAT('%', :query, '%'))) " +
            "AND (:tone IS NULL OR LOWER(h.tone) = LOWER(:tone)) " +
            "AND (:provider IS NULL OR LOWER(h.provider) = LOWER(:provider)) " +
            "AND (:language IS NULL OR LOWER(h.language) = LOWER(:language)) " +
            "AND (:createdAfter IS NULL OR h.createdAt >= :createdAfter) " +
            "AND (:createdBefore IS NULL OR h.createdAt <= :createdBefore)")
    Page<EmailHistory> searchWithFilters(
            @Param("query") String query,
            @Param("tone") String tone,
            @Param("provider") String provider,
            @Param("language") String language,
            @Param("createdAfter") LocalDateTime createdAfter,
            @Param("createdBefore") LocalDateTime createdBefore,
            Pageable pageable);

    /**
     * Count records grouped by tone.
     */
    @Query("SELECT h.tone, COUNT(h) FROM EmailHistory h WHERE h.tone IS NOT NULL GROUP BY h.tone")
    List<Object[]> countByTone();

    /**
     * Count records grouped by provider.
     */
    @Query("SELECT h.provider, COUNT(h) FROM EmailHistory h WHERE h.provider IS NOT NULL GROUP BY h.provider")
    List<Object[]> countByProvider();

    /**
     * Count records grouped by language.
     */
    @Query("SELECT h.language, COUNT(h) FROM EmailHistory h WHERE h.language IS NOT NULL GROUP BY h.language")
    List<Object[]> countByLanguage();

    /**
     * Count records grouped by creation date (date only, no time).
     */
    @Query("SELECT FUNCTION('FORMATDATETIME', h.createdAt, 'yyyy-MM-dd'), COUNT(h) " +
            "FROM EmailHistory h GROUP BY FUNCTION('FORMATDATETIME', h.createdAt, 'yyyy-MM-dd') " +
            "ORDER BY FUNCTION('FORMATDATETIME', h.createdAt, 'yyyy-MM-dd') DESC")
    List<Object[]> countByDay();

    /**
     * Count records with non-null user comments.
     */
    @Query("SELECT COUNT(h) FROM EmailHistory h WHERE h.userComment IS NOT NULL AND h.userComment <> ''")
    long countWithComments();

    /**
     * Average length of generatedReply.
     */
    @Query("SELECT AVG(LENGTH(h.generatedReply)) FROM EmailHistory h WHERE h.generatedReply IS NOT NULL")
    Double averageReplyLength();

    /**
     * Average length of originalContent.
     */
    @Query("SELECT AVG(LENGTH(h.originalContent)) FROM EmailHistory h WHERE h.originalContent IS NOT NULL")
    Double averageOriginalLength();

    /**
     * Find all records for CSV export (no pagination).
     */
    @Query("SELECT h FROM EmailHistory h ORDER BY h.createdAt DESC")
    List<EmailHistory> findAllForExport();
}
