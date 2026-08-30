package com.email.writer;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service layer managing draft version lifecycle: creation, comparison,
 * reversion, and session management.
 */
@Service
public class DraftVersionService {

    private final DraftVersionRepository repository;

    public DraftVersionService(DraftVersionRepository repository) {
        this.repository = repository;
    }

    /**
     * Creates a new draft version within a session.
     * Automatically assigns the next version number and deactivates previous versions.
     */
    @Transactional
    public DraftVersion saveVersion(DraftVersion draft) {
        if (draft.getSessionId() == null || draft.getSessionId().trim().isEmpty()) {
            throw new IllegalArgumentException("Session ID is required.");
        }
        if (draft.getContent() == null || draft.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("Draft content is required.");
        }

        int nextVersion = repository.getMaxVersionNumber(draft.getSessionId()) + 1;
        draft.setVersionNumber(nextVersion);
        draft.setActive(true);
        draft.calculateStats();

        // Deactivate previous versions in this session
        repository.deactivateAllInSession(draft.getSessionId());

        return repository.save(draft);
    }

    /**
     * Creates a new version from a user-edited draft (saves current edit).
     */
    @Transactional
    public DraftVersion saveUserEdit(String sessionId, String content, String label) {
        DraftVersion draft = DraftVersion.builder()
                .sessionId(sessionId)
                .content(content)
                .source("USER_EDITED")
                .label(label)
                .build();
        return saveVersion(draft);
    }

    /**
     * Creates a new version from an AI generation.
     */
    @Transactional
    public DraftVersion saveAiGeneration(String sessionId, String content,
                                         String tone, String language, String provider,
                                         String originalEmailContent,
                                         String generatedReply,
                                         Long generationTimeMs) {
        DraftVersion draft = DraftVersion.builder()
                .sessionId(sessionId)
                .content(content)
                .source("AI_GENERATED")
                .tone(tone)
                .language(language)
                .provider(provider)
                .originalEmailContent(originalEmailContent)
                .generatedReply(generatedReply)
                .generationTimeMs(generationTimeMs)
                .build();
        return saveVersion(draft);
    }

    /**
     * Returns all versions for a session, ordered oldest to newest.
     */
    public List<DraftVersion> getSessionVersions(String sessionId) {
        return repository.findBySessionIdOrderByVersionNumberAsc(sessionId);
    }

    /**
     * Returns all versions for a session, ordered newest to oldest.
     */
    public List<DraftVersion> getSessionVersionsDesc(String sessionId) {
        return repository.findBySessionIdOrderByVersionNumberDesc(sessionId);
    }

    /**
     * Returns the currently active version for a session.
     */
    public Optional<DraftVersion> getActiveVersion(String sessionId) {
        return repository.findBySessionIdAndActiveTrue(sessionId);
    }

    /**
     * Returns a specific version within a session.
     */
    public Optional<DraftVersion> getVersion(String sessionId, int versionNumber) {
        return repository.findBySessionIdAndVersionNumber(sessionId, versionNumber);
    }

    /**
     * Reverts to a specific version by creating a new REVERTED version with that content.
     */
    @Transactional
    public DraftVersion revertToVersion(String sessionId, int versionNumber) {
        Optional<DraftVersion> targetVersion = repository.findBySessionIdAndVersionNumber(sessionId, versionNumber);
        if (targetVersion.isEmpty()) {
            throw new NoSuchElementException(
                    "Version " + versionNumber + " not found in session " + sessionId);
        }

        DraftVersion reverted = DraftVersion.builder()
                .sessionId(sessionId)
                .content(targetVersion.get().getContent())
                .source("REVERTED")
                .tone(targetVersion.get().getTone())
                .language(targetVersion.get().getLanguage())
                .provider(targetVersion.get().getProvider())
                .originalEmailContent(targetVersion.get().getOriginalEmailContent())
                .generatedReply(targetVersion.get().getGeneratedReply())
                .label("Reverted to v" + versionNumber)
                .build();

        return saveVersion(reverted);
    }

    /**
     * Computes a character-level diff between two versions.
     * Returns a list of diff operations (KEEP, INSERT, DELETE).
     */
    public List<DiffOperation> computeDiff(String sessionId, int fromVersion, int toVersion) {
        DraftVersion from = getVersion(sessionId, fromVersion)
                .orElseThrow(() -> new NoSuchElementException("Version " + fromVersion + " not found"));
        DraftVersion to = getVersion(sessionId, toVersion)
                .orElseThrow(() -> new NoSuchElementException("Version " + toVersion + " not found"));

        return computeTextDiff(from.getContent(), to.getContent());
    }

    /**
     * Computes a line-level diff between two versions.
     * Returns lines grouped by status: ADDED, REMOVED, UNCHANGED.
     */
    public List<DiffLine> computeLineDiff(String sessionId, int fromVersion, int toVersion) {
        DraftVersion from = getVersion(sessionId, fromVersion)
                .orElseThrow(() -> new NoSuchElementException("Version " + fromVersion + " not found"));
        DraftVersion to = getVersion(sessionId, toVersion)
                .orElseThrow(() -> new NoSuchElementException("Version " + toVersion + " not found"));

        return computeLineDiff(from.getContent(), to.getContent());
    }

