package com.email.writer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Enterprise service for email template versioning, collaboration, and approval workflows.
 * Supports version creation with automatic diff calculation, optimistic locking,
 * multi-reviewer approval, rollback to previous versions, and analytics.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TemplateVersioningService {

    private final TemplateVersionRepository versionRepository;
    private final TemplateReviewRepository reviewRepository;
    private final EmailTemplateRepository templateRepository;

    // ─── Version Creation ──────────────────────────────────────────────

    /**
     * Create a new version of an existing email template.
     * Automatically calculates diff against the previous version.
     */
    @Transactional
    public TemplateVersion createVersion(TemplateVersionCreateRequest request) {
        if (request.getTemplateId() == null) {
            throw new IllegalArgumentException("Template ID is required.");
        }
        if (request.getBody() == null || request.getBody().trim().isEmpty()) {
            throw new IllegalArgumentException("Template body is required.");
        }

        // Get the latest version number
        Optional<Integer> latestVersion = versionRepository.findLatestVersionNumber(request.getTemplateId());
        int nextVersion = latestVersion.map(v -> v + 1).orElse(1);

        // Get previous version for diff calculation
        TemplateVersion previousVersion = null;
        if (nextVersion > 1) {
            previousVersion = versionRepository.findByTemplateIdAndVersionNumber(
                    request.getTemplateId(), nextVersion - 1).orElse(null);
        }

        // Calculate diff
        TemplateDiffResult diff = null;
        int linesAdded = 0, linesRemoved = 0;
        if (previousVersion != null) {
            // Build a temporary version for diff calculation
            TemplateVersion tempCurrent = TemplateVersion.builder()
                    .body(request.getBody())
                    .build();
            diff = tempCurrent.computeDiff(previousVersion);
            linesAdded = diff.getAddedLines();
            linesRemoved = diff.getRemovedLines();
        } else {
            linesAdded = request.getBody().split("\n").length;
        }

        TemplateVersion version = TemplateVersion.builder()
                .templateId(request.getTemplateId())
                .versionNumber(nextVersion)
                .title(request.getTitle() != null ? request.getTitle().trim() : "Untitled")
                .body(request.getBody().trim())
                .changeDescription(request.getChangeDescription())
                .author(request.getAuthor() != null ? request.getAuthor().trim() : "anonymous")
                .status("DRAFT")
                .isCurrent(false)
                .linesAdded(linesAdded)
                .linesRemoved(linesRemoved)
                .characterCount(request.getBody().trim().length())
                .tags(request.getTags() != null ? String.join(",", request.getTags()) : null)
                .build();

        TemplateVersion saved = versionRepository.save(version);
        log.info("Created version {} for template {} (added={}, removed={})",
                nextVersion, request.getTemplateId(), linesAdded, linesRemoved);
        return saved;
    }

    /**
     * Create a new version directly from the current live template body.
     */
    @Transactional
    public TemplateVersion createVersionFromTemplate(Long templateId, String changeDescription, String author) {
        EmailTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new NoSuchElementException("Template not found: " + templateId));

        TemplateVersionCreateRequest request = new TemplateVersionCreateRequest();
        request.setTemplateId(templateId);
        request.setTitle(template.getTitle());
        request.setBody(template.getBody());
        request.setChangeDescription(changeDescription);
        request.setAuthor(author);
        return createVersion(request);
    }

    // ─── Version Management ────────────────────────────────────────────

    /**
     * Get all versions for a template.
     */
    public List<TemplateVersion> getVersions(Long templateId) {
        return versionRepository.findByTemplateIdOrderByVersionNumberDesc(templateId);
    }

    /**
     * Get a specific version by template ID and version number.
     */
    public TemplateVersion getVersion(Long templateId, Integer versionNumber) {
        return versionRepository.findByTemplateIdAndVersionNumber(templateId, versionNumber)
                .orElseThrow(() -> new NoSuchElementException(
                        "Version " + versionNumber + " not found for template " + templateId));
    }

    /**
     * Get the current (published) version of a template.
     */
    public TemplateVersion getCurrentVersion(Long templateId) {
        return versionRepository.findByTemplateIdAndIsCurrentTrue(templateId)
                .orElseThrow(() -> new NoSuchElementException(
                        "No published version found for template " + templateId));
    }

    /**
     * Calculate diff between two versions of a template.
     */
    public TemplateDiffResult diffVersions(Long templateId, Integer fromVersion, Integer toVersion) {
        TemplateVersion from = versionRepository.findByTemplateIdAndVersionNumber(templateId, fromVersion)
                .orElseThrow(() -> new NoSuchElementException("Version " + fromVersion + " not found."));
        TemplateVersion to = versionRepository.findByTemplateIdAndVersionNumber(templateId, toVersion)
                .orElseThrow(() -> new NoSuchElementException("Version " + toVersion + " not found."));

        TemplateDiffResult diff = to.computeDiff(from);
        diff.setPreviousVersionSummary("v" + fromVersion + " by " + from.getAuthor() + " (" + from.getStatus() + ")");
        diff.setCurrentVersionSummary("v" + toVersion + " by " + to.getAuthor() + " (" + to.getStatus() + ")");
        return diff;
    }

    // ─── Publishing & Rollback ─────────────────────────────────────────

    /**
     * Publish a version — mark it as the current active version.
     * Requires the version to be APPROVED or have no pending reviews.
     */
    @Transactional
    public TemplateVersion publishVersion(Long templateId, Integer versionNumber) {
        TemplateVersion version = getVersion(templateId, versionNumber);

        // Unpublish the current version
        versionRepository.findByTemplateIdAndIsCurrentTrue(templateId).ifPresent(current -> {
            current.setIsCurrent(false);
            versionRepository.save(current);
        });

        version.setIsCurrent(true);
        version.setStatus("PUBLISHED");
        TemplateVersion saved = versionRepository.save(version);

        // Sync back to the EmailTemplate entity
        syncTemplateFromVersion(templateId, version);

        log.info("Published version {} for template {}", versionNumber, templateId);
        return saved;
    }

    /**
     * Rollback to a previous version — creates a new version with the old content.
     */
    @Transactional
    public TemplateVersion rollbackToVersion(Long templateId, Integer targetVersion, String author) {
        TemplateVersion target = getVersion(templateId, targetVersion);

        TemplateVersionCreateRequest request = new TemplateVersionCreateRequest();
        request.setTemplateId(templateId);
        request.setTitle(target.getTitle());
        request.setBody(target.getBody());
        request.setChangeDescription("Rollback to version " + targetVersion);
        request.setAuthor(author);
        request.setTags(List.of("rollback", "v" + targetVersion));

        TemplateVersion rolled = createVersion(request);
        rolled.setStatus("APPROVED");
        versionRepository.save(rolled);

        log.info("Rolled back template {} to version {} (new version created)", templateId, targetVersion);
        return rolled;
    }

    // ─── Locking ───────────────────────────────────────────────────────

    /**
     * Acquire an editing lock on a version (optimistic locking).
     */
    @Transactional
    public boolean acquireLock(Long versionId, String lockToken, int lockDurationMinutes) {
        TemplateVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new NoSuchElementException("Version not found: " + versionId));

        if (version.isLocked()) {
            return false; // Already locked
        }

        version.setLockToken(lockToken);
        version.setLockExpiresAt(LocalDateTime.now().plusMinutes(lockDurationMinutes));
        versionRepository.save(version);
        log.info("Acquired lock on version {} until {}", versionId, version.getLockExpiresAt());
        return true;
    }

    /**
     * Release an editing lock.
     */
    @Transactional
    public boolean releaseLock(Long versionId, String lockToken) {
        TemplateVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new NoSuchElementException("Version not found: " + versionId));

        if (version.getLockToken() != null && version.getLockToken().equals(lockToken)) {
            version.setLockToken(null);
            version.setLockExpiresAt(null);
            versionRepository.save(version);
            return true;
        }
        return false;
    }

    // ─── Review Workflow ───────────────────────────────────────────────

    /**
     * Submit a review for a template version.
     */
    @Transactional
    public TemplateReview submitReview(Long versionId, TemplateReviewRequest request) {
        TemplateVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new NoSuchElementException("Version not found: " + versionId));

        if (request.getReviewer() == null || request.getReviewer().trim().isEmpty()) {
            throw new IllegalArgumentException("Reviewer name is required.");
        }
        if (request.getStatus() == null) {
            throw new IllegalArgumentException("Review status is required.");
        }

        TemplateReview review = TemplateReview.builder()
                .templateVersionId(versionId)
                .templateId(version.getTemplateId())
                .reviewer(request.getReviewer().trim())
                .status(request.getStatus().toUpperCase())
                .rating(request.getRating())
                .comments(request.getComments())
                .suggestions(request.getSuggestions())
                .resolvedAllFeedback(Boolean.TRUE.equals(request.getResolvedAllFeedback()))
                .reviewDurationSeconds(request.getReviewDurationSeconds())
                .build();

        TemplateReview saved = reviewRepository.save(review);

        // Update version with review metadata
        version.setReviewedBy(request.getReviewer());
        version.setReviewComments(request.getComments());
        version.setReviewedAt(LocalDateTime.now());

        // Auto-approve version if all reviews are approved
        if ("APPROVED".equalsIgnoreCase(request.getStatus())) {
            long totalReviews = reviewRepository.countByTemplateVersionIdAndStatus(versionId, "APPROVED")
                    + reviewRepository.countByTemplateVersionIdAndStatus(versionId, "REJECTED")
                    + reviewRepository.countByTemplateVersionIdAndStatus(versionId, "CHANGES_REQUESTED")
                    + 1; // +1 for the one just saved

            long approvedCount = reviewRepository.countByTemplateVersionIdAndStatus(versionId, "APPROVED") + 1;

            if (approvedCount == totalReviews && totalReviews > 0) {
                version.setStatus("APPROVED");
                log.info("Version {} auto-approved (all {} reviews approved)", versionId, totalReviews);
            }
        } else if ("REJECTED".equalsIgnoreCase(request.getStatus())) {
            version.setStatus("REJECTED");
        } else if ("CHANGES_REQUESTED".equalsIgnoreCase(request.getStatus())) {
            version.setStatus("PENDING_REVIEW");
        }

        versionRepository.save(version);
        log.info("Review submitted for version {} by {}: {}", versionId, request.getReviewer(), request.getStatus());
        return saved;
    }

    /**
     * Get all reviews for a version.
     */
    public List<TemplateReview> getReviews(Long versionId) {
        return reviewRepository.findByTemplateVersionIdOrderByCreatedAtDesc(versionId);
    }

    /**
     * Get pending reviews for a reviewer.
     */
    public List<TemplateReview> getPendingReviews(String reviewer) {
        return reviewRepository.findByReviewerAndStatusOrderByCreatedAtDesc(reviewer, "PENDING");
    }

    /**
     * Get all pending reviews.
     */
    public List<TemplateReview> getAllPendingReviews() {
        return reviewRepository.findByStatusOrderByCreatedAtDesc("PENDING");
    }

    /**
     * Calculate average rating for a version.
     */
    public Double getAverageRating(Long versionId) {
        return reviewRepository.averageRatingByVersionId(versionId);
    }

    // ─── Analytics ─────────────────────────────────────────────────────

    /**
     * Get comprehensive versioning and collaboration statistics.
     */
    public TemplateVersionStats getStats() {
        List<TemplateVersion> allVersions = versionRepository.findAll();
        List<TemplateReview> allReviews = reviewRepository.findAll();

        long totalVersions = allVersions.size();
        long totalReviews = allReviews.size();
        long pendingReviews = allReviews.stream().filter(r -> "PENDING".equals(r.getStatus())).count();
        long approvedVersions = allVersions.stream().filter(v -> "APPROVED".equals(v.getStatus()) || "PUBLISHED".equals(v.getStatus())).count();
        long rejectedVersions = allVersions.stream().filter(v -> "REJECTED".equals(v.getStatus())).count();
        long draftVersions = allVersions.stream().filter(v -> "DRAFT".equals(v.getStatus())).count();
        long lockedVersions = allVersions.stream().filter(TemplateVersion::isLocked).count();

        long totalTemplates = allVersions.stream().map(TemplateVersion::getTemplateId).distinct().count();

        double avgReviewsPerVersion = totalVersions > 0 ? Math.round(((double) totalReviews / totalVersions) * 100.0) / 100.0 : 0.0;
        double avgVersionCountPerTemplate = totalTemplates > 0 ? Math.round(((double) totalVersions / totalTemplates) * 100.0) / 100.0 : 0.0;

        Double avgRating = allReviews.stream()
                .filter(r -> r.getRating() != null)
                .mapToInt(TemplateReview::getRating)
                .average()
                .orElse(0.0);

        Map<String, Long> authorCounts = allVersions.stream()
                .filter(v -> v.getAuthor() != null)
                .collect(Collectors.groupingBy(TemplateVersion::getAuthor, Collectors.counting()));

        Map<String, Long> reviewerCounts = reviewRepository.countByReviewer().stream()
                .collect(Collectors.toMap(r -> (String) r[0], r -> (Long) r[1],
                        (a, b) -> a, LinkedHashMap::new));

        Map<String, Long> statusBreakdown = allVersions.stream()
                .collect(Collectors.groupingBy(TemplateVersion::getStatus, Collectors.counting()));

        return TemplateVersionStats.builder()
                .totalTemplates(totalTemplates)
                .totalVersions(totalVersions)
                .totalReviews(totalReviews)
                .pendingReviews(pendingReviews)
                .approvedVersions(approvedVersions)
                .rejectedVersions(rejectedVersions)
                .draftVersions(draftVersions)
                .lockedVersions(lockedVersions)
                .avgReviewsPerVersion(avgReviewsPerVersion)
                .avgRatingAcrossAll(Math.round(avgRating * 100.0) / 100.0)
                .authorVersionCounts(authorCounts)
                .reviewerCounts(reviewerCounts)
                .statusBreakdown(statusBreakdown)
                .avgVersionCountPerTemplate((long) avgVersionCountPerTemplate)
                .build();
    }

    /**
     * Get version count for a specific template.
     */
    public long getVersionCount(Long templateId) {
        return versionRepository.countByTemplateId(templateId);
    }

    /**
     * Find versions by tag.
     */
    public List<TemplateVersion> getVersionsByTag(String tag) {
        return versionRepository.findByTag(tag);
    }

    /**
     * Find all locked versions.
     */
    public List<TemplateVersion> getLockedVersions() {
        return versionRepository.findLockedVersions();
    }

    /**
     * Find versions by author.
     */
    public List<TemplateVersion> getVersionsByAuthor(String author) {
        return versionRepository.findByAuthorOrderByCreatedAtDesc(author);
    }

    // ─── Internal Helpers ──────────────────────────────────────────────

    /**
     * Sync the published version content back to the EmailTemplate entity.
     */
    private void syncTemplateFromVersion(Long templateId, TemplateVersion version) {
        templateRepository.findById(templateId).ifPresent(template -> {
            template.setTitle(version.getTitle());
            template.setBody(version.getBody());
            templateRepository.save(template);
        });
    }
}
