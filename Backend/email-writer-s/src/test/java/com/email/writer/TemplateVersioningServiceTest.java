package com.email.writer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TemplateVersioningServiceTest {

    @Mock
    private TemplateVersionRepository versionRepository;

    @Mock
    private TemplateReviewRepository reviewRepository;

    @Mock
    private EmailTemplateRepository templateRepository;

    @InjectMocks
    private TemplateVersioningService versioningService;

    private TemplateVersion sampleVersion;
    private EmailTemplate sampleTemplate;

    @BeforeEach
    void setUp() {
        sampleVersion = TemplateVersion.builder()
                .id(1L)
                .templateId(10L)
                .versionNumber(1)
                .title("Meeting Confirmation")
                .body("Hi {name},\n\nPlease confirm the meeting on {date}.\n\nBest,\nTeam")
                .changeDescription("Initial version")
                .author("alice")
                .status("DRAFT")
                .isCurrent(false)
                .linesAdded(4)
                .linesRemoved(0)
                .characterCount(62)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        sampleTemplate = EmailTemplate.builder()
                .id(10L)
                .title("Meeting Confirmation")
                .body("Hi {name},\n\nPlease confirm the meeting on {date}.\n\nBest,\nTeam")
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ─── Version Creation Tests ────────────────────────────────────────

    @Test
    void testCreateVersion_firstVersion() {
        when(versionRepository.findLatestVersionNumber(10L)).thenReturn(Optional.empty());
        when(versionRepository.save(any(TemplateVersion.class))).thenAnswer(inv -> {
            TemplateVersion v = inv.getArgument(0);
            v.setId(1L);
            return v;
        });

        TemplateVersionCreateRequest request = new TemplateVersionCreateRequest();
        request.setTemplateId(10L);
        request.setTitle("Meeting Confirmation");
        request.setBody("Hi {name},\n\nPlease confirm the meeting.");
        request.setAuthor("alice");

        TemplateVersion result = versioningService.createVersion(request);

        assertNotNull(result);
        assertEquals(Integer.valueOf(1), result.getVersionNumber());
        assertEquals("DRAFT", result.getStatus());
        assertFalse(result.getIsCurrent());
        assertEquals(4, result.getLinesAdded());
    }

    @Test
    void testCreateVersion_incrementalVersion() {
        when(versionRepository.findLatestVersionNumber(10L)).thenReturn(Optional.of(2));

        TemplateVersion previous = TemplateVersion.builder()
                .body("Line 1\nLine 2\nLine 3")
                .build();
        when(versionRepository.findByTemplateIdAndVersionNumber(10L, 2)).thenReturn(Optional.of(previous));
        when(versionRepository.save(any(TemplateVersion.class))).thenAnswer(inv -> inv.getArgument(0));

        TemplateVersionCreateRequest request = new TemplateVersionCreateRequest();
        request.setTemplateId(10L);
        request.setBody("Line 1\nLine 2\nLine 3\nLine 4");
        request.setAuthor("bob");

        TemplateVersion result = versioningService.createVersion(request);

        assertEquals(Integer.valueOf(3), result.getVersionNumber());
        assertEquals("bob", result.getAuthor());
    }

    @Test
    void testCreateVersion_throwsOnMissingTemplateId() {
        TemplateVersionCreateRequest request = new TemplateVersionCreateRequest();
        request.setBody("Body");

        assertThrows(IllegalArgumentException.class, () -> versioningService.createVersion(request));
    }

    @Test
    void testCreateVersion_throwsOnEmptyBody() {
        TemplateVersionCreateRequest request = new TemplateVersionCreateRequest();
        request.setTemplateId(1L);
        request.setBody("");

        assertThrows(IllegalArgumentException.class, () -> versioningService.createVersion(request));
    }

    @Test
    void testCreateVersionFromTemplate() {
        when(templateRepository.findById(10L)).thenReturn(Optional.of(sampleTemplate));
        when(versionRepository.findLatestVersionNumber(10L)).thenReturn(Optional.empty());
        when(versionRepository.save(any(TemplateVersion.class))).thenAnswer(inv -> inv.getArgument(0));

        TemplateVersion result = versioningService.createVersionFromTemplate(10L, "Synced from live", "admin");

        assertNotNull(result);
        assertEquals("Meeting Confirmation", result.getTitle());
        assertEquals("admin", result.getAuthor());
    }

    @Test
    void testCreateVersionFromTemplate_notFound() {
        when(templateRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> versioningService.createVersionFromTemplate(99L, "test", "admin"));
    }

    // ─── Query Tests ───────────────────────────────────────────────────

    @Test
    void testGetVersions() {
        when(versionRepository.findByTemplateIdOrderByVersionNumberDesc(10L))
                .thenReturn(List.of(sampleVersion));

        List<TemplateVersion> result = versioningService.getVersions(10L);
        assertEquals(1, result.size());
    }

    @Test
    void testGetVersion() {
        when(versionRepository.findByTemplateIdAndVersionNumber(10L, 1))
                .thenReturn(Optional.of(sampleVersion));

        TemplateVersion result = versioningService.getVersion(10L, 1);
        assertEquals("Meeting Confirmation", result.getTitle());
    }

    @Test
    void testGetVersion_notFound() {
        when(versionRepository.findByTemplateIdAndVersionNumber(10L, 99))
                .thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> versioningService.getVersion(10L, 99));
    }

    @Test
    void testGetCurrentVersion() {
        sampleVersion.setIsCurrent(true);
        when(versionRepository.findByTemplateIdAndIsCurrentTrue(10L)).thenReturn(Optional.of(sampleVersion));

        TemplateVersion result = versioningService.getCurrentVersion(10L);
        assertTrue(result.getIsCurrent());
    }

    @Test
    void testGetCurrentVersion_notFound() {
        when(versionRepository.findByTemplateIdAndIsCurrentTrue(10L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> versioningService.getCurrentVersion(10L));
    }

    // ─── Diff Tests ────────────────────────────────────────────────────

    @Test
    void testDiffVersions() {
        TemplateVersion v1 = TemplateVersion.builder()
                .body("Line A\nLine B\nLine C")
                .author("alice")
                .status("PUBLISHED")
                .build();

        TemplateVersion v2 = TemplateVersion.builder()
                .body("Line A\nLine B\nLine C\nLine D")
                .author("bob")
                .status("DRAFT")
                .build();

        when(versionRepository.findByTemplateIdAndVersionNumber(10L, 1)).thenReturn(Optional.of(v1));
        when(versionRepository.findByTemplateIdAndVersionNumber(10L, 2)).thenReturn(Optional.of(v2));

        TemplateDiffResult diff = versioningService.diffVersions(10L, 1, 2);

        assertNotNull(diff);
        assertEquals("ADDITIVE", diff.getDiffType());
        assertEquals(Integer.valueOf(1), diff.getAddedLines());
        assertEquals(Integer.valueOf(0), diff.getRemovedLines());
        assertTrue(diff.getSimilarityPercent() > 0);
    }

    // ─── Publishing Tests ──────────────────────────────────────────────

    @Test
    void testPublishVersion() {
        when(versionRepository.findByTemplateIdAndVersionNumber(10L, 1))
                .thenReturn(Optional.of(sampleVersion));
        when(versionRepository.findByTemplateIdAndIsCurrentTrue(10L)).thenReturn(Optional.empty());
        when(versionRepository.save(any(TemplateVersion.class))).thenAnswer(inv -> inv.getArgument(0));
        when(templateRepository.findById(10L)).thenReturn(Optional.of(sampleTemplate));
        when(templateRepository.save(any(EmailTemplate.class))).thenAnswer(inv -> inv.getArgument(0));

        TemplateVersion result = versioningService.publishVersion(10L, 1);

        assertTrue(result.getIsCurrent());
        assertEquals("PUBLISHED", result.getStatus());
    }

    // ─── Rollback Tests ────────────────────────────────────────────────

    @Test
    void testRollbackToVersion() {
        TemplateVersion target = TemplateVersion.builder()
                .templateId(10L)
                .title("Old Title")
                .body("Old body content")
                .author("original")
                .build();

        when(versionRepository.findByTemplateIdAndVersionNumber(10L, 1)).thenReturn(Optional.of(target));
        when(versionRepository.findLatestVersionNumber(10L)).thenReturn(Optional.of(3));
        when(versionRepository.save(any(TemplateVersion.class))).thenAnswer(inv -> inv.getArgument(0));

        TemplateVersion result = versioningService.rollbackToVersion(10L, 1, "admin");

        assertNotNull(result);
        assertEquals("APPROVED", result.getStatus());
    }

    // ─── Locking Tests ─────────────────────────────────────────────────

    @Test
    void testAcquireLock_success() {
        when(versionRepository.findById(1L)).thenReturn(Optional.of(sampleVersion));
        when(versionRepository.save(any(TemplateVersion.class))).thenAnswer(inv -> inv.getArgument(0));

        boolean result = versioningService.acquireLock(1L, "token123", 30);

        assertTrue(result);
        assertEquals("token123", sampleVersion.getLockToken());
        assertNotNull(sampleVersion.getLockExpiresAt());
    }

    @Test
    void testAcquireLock_alreadyLocked() {
        sampleVersion.setLockToken("existing-token");
        sampleVersion.setLockExpiresAt(LocalDateTime.now().plusHours(1));
        when(versionRepository.findById(1L)).thenReturn(Optional.of(sampleVersion));

        boolean result = versioningService.acquireLock(1L, "new-token", 30);

        assertFalse(result);
    }

    @Test
    void testReleaseLock_success() {
        sampleVersion.setLockToken("token123");
        when(versionRepository.findById(1L)).thenReturn(Optional.of(sampleVersion));
        when(versionRepository.save(any(TemplateVersion.class))).thenAnswer(inv -> inv.getArgument(0));

        boolean result = versioningService.releaseLock(1L, "token123");

        assertTrue(result);
        assertNull(sampleVersion.getLockToken());
    }

    @Test
    void testReleaseLock_wrongToken() {
        sampleVersion.setLockToken("token123");
        when(versionRepository.findById(1L)).thenReturn(Optional.of(sampleVersion));

        boolean result = versioningService.releaseLock(1L, "wrong-token");

        assertFalse(result);
    }

    // ─── Entity Method Tests ───────────────────────────────────────────

    @Test
    void testTemplateVersion_isLocked() {
        sampleVersion.setLockToken("token");
        sampleVersion.setLockExpiresAt(LocalDateTime.now().plusMinutes(10));
        assertTrue(sampleVersion.isLocked());

        sampleVersion.setLockExpiresAt(LocalDateTime.now().minusMinutes(1));
        assertFalse(sampleVersion.isLocked());
    }

    @Test
    void testTemplateVersion_isNotLocked_noToken() {
        sampleVersion.setLockToken(null);
        assertFalse(sampleVersion.isLocked());
    }

    @Test
    void testTemplateVersion_computeDiff_created() {
        TemplateVersion v = TemplateVersion.builder().body("New content").build();

        TemplateDiffResult diff = v.computeDiff(null);

        assertEquals("CREATED", diff.getDiffType());
        assertEquals(Integer.valueOf(1), diff.getAddedLines());
        assertEquals(Integer.valueOf(0), diff.getRemovedLines());
    }

    @Test
    void testTemplateVersion_computeDiff_unchanged() {
        TemplateVersion prev = TemplateVersion.builder().body("Same\nContent").build();
        TemplateVersion curr = TemplateVersion.builder().body("Same\nContent").build();

        TemplateDiffResult diff = curr.computeDiff(prev);

        assertEquals("UNCHANGED", diff.getDiffType());
        assertEquals(Integer.valueOf(0), diff.getAddedLines());
        assertEquals(Integer.valueOf(0), diff.getRemovedLines());
    }

    @Test
    void testTemplateVersion_computeDiff_additive() {
        TemplateVersion prev = TemplateVersion.builder().body("Line 1").build();
        TemplateVersion curr = TemplateVersion.builder().body("Line 1\nLine 2").build();

        TemplateDiffResult diff = curr.computeDiff(prev);

        assertEquals("ADDITIVE", diff.getDiffType());
        assertEquals(Integer.valueOf(1), diff.getAddedLines());
    }

    @Test
    void testTemplateVersion_computeDiff_reductive() {
        TemplateVersion prev = TemplateVersion.builder().body("Line 1\nLine 2").build();
        TemplateVersion curr = TemplateVersion.builder().body("Line 1").build();

        TemplateDiffResult diff = curr.computeDiff(prev);

        assertEquals("REDUCTIVE", diff.getDiffType());
        assertEquals(Integer.valueOf(0), diff.getAddedLines());
        assertEquals(Integer.valueOf(1), diff.getRemovedLines());
    }

    @Test
    void testTemplateVersion_computeDiff_modified() {
        TemplateVersion prev = TemplateVersion.builder().body("A\nB\nC").build();
        TemplateVersion curr = TemplateVersion.builder().body("A\nD\nE").build();

        TemplateDiffResult diff = curr.computeDiff(prev);

        assertEquals("MODIFIED", diff.getDiffType());
    }

    // ─── Review Tests ──────────────────────────────────────────────────

    @Test
    void testSubmitReview_approve() {
        when(versionRepository.findById(1L)).thenReturn(Optional.of(sampleVersion));
        when(reviewRepository.save(any(TemplateReview.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reviewRepository.countByTemplateVersionIdAndStatus(1L, "APPROVED")).thenReturn(0L);
        when(reviewRepository.countByTemplateVersionIdAndStatus(1L, "REJECTED")).thenReturn(0L);
        when(reviewRepository.countByTemplateVersionIdAndStatus(1L, "CHANGES_REQUESTED")).thenReturn(0L);
        when(versionRepository.save(any(TemplateVersion.class))).thenAnswer(inv -> inv.getArgument(0));

        TemplateReviewRequest request = new TemplateReviewRequest();
        request.setReviewer("bob");
        request.setStatus("APPROVED");
        request.setRating(5);
        request.setComments("Looks great!");

        TemplateReview result = versioningService.submitReview(1L, request);

        assertEquals("APPROVED", result.getStatus());
        assertEquals("bob", result.getReviewer());
        assertEquals(5, result.getRating());
    }

    @Test
    void testSubmitReview_reject() {
        when(versionRepository.findById(1L)).thenReturn(Optional.of(sampleVersion));
        when(reviewRepository.save(any(TemplateReview.class))).thenAnswer(inv -> inv.getArgument(0));
        when(versionRepository.save(any(TemplateVersion.class))).thenAnswer(inv -> inv.getArgument(0));

        TemplateReviewRequest request = new TemplateReviewRequest();
        request.setReviewer("carol");
        request.setStatus("REJECTED");
        request.setComments("Needs major changes");

        TemplateReview result = versioningService.submitReview(1L, request);

        assertEquals("REJECTED", result.getStatus());
    }

    @Test
    void testSubmitReview_throwsOnMissingReviewer() {
        TemplateReviewRequest request = new TemplateReviewRequest();
        request.setStatus("APPROVED");

        assertThrows(IllegalArgumentException.class, () -> versioningService.submitReview(1L, request));
    }

    @Test
    void testSubmitReview_throwsOnMissingStatus() {
        TemplateReviewRequest request = new TemplateReviewRequest();
        request.setReviewer("bob");

        assertThrows(IllegalArgumentException.class, () -> versioningService.submitReview(1L, request));
    }

    @Test
    void testSubmitReview_versionNotFound() {
        when(versionRepository.findById(99L)).thenReturn(Optional.empty());

        TemplateReviewRequest request = new TemplateReviewRequest();
        request.setReviewer("bob");
        request.setStatus("APPROVED");

        assertThrows(NoSuchElementException.class, () -> versioningService.submitReview(99L, request));
    }

    // ─── Tag & Author Query Tests ──────────────────────────────────────

    @Test
    void testGetVersionsByTag() {
        when(versionRepository.findByTag("rollback")).thenReturn(List.of(sampleVersion));

        List<TemplateVersion> result = versioningService.getVersionsByTag("rollback");
        assertEquals(1, result.size());
    }

    @Test
    void testGetVersionsByAuthor() {
        when(versionRepository.findByAuthorOrderByCreatedAtDesc("alice")).thenReturn(List.of(sampleVersion));

        List<TemplateVersion> result = versioningService.getVersionsByAuthor("alice");
        assertEquals(1, result.size());
    }

    @Test
    void testGetLockedVersions() {
        when(versionRepository.findLockedVersions()).thenReturn(List.of(sampleVersion));

        List<TemplateVersion> result = versioningService.getLockedVersions();
        assertEquals(1, result.size());
    }

    // ─── Version Count Tests ───────────────────────────────────────────

    @Test
    void testGetVersionCount() {
        when(versionRepository.countByTemplateId(10L)).thenReturn(5L);

        long count = versioningService.getVersionCount(10L);
        assertEquals(5L, count);
    }

    // ─── Stats Tests ───────────────────────────────────────────────────

    @Test
    void testGetStats() {
        when(versionRepository.findAll()).thenReturn(List.of(sampleVersion));
        when(reviewRepository.findAll()).thenReturn(Collections.emptyList());
        when(reviewRepository.countByReviewer()).thenReturn(Collections.emptyList());

        TemplateVersionStats stats = versioningService.getStats();

        assertEquals(1, stats.getTotalVersions());
        assertEquals(0, stats.getTotalReviews());
        assertEquals(1, stats.getTotalTemplates());
    }
}
