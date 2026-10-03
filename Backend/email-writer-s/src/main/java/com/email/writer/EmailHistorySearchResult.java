package com.email.writer;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

/**
 * Paginated search result DTO for email history queries.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailHistorySearchResult {

    /**
     * The page of results returned.
     */
    private List<EmailHistory> records;

    /**
     * Total number of records matching the criteria (across all pages).
     */
    private long totalRecords;

    /**
     * Current page number (0-indexed).
     */
    private int currentPage;

    /**
     * Number of records per page.
     */
    private int pageSize;

    /**
     * Total number of pages available.
     */
    private int totalPages;

    /**
     * Whether this is the first page.
     */
    private boolean first;

    /**
     * Whether this is the last page.
     */
    private boolean last;

    /**
     * Whether there are any results at all.
     */
    private boolean empty;
}