    /**
     * Returns a summary of changes between two versions.
     */
    public DiffSummary getDiffSummary(String sessionId, int fromVersion, int toVersion) {
        DraftVersion from = getVersion(sessionId, fromVersion)
                .orElseThrow(() -> new NoSuchElementException("Version " + fromVersion + " not found"));
        DraftVersion to = getVersion(sessionId, toVersion)
                .orElseThrow(() -> new NoSuchElementException("Version " + toVersion + " not found"));

        List<DiffLine> lines = computeLineDiff(from.getContent(), to.getContent());

        long added = lines.stream().filter(l -> "ADDED".equals(l.getStatus())).count();
        long removed = lines.stream().filter(l -> "REMOVED".equals(l.getStatus())).count();
        long unchanged = lines.stream().filter(l -> "UNCHANGED".equals(l.getStatus())).count();

        int fromWords = from.getWordCount();
        int toWords = to.getWordCount();
        int fromChars = from.getCharCount();
        int toChars = to.getCharCount();

        return DiffSummary.builder()
                .fromVersion(fromVersion)
                .toVersion(toVersion)
                .linesAdded(added)
                .linesRemoved(removed)
                .linesUnchanged(unchanged)
                .wordsChanged(toWords - fromWords)
                .charsChanged(toChars - fromChars)
                .fromWordCount(fromWords)
                .toWordCount(toWords)
                .build();
    }

    /**
     * Returns all unique session IDs, most recent first.
     */
    public List<String> getAllSessionIds() {
        return repository.findAllSessionIds();
    }

    /**
     * Returns all sessions created within a date range.
     */
    public List<String> getSessionIdsInRange(LocalDateTime start, LocalDateTime end) {
        return repository.findSessionIdsInRange(start, end);
    }

    /**
     * Returns the total number of draft versions across all sessions.
     */
    public long getTotalVersionCount() {
        return repository.count();
    }

    /**
     * Returns the version count for a specific session.
     */
    public long getSessionVersionCount(String sessionId) {
        return repository.countBySessionId(sessionId);
    }

    /**
     * Deletes all versions in a session.
     */
    @Transactional
    public boolean deleteSession(String sessionId) {
        long count = repository.countBySessionId(sessionId);
        if (count == 0) return false;
        repository.deleteBySessionId(sessionId);
        return true;
    }

    /**
     * Purges draft versions older than the given number of days.
     */
    @Transactional
    public int purgeOldVersions(int olderThanDays) {
        return repository.purgeOlderThan(LocalDateTime.now().minusDays(olderThanDays));
    }

    /**
     * Returns the most active sessions (by version count).
     */
    public List<Map<String, Object>> getMostActiveSessions() {
        List<Object[]> rows = repository.findMostActiveSessions();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("sessionId", row[0]);
            entry.put("versionCount", row[1]);
            result.add(entry);
        }
        return result;
    }

    // ---- Internal Diff Algorithm ----

    /**
     * Simple character-level LCS-based diff.
     */
    private List<DiffOperation> computeTextDiff(String oldText, String newText) {
        if (oldText == null) oldText = "";
        if (newText == null) newText = "";

        int m = oldText.length();
        int n = newText.length();

        // LCS table
        int[][] dp = new int[m + 1][n + 1];
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (oldText.charAt(i - 1) == newText.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }

        // Backtrack to build operations
        List<DiffOperation> ops = new ArrayList<>();
        int i = m, j = n;
        while (i > 0 || j > 0) {
            if (i > 0 && j > 0 && oldText.charAt(i - 1) == newText.charAt(j - 1)) {
                ops.add(new DiffOperation("KEEP", String.valueOf(oldText.charAt(i - 1))));
                i--;
                j--;
            } else if (j > 0 && (i == 0 || dp[i][j - 1] >= dp[i - 1][j])) {
                ops.add(new DiffOperation("INSERT", String.valueOf(newText.charAt(j - 1))));
                j--;
            } else {
                ops.add(new DiffOperation("DELETE", String.valueOf(oldText.charAt(i - 1))));
                i--;
            }
        }

        Collections.reverse(ops);
        return ops;
    }

    /**
     * Simple line-level diff using longest common subsequence.
     */
    private List<DiffLine> computeLineDiff(String oldText, String newText) {
        String[] oldLines = oldText.split("\n", -1);
        String[] newLines = newText.split("\n", -1);

        int m = oldLines.length;
        int n = newLines.length;
        int[][] dp = new int[m + 1][n + 1];

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (oldLines[i - 1].equals(newLines[j - 1])) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }

        List<DiffLine> lines = new ArrayList<>();
        int i = m, j = n;
        while (i > 0 || j > 0) {
            if (i > 0 && j > 0 && oldLines[i - 1].equals(newLines[j - 1])) {
                lines.add(new DiffLine("UNCHANGED", oldLines[i - 1]));
                i--;
                j--;
            } else if (j > 0 && (i == 0 || dp[i][j - 1] >= dp[i - 1][j])) {
                lines.add(new DiffLine("ADDED", newLines[j - 1]));
                j--;
            } else {
                lines.add(new DiffLine("REMOVED", oldLines[i - 1]));
                i--;
            }
        }

        Collections.reverse(lines);
        return lines;
    }

    // ---- Diff DTOs ----

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class DiffOperation {
        private String type; // KEEP, INSERT, DELETE
        private String value;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class DiffLine {
        private String status; // ADDED, REMOVED, UNCHANGED
        private String line;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class DiffSummary {
        private int fromVersion;
        private int toVersion;
        private long linesAdded;
        private long linesRemoved;
        private long linesUnchanged;
        private int wordsChanged;
        private int charsChanged;
        private int fromWordCount;
        private int toWordCount;
    }
}
