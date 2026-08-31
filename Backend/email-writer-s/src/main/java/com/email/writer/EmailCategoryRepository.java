package com.email.writer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for EmailCategory persistence and analytics queries.
 */
@Repository
public interface EmailCategoryRepository extends JpaRepository<EmailCategory, Long> {

    /**
     * Find all records for a specific category.
     */
    List<EmailCategory> findByCategoryOrderByCreatedAtDesc(String category);

    /**
     * Find all records with a specific sentiment.
     */
    List<EmailCategory> findBySentimentOrderByCreatedAtDesc(String sentiment);

    /**
     * Find all records with a specific urgency level.
     */
    List<EmailCategory> findByUrgencyOrderByCreatedAtDesc(String urgency);

    /**
     * Count records grouped by category.
     */
    @Query("SELECT e.category, COUNT(e) FROM EmailCategory e GROUP BY e.category ORDER BY COUNT(e) DESC")
    List<Object[]> countByCategory();

    /**
     * Count records grouped by sentiment.
     */
    @Query("SELECT e.sentiment, COUNT(e) FROM EmailCategory e GROUP BY e.sentiment")
    List<Object[]> countBySentiment();

    /**
     * Count records grouped by urgency.
     */
    @Query("SELECT e.urgency, COUNT(e) FROM EmailCategory e GROUP BY e.urgency")
    List<Object[]> countByUrgency();

    /**
     * Average confidence score across all records.
     */
    @Query("SELECT AVG(e.confidence) FROM EmailCategory e")
    Double averageConfidence();

    /**
     * Count emails with high confidence (>= 0.7).
     */
    @Query("SELECT COUNT(e) FROM EmailCategory e WHERE e.confidence >= 0.7")
    long countHighConfidence();

    /**
     * Count emails that contain questions.
     */
    long countByContainsQuestionsTrue();

    /**
     * Count emails that mention deadlines.
     */
    long countByMentionsDeadlineTrue();

    /**
     * Count emails that are actionable.
     */
    @Query("SELECT COUNT(e) FROM EmailCategory e WHERE e.category IN ('INQUIRY','REQUEST','MEETING','FOLLOW_UP')")
    long countActionable();

    /**
     * Find records within a date range.
     */
    List<EmailCategory> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime start, LocalDateTime end);

    /**
     * Find the N most recent records.
     */
    List<EmailCategory> findTop10ByOrderByCreatedAtDesc();

    /**
     * Find records by category with minimum confidence.
     */
    @Query("SELECT e FROM EmailCategory e WHERE e.category = :category AND e.confidence >= :minConfidence ORDER BY e.confidence DESC")
    List<EmailCategory> findByCategoryWithMinConfidence(@Param("category") String category, @Param("minConfidence") double minConfidence);

    /**
     * Search by tag (comma-separated field contains the tag).
     */
    @Query("SELECT e FROM EmailCategory e WHERE LOWER(e.tags) LIKE LOWER(CONCAT('%', :tag, '%')) ORDER BY e.createdAt DESC")
    List<EmailCategory> findByTag(@Param("tag") String tag);

    /**
     * Purge old categorization records.
     */
    @Modifying
    @Query("DELETE FROM EmailCategory e WHERE e.createdAt < :threshold")
    int purgeOlderThan(@Param("threshold") LocalDateTime threshold);

    /**
     * Find records where user label is set.
     */
    List<EmailCategory> findByUserLabelIsNotNullOrderByCreatedAtDesc();

    /**
     * Find records by detected language.
     */
    List<EmailCategory> findByDetectedLanguageOrderByCreatedAtDesc(String language);
}
