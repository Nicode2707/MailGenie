package com.email.writer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DraftVersionService covering version creation, revert,
 * diff computation, session management, and entity behavior.
 */
@ExtendWith(MockitoExtension.class)
class DraftVersionManagerTest {

    @Mock
    private DraftVersionRepository repository;

    @InjectMocks
    private DraftVersionService service;

    private DraftVersion sampleDraft;

    @BeforeEach
    void setUp() {
        sampleDraft = DraftVersion.builder()
                .id(1L)
                .sessionId("session-abc")
                .versionNumber(1)
                .content("Hello, thank you for your email.")
                .source("AI_GENERATED")
                .tone("professional")
                .language("English")
                .provider("groq")
                .active(true)
                .build();
    }

    // ---- Save Version Tests ----

    @Test
    void saveVersion_validDraft_assignsVersionNumber() {
        when(repository.getMaxVersionNumber("session-abc")).thenReturn(0);
        when(repository.save(any(DraftVersion.class))).thenReturn(sampleDraft);

        DraftVersion result = service.saveVersion(sampleDraft);

        assertEquals(1, result.getVersionNumber());
        assertTrue(result.isActive());
        verify(repository).deactivateAllInSession("session-abc");
        verify(repository).save(any(DraftVersion.class));
    }

    @Test
    void saveVersion_existingSession_incrementsVersion() {
        when(repository.getMaxVersionNumber("session-abc")).thenReturn(2);
        when(repository.save(any(DraftVersion.class))).thenReturn(sampleDraft);

        DraftVersion result = service.saveVersion(sampleDraft);
        assertEquals(3, result.getVersionNumber());
    }

    @Test
    void saveVersion_nullSessionId_throwsException() {
        sampleDraft.setSessionId(null);
        assertThrows(IllegalArgumentException.class, () -> service.saveVersion(sampleDraft));
    }

    @Test
    void saveVersion_emptyContent_throwsException() {
        sampleDraft.setContent("");
        assertThrows(IllegalArgumentException.class, () -> service.saveVersion(sampleDraft));
    }

    @Test
    void saveAiGeneration_savesWithCorrectSource() {
        when(repository.getMaxVersionNumber("session-abc")).thenReturn(0);
        when(repository.save(any(DraftVersion.class))).thenReturn(sampleDraft);

        DraftVersion result = service.saveAiGeneration(
                "session-abc", "Generated text", "professional", "English", "groq",
                "Original email", "Generated reply", 150L);

        assertEquals("AI_GENERATED", result.getSource());
    }

    @Test
    void saveUserEdit_savesWithCorrectSource() {
        when(repository.getMaxVersionNumber("session-abc")).thenReturn(1);
        when(repository.save(any(DraftVersion.class))).thenReturn(sampleDraft);

        DraftVersion result = service.saveUserEdit("session-abc", "Edited text", "My edit");

        assertEquals("USER_EDITED", result.getSource());
    }

    // ---- Revert Tests ----

    @Test
    void revertToVersion_createsRevertedVersion() {
        DraftVersion target = DraftVersion.builder()
                .sessionId("session-abc").versionNumber(2)
                .content("Version 2 content").tone("casual").language("English")
                .build();
        when(repository.findBySessionIdAndVersionNumber("session-abc", 2))
                .thenReturn(Optional.of(target));
        when(repository.getMaxVersionNumber("session-abc")).thenReturn(2);
        when(repository.save(any(DraftVersion.class))).thenReturn(sampleDraft);

        DraftVersion result = service.revertToVersion("session-abc", 2);

        assertEquals("REVERTED", result.getSource());
        verify(repository).save(any(DraftVersion.class));
    }

