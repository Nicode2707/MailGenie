package com.email.writer;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * Search criteria DTO for querying email history records.
 * All fields are optional — null means "don't filter on this field".
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailHistorySearchCriteria {

    /**
     * Full-text search across original content and generated reply.
     */
    private String query;

    /**
     * Filter by tone (e.g., "professional", "casual").
     */
    private String tone;

    /**
     * Filter by LLM provider (e.g., "groq", "openai").
     */
    private String provider;

    /**
     * Filter by language (e.g., "English", "French").
     */
    private String language;

    /**
     * Only return records created after this timestamp.
     */
    private LocalDateTime createdAfter;

    /**
     * Only return records created before this timestamp.
     */
    private LocalDateTime createdBefore;

    /**
     * Sort field: "createdAt", "tone", "provider".
     */
    private String sortBy;

    /**
     * Sort direction: "asc" or "desc".
     */
    private String sortDirection;

    /**
     * Page number (0-indexed).
     */
    private int page = 0;

    /**
     * Page size (max 100).
     */
    private int pageSize = 20;

    /**
     * Returns the effective page size, capped at 100.
     */
    public int effectivePageSize() {
        return Math.max(1, Math.min(pageSize, 100));
    }

    /**
     * Returns the effective page number, floored at 0.
     */
    public int effectivePage() {
        return Math.max(0, page);
    }

    /**
     * Returns the sort field with a safe default.
     */
    public String effectiveSortBy() {
        if (sortBy == null || sortBy.trim().isEmpty()) return "createdAt";
        String allowed = sortBy.trim().toLowerCase();
        if ("tone".equals(allowed) || "provider".equals(allowed) || "language".equals(allowed)
                || "createdat".equals(allowed)) {
            return sortBy.trim();
        }
        return "createdAt";
    }

    /**
     * Returns true if any filter criteria are active (non-null/non-default).
     */
    public boolean hasFilters() {
        return query != null || tone != null || provider != null || language != null
                || createdAfter != null || createdBefore != null;
    }
}
