package com.email.writer;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * REST controller exposing search, analytics, and export endpoints
 * for the email generation history subsystem.
 */
@RestController
@RequestMapping("/api/history/search")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class EmailHistorySearchController {

    private final EmailHistorySearchService searchService;

    public EmailHistorySearchController(EmailHistorySearchService searchService) {
        this.searchService = searchService;
    }

    /**
     * POST /api/history/search — Paginated search with filters.
     */
    @PostMapping
    public ResponseEntity<EmailHistorySearchResult> search(
            @RequestBody(required = false) EmailHistorySearchCriteria criteria) {
        return ResponseEntity.ok(searchService.search(criteria));
    }

    /**
     * GET /api/history/search?query=...&tone=...&provider=...&language=...
     * — Simple GET-based search with query params.
     */
    @GetMapping
    public ResponseEntity<EmailHistorySearchResult> searchGet(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String tone,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdAfter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdBefore,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {

        EmailHistorySearchCriteria criteria = new EmailHistorySearchCriteria();
        criteria.setQuery(query);
        criteria.setTone(tone);
        criteria.setProvider(provider);
        criteria.setLanguage(language);
        criteria.setCreatedAfter(createdAfter);
        criteria.setCreatedBefore(createdBefore);
        criteria.setSortBy(sortBy);
        criteria.setSortDirection(sortDirection);
        criteria.setPage(page);
        criteria.setPageSize(pageSize);

        return ResponseEntity.ok(searchService.search(criteria));
    }

    /**
     * GET /api/history/search/analytics — Full usage analytics summary.
     */
    @GetMapping("/analytics")
    public ResponseEntity<EmailHistoryAnalytics> getAnalytics() {
        return ResponseEntity.ok(searchService.getAnalytics());
    }

    /**
     * GET /api/history/search/analytics/range?start=...&end=...
     * — Analytics scoped to a date range.
     */
    @GetMapping("/analytics/range")
    public ResponseEntity<EmailHistoryAnalytics> getAnalyticsForRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(searchService.getAnalyticsForRange(start, end));
    }

    /**
     * GET /api/history/search/export — Download all history as CSV.
     */
    @GetMapping("/export")
    public ResponseEntity<String> exportCsv() {
        String csv = searchService.exportAsCsv();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        headers.setContentDispositionFormData("attachment", "email-history-export.csv");
        return ResponseEntity.ok()
                .headers(headers)
                .body(csv);
    }

    /**
     * GET /api/history/search/facets/tones — Distinct tones.
     */
    @GetMapping("/facets/tones")
    public ResponseEntity<List<String>> getDistinctTones() {
        return ResponseEntity.ok(searchService.getDistinctTones());
    }

    /**
     * GET /api/history/search/facets/providers — Distinct providers.
     */
    @GetMapping("/facets/providers")
    public ResponseEntity<List<String>> getDistinctProviders() {
        return ResponseEntity.ok(searchService.getDistinctProviders());
    }

    /**
     * GET /api/history/search/facets/languages — Distinct languages.
     */
    @GetMapping("/facets/languages")
    public ResponseEntity<List<String>> getDistinctLanguages() {
        return ResponseEntity.ok(searchService.getDistinctLanguages());
    }
}