    @Test
    void revertToVersion_missingVersion_throwsException() {
        when(repository.findBySessionIdAndVersionNumber("session-abc", 99))
                .thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> service.revertToVersion("session-abc", 99));
    }

    // ---- Retrieval Tests ----

    @Test
    void getSessionVersions_delegatesToRepository() {
        when(repository.findBySessionIdOrderByVersionNumberAsc("session-abc"))
                .thenReturn(List.of(sampleDraft));

        List<DraftVersion> versions = service.getSessionVersions("session-abc");
        assertEquals(1, versions.size());
    }

    @Test
    void getActiveVersion_returnsActiveDraft() {
        when(repository.findBySessionIdAndActiveTrue("session-abc"))
                .thenReturn(Optional.of(sampleDraft));

        Optional<DraftVersion> result = service.getActiveVersion("session-abc");
        assertTrue(result.isPresent());
    }

    @Test
    void getVersion_specificVersion() {
        when(repository.findBySessionIdAndVersionNumber("session-abc", 1))
                .thenReturn(Optional.of(sampleDraft));

        Optional<DraftVersion> result = service.getVersion("session-abc", 1);
        assertTrue(result.isPresent());
        assertEquals(1, result.get().getVersionNumber());
    }

    // ---- Diff Tests ----

    @Test
    void computeDiff_identicalVersions_allKeep() {
        DraftVersion v1 = DraftVersion.builder().sessionId("s1").versionNumber(1).content("Hello").build();
        DraftVersion v2 = DraftVersion.builder().sessionId("s1").versionNumber(2).content("Hello").build();

        when(repository.findBySessionIdAndVersionNumber("s1", 1)).thenReturn(Optional.of(v1));
        when(repository.findBySessionIdAndVersionNumber("s1", 2)).thenReturn(Optional.of(v2));

        List<DraftVersionService.DiffOperation> ops = service.computeDiff("s1", 1, 2);
        assertTrue(ops.stream().allMatch(op -> "KEEP".equals(op.getType())));
    }

    @Test
    void computeDiff_addedContent() {
        DraftVersion v1 = DraftVersion.builder().sessionId("s1").versionNumber(1).content("Hi").build();
        DraftVersion v2 = DraftVersion.builder().sessionId("s1").versionNumber(2).content("Hi there").build();

        when(repository.findBySessionIdAndVersionNumber("s1", 1)).thenReturn(Optional.of(v1));
        when(repository.findBySessionIdAndVersionNumber("s1", 2)).thenReturn(Optional.of(v2));

        List<DraftVersionService.DiffOperation> ops = service.computeDiff("s1", 1, 2);
        long inserts = ops.stream().filter(op -> "INSERT".equals(op.getType())).count();
        assertTrue(inserts > 0);
    }

    @Test
    void computeDiff_removedContent() {
        DraftVersion v1 = DraftVersion.builder().sessionId("s1").versionNumber(1).content("Hello world").build();
        DraftVersion v2 = DraftVersion.builder().sessionId("s1").versionNumber(2).content("Hi").build();

        when(repository.findBySessionIdAndVersionNumber("s1", 1)).thenReturn(Optional.of(v1));
        when(repository.findBySessionIdAndVersionNumber("s1", 2)).thenReturn(Optional.of(v2));

        List<DraftVersionService.DiffOperation> ops = service.computeDiff("s1", 1, 2);
        long deletes = ops.stream().filter(op -> "DELETE".equals(op.getType())).count();
        assertTrue(deletes > 0);
    }

    @Test
    void computeLineDiff_addsAndRemoves() {
        DraftVersion v1 = DraftVersion.builder().sessionId("s1").versionNumber(1)
                .content("Line 1\nLine 2\nLine 3").build();
        DraftVersion v2 = DraftVersion.builder().sessionId("s1").versionNumber(2)
                .content("Line 1\nLine 2 modified\nLine 3\nLine 4").build();

        when(repository.findBySessionIdAndVersionNumber("s1", 1)).thenReturn(Optional.of(v1));
        when(repository.findBySessionIdAndVersionNumber("s1", 2)).thenReturn(Optional.of(v2));

        List<DraftVersionService.DiffLine> lines = service.computeLineDiff("s1", 1, 2);
        long added = lines.stream().filter(l -> "ADDED".equals(l.getStatus())).count();
        long removed = lines.stream().filter(l -> "REMOVED".equals(l.getStatus())).count();
        assertTrue(added > 0);
        assertTrue(removed > 0);
    }

    @Test
    void getDiffSummary_calculatesStats() {
        DraftVersion v1 = DraftVersion.builder().sessionId("s1").versionNumber(1)
                .content("Line 1\nLine 2").wordCount(3).charCount(10).build();
        DraftVersion v2 = DraftVersion.builder().sessionId("s1").versionNumber(2)
                .content("Line 1\nLine 2\nLine 3").wordCount(5).charCount(18).build();

        when(repository.findBySessionIdAndVersionNumber("s1", 1)).thenReturn(Optional.of(v1));
        when(repository.findBySessionIdAndVersionNumber("s1", 2)).thenReturn(Optional.of(v2));

        DraftVersionService.DiffSummary summary = service.getDiffSummary("s1", 1, 2);
        assertEquals(1, summary.getFromVersion());
        assertEquals(2, summary.getToVersion());
        assertEquals(8, summary.getCharsChanged());
    }

    // ---- Session Management Tests ----

    @Test
    void deleteSession_existingSession_returnsTrue() {
        when(repository.countBySessionId("s1")).thenReturn(3L);
        boolean result = service.deleteSession("s1");
        assertTrue(result);
        verify(repository).deleteBySessionId("s1");
    }

    @Test
    void deleteSession_noVersions_returnsFalse() {
        when(repository.countBySessionId("s1")).thenReturn(0L);
        assertFalse(service.deleteSession("s1"));
    }

    @Test
    void getAllSessionIds_delegatesToRepository() {
        when(repository.findAllSessionIds()).thenReturn(List.of("s1", "s2"));
        assertEquals(2, service.getAllSessionIds().size());
    }

    @Test
    void getTotalVersionCount_delegatesToRepository() {
        when(repository.count()).thenReturn(15L);
        assertEquals(15, service.getTotalVersionCount());
    }

    @Test
    void purgeOldVersions_delegatesToRepository() {
        when(repository.purgeOlderThan(any())).thenReturn(5);
        assertEquals(5, service.purgeOldVersions(30));
    }

    @Test
    void getMostActiveSessions_mapsCorrectly() {
        when(repository.findMostActiveSessions()).thenReturn(List.of(
                new Object[]{"s1", 5L}, new Object[]{"s2", 3L}));

        List<Map<String, Object>> result = service.getMostActiveSessions();
        assertEquals(2, result.size());
        assertEquals("s1", result.get(0).get("sessionId"));
        assertEquals(5L, result.get(0).get("versionCount"));
    }

    // ---- DraftVersion Entity Tests ----

    @Test
    void draftVersion_calculateStats_computesCorrectly() {
        DraftVersion draft = DraftVersion.builder().content("Hello world test").build();
        draft.calculateStats();
        assertEquals(3, draft.getWordCount());
        assertEquals(16, draft.getCharCount());
    }

    @Test
    void draftVersion_calculateStats_emptyContent() {
        DraftVersion draft = DraftVersion.builder().content("").build();
        draft.calculateStats();
        assertEquals(0, draft.getWordCount());
        assertEquals(0, draft.getCharCount());
    }

    @Test
    void draftVersion_getPreview_shortContent_returnsFull() {
        DraftVersion draft = DraftVersion.builder().content("Short").build();
        assertEquals("Short", draft.getPreview(100));
    }

    @Test
    void draftVersion_getPreview_longContent_truncates() {
        DraftVersion draft = DraftVersion.builder().content("A very long draft content").build();
        String preview = draft.getPreview(10);
        assertEquals("A very long ...", preview);
    }

    @Test
    void draftVersion_isAiGenerated_true() {
        DraftVersion draft = DraftVersion.builder().source("AI_GENERATED").build();
        assertTrue(draft.isAiGenerated());
    }

    @Test
    void draftVersion_isUserEdited_true() {
        DraftVersion draft = DraftVersion.builder().source("USER_EDITED").build();
        assertTrue(draft.isUserEdited());
    }

    @Test
    void draftVersion_isReverted_true() {
        DraftVersion draft = DraftVersion.builder().source("REVERTED").build();
        assertTrue(draft.isReverted());
    }
}
