package com.email.writer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for TemplateVersion entity with versioning queries.
 */
@Repository
public interface TemplateVersionRepository extends JpaRepository<TemplateVersion, Long> {

    /**
     * Find all versions for a template, ordered by version number descending.
     */
    List<TemplateVersion> findByTemplateIdOrderByVersionNumberDesc(Long templateId);

    /**
     * Find the latest version number for a template.
     */
    @Query("SELECT MAX(v.versionNumber) FROM TemplateVersion v WHERE v.templateId = :templateId")
    Optional<Integer> findLatestVersionNumber(@Param("templateId") Long templateId);

    /**
     * Find the current (published) version for a template.
     */
    Optional<TemplateVersion> findByTemplateIdAndIsCurrentTrue(Long templateId);

    /**
     * Find all versions with a given status.
     */
    List<TemplateVersion> findByStatusOrderByCreatedAtDesc(String status);

    /**
     * Find versions pending review.
     */
    List<TemplateVersion> findByStatusInOrderByCreatedAtDesc(List<String> statuses);

    /**
     * Find versions by author.
     */
    List<TemplateVersion> findByAuthorOrderByCreatedAtDesc(String author);

    /**
     * Find a specific version by template ID and version number.
     */
    Optional<TemplateVersion> findByTemplateIdAndVersionNumber(Long templateId, Integer versionNumber);

    /**
     * Find all versions that are locked.
     */
    @Query("SELECT v FROM TemplateVersion v WHERE v.lockToken IS NOT NULL AND v.lockExpiresAt > CURRENT_TIMESTAMP")
    List<TemplateVersion> findLockedVersions();

    /**
     * Find all current versions across all templates.
     */
    List<TemplateVersion> findByIsCurrentTrue();

    /**
     * Count versions for a given template.
     */
    long countByTemplateId(Long templateId);

    /**
     * Find versions by tag.
     */
    @Query("SELECT v FROM TemplateVersion v WHERE v.tags LIKE CONCAT('%', :tag, '%') ORDER BY v.createdAt DESC")
    List<TemplateVersion> findByTag(@Param("tag") String tag);
}
