package com.email.writer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for TemplateReview entity with approval workflow queries.
 */
@Repository
public interface TemplateReviewRepository extends JpaRepository<TemplateReview, Long> {

    /**
     * Find all reviews for a specific template version.
     */
    List<TemplateReview> findByTemplateVersionIdOrderByCreatedAtDesc(Long templateVersionId);

    /**
     * Find pending reviews for a reviewer.
     */
    List<TemplateReview> findByReviewerAndStatusOrderByCreatedAtDesc(String reviewer, String status);

    /**
     * Find all pending reviews across all reviewers.
     */
    List<TemplateReview> findByStatusOrderByCreatedAtDesc(String status);

    /**
     * Count reviews by status for a specific version.
     */
    long countByTemplateVersionIdAndStatus(Long templateVersionId, String status);

    /**
     * Find reviews for a template (across all versions).
     */
    List<TemplateReview> findByTemplateIdOrderByCreatedAtDesc(Long templateId);

    /**
     * Calculate average rating for a template version.
     */
    @Query("SELECT AVG(r.rating) FROM TemplateReview r WHERE r.templateVersionId = :versionId AND r.rating IS NOT NULL")
    Double averageRatingByVersionId(@Param("versionId") Long versionId);

    /**
     * Find the most recent review for a version.
     */
    List<TemplateReview> findTop1ByTemplateVersionIdOrderByCreatedAtDesc(Long templateVersionId);

    /**
     * Check if all reviews for a version are approved.
     */
    @Query("SELECT CASE WHEN COUNT(r) = 0 THEN true ELSE " +
           "(SELECT COUNT(r2) FROM TemplateReview r2 WHERE r2.templateVersionId = :versionId AND r2.status = 'APPROVED') = " +
           "COUNT(r) END FROM TemplateReview r WHERE r.templateVersionId = :versionId")
    boolean allReviewsApproved(@Param("versionId") Long versionId);

    /**
     * Count reviews by reviewer.
     */
    @Query("SELECT r.reviewer, COUNT(r) FROM TemplateReview r GROUP BY r.reviewer")
    List<Object[]> countByReviewer();

    /**
     * Find reviews that requested changes.
     */
    List<TemplateReview> findByStatusAndTemplateVersionIdOrderByCreatedAtDesc(String status, Long templateVersionId);
}
