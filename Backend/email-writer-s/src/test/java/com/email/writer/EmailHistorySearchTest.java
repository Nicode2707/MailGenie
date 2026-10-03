package com.email.writer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EmailHistorySearchService covering search, analytics,
 * export, and facet retrieval functionality.
 */
@ExtendWith(MockitoExtension.class)
class EmailHistorySearchServiceTest {

    @Mock
    private EmailHistorySearchRepository searchRepository;

    @InjectMocks
    private EmailHistorySearchService searchService;

    private EmailHistory sampleRecord;

    @BeforeEach
    void setUp() {
        sampleRecord = EmailHistory.builder()
                .id(1L)
                .originalContent("Hello, can you help me with the project?")
                .generatedReply("Sure, I'd be happy to help with the project.")
                .tone("professional")
                .provider("groq")
                .language("English")
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ---- Search Tests ----

    @Test
    void search_noCriteria_returnsAll() {
        Page<EmailHistory> page = new PageImpl<>(List.of(sampleRecord));
        when(searchRepository.findAll(any(Pageable.class))).thenReturn(page);

        EmailHistorySearchResult result = searchService.search(null);

        assertEquals(1, result.getRecords().size());
        assertEquals(1, result.getTotalRecords());
        assertFalse(result.isEmpty());
    }

    @Test
    void search_withQuery_filtersByContent() {
        Page<EmailHistory> page = new PageImpl<>(List.of(sampleRecord));
        when(searchRepository.searchWithFilters(
                eq("project"), isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(page);

        EmailHistorySearchCriteria criteria = new EmailHistorySearchCriteria();
        criteria.setQuery("project");

        EmailHistorySearchResult result = searchService.search(criteria);
        assertEquals(1, result.getRecords().size());
    }

    @Test
    void search_withToneFilter_appliesFilter() {
        Page<EmailHistory> page = new PageImpl<>(List.of(sampleRecord));
        when(searchRepository.searchWithFilters(
                isNull(), eq("professional"), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(page);

        EmailHistorySearchCriteria criteria = new EmailHistorySearchCriteria();
        criteria.setTone("professional");

        EmailHistorySearchResult result = searchService.search(criteria);
        assertEquals(1, result.getRecords().size());
    }

    @Test
    void search_withProviderFilter_appliesFilter() {
        Page<EmailHistory> page = new PageImpl<>(List.of(sampleRecord));
        when(searchRepository.searchWithFilters(
                isNull(), isNull(), eq("groq"), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(page);

        EmailHistorySearchCriteria criteria = new EmailHistorySearchCriteria();
        criteria.setProvider("groq");

        EmailHistorySearchResult result = searchService.search(criteria);
        assertEquals(1, result.getRecords().size());
    }

    @Test
    void search_withDateRange_appliesDateFilter() {
        Page<EmailHistory> page = new PageImpl<>(List.of(sampleRecord));
        LocalDateTime start = LocalDateTime.now().minusDays(7);
        LocalDateTime end = LocalDateTime.now();

        when(searchRepository.searchWithFilters(
                isNull(), isNull(), isNull(), isNull(), eq(start), eq(end), any(Pageable.class)))
                .thenReturn(page);

        EmailHistorySearchCriteria criteria = new EmailHistorySearchCriteria();
        criteria.setCreatedAfter(start);
        criteria.setCreatedBefore(end);

        EmailHistorySearchResult result = searchService.search(criteria);
        assertEquals(1, result.getRecords().size());
    }

    @Test
    void search_withMultipleFilters_combinesAll() {
        Page<EmailHistory> page = new PageImpl<>(List.of(sampleRecord));
        LocalDateTime start = LocalDateTime.now().minusDays(30);
        LocalDateTime end = LocalDateTime.now();

        when(searchRepository.searchWithFilters(
                eq("test"), eq("casual"), eq("openai"), eq("French"), eq(start), eq(end), any(Pageable.class)))
                .thenReturn(page);

        EmailHistorySearchCriteria criteria = new EmailHistorySearchCriteria();
        criteria.setQuery("test");
        criteria.setTone("casual");
        criteria.setProvider("openai");
        criteria.setLanguage("French");
        criteria.setCreatedAfter(start);
        criteria.setCreatedBefore(end);

        EmailHistorySearchResult result = searchService.search(criteria);
        assertEquals(1, result.getRecords().size());
    }

    @Test
    void search_emptyResult_returnsEmptyResult() {
        Page<EmailHistory> emptyPage = new PageImpl<>(Collections.emptyList());
        when(searchRepository.findAll(any(Pageable.class))).thenReturn(emptyPage);

        EmailHistorySearchResult result = searchService.search(new EmailHistorySearchCriteria());
        assertTrue(result.isEmpty());
        assertEquals(0, result.getTotalRecords());
    }

    @Test
    void search_resultPaginationMetadata_correct() {
        List<EmailHistory> records = List.of(sampleRecord);
        Page<EmailHistory> page = new PageImpl<>(records, org.springframework.data.domain.PageRequest.of(1, 10), 25);
        when(searchRepository.findAll(any(Pageable.class))).thenReturn(page);

        EmailHistorySearchResult result = searchService.search(new EmailHistorySearchCriteria());
        assertEquals(1, result.getCurrentPage());
        assertEquals(10, result.getPageSize());
        assertEquals(3, result.getTotalPages());
        assertFalse(result.isFirst());
        assertFalse(result.isLast());
    }

    // ---- Analytics Tests ----

    @Test
    void getAnalytics_noRecords_returnsEmpty() {
        when(searchRepository.count()).thenReturn(0L);

        EmailHistoryAnalytics analytics = searchService.getAnalytics();
        assertEquals(0, analytics.getTotalEmails());
        assertTrue(analytics.getToneBreakdown().isEmpty());
    }

    @Test
    void getAnalytics_withRecords_returnsAggregates() {
        when(searchRepository.count()).thenReturn(10L);
        when(searchRepository.averageReplyLength()).thenReturn(250.5);
        when(searchRepository.averageOriginalLength()).thenReturn(120.3);
        when(searchRepository.countWithComments()).thenReturn(3L);
        when(searchRepository.countByTone()).thenReturn(List.of(
                new Object[]{"professional", 6L}, new Object[]{"casual", 4L}));
        when(searchRepository.countByProvider()).thenReturn(List.of(
                new Object[]{"groq", 8L}, new Object[]{"openai", 2L}));
        when(searchRepository.countByLanguage()).thenReturn(List.of(
                new Object[]{"English", 9L}, new Object[]{"French", 1L}));
        when(searchRepository.countByDay()).thenReturn(List.of(
                new Object[]{"2026-08-30", 5L}, new Object[]{"2026-08-29", 5L}));

        EmailHistoryAnalytics analytics = searchService.getAnalytics();
        assertEquals(10, analytics.getTotalEmails());
        assertEquals(250.5, analytics.getAverageReplyLength());
        assertEquals(120.3, analytics.getAverageOriginalLength());
        assertEquals(6L, analytics.getToneBreakdown().get("professional"));
        assertEquals(8L, analytics.getProviderBreakdown().get("groq"));
        assertEquals(30.0, analytics.getCommentRate());
    }

    @Test
    void getAnalyticsForRange_filtersByDate() {
        LocalDateTime start = LocalDateTime.now().minusDays(7);
        LocalDateTime end = LocalDateTime.now();
        Page<EmailHistory> page = new PageImpl<>(List.of(sampleRecord));
        when(searchRepository.searchWithFilters(
                isNull(), isNull(), isNull(), isNull(), eq(start), eq(end), any(Pageable.class)))
                .thenReturn(page);

        EmailHistoryAnalytics analytics = searchService.getAnalyticsForRange(start, end);
        assertEquals(1, analytics.getTotalEmails());
    }

    @Test
    void getAnalyticsForRange_emptyRange_returnsEmpty() {
        LocalDateTime start = LocalDateTime.now().minusDays(30);
        LocalDateTime end = LocalDateTime.now().minusDays(20);
        Page<EmailHistory> emptyPage = new PageImpl<>(Collections.emptyList());
        when(searchRepository.searchWithFilters(
                isNull(), isNull(), isNull(), isNull(), eq(start), eq(end), any(Pageable.class)))
                .thenReturn(emptyPage);

        EmailHistoryAnalytics analytics = searchService.getAnalyticsForRange(start, end);
        assertEquals(0, analytics.getTotalEmails());
    }

    // ---- CSV Export Tests ----

    @Test
    void exportAsCsv_returnsCorrectFormat() {
        when(searchRepository.findAllForExport()).thenReturn(List.of(sampleRecord));

        String csv = searchService.exportAsCsv();
        assertTrue(csv.startsWith("ID,Created At,Tone,Provider,Language,"));
        assertTrue(csv.contains("professional"));
        assertTrue(csv.contains("groq"));
        assertTrue(csv.contains("English"));
    }

    @Test
    void exportAsCsv_emptyHistory_returnsHeaderOnly() {
        when(searchRepository.findAllForExport()).thenReturn(Collections.emptyList());

        String csv = searchService.exportAsCsv();
        String[] lines = csv.split("\n");
        assertEquals(1, lines.length); // Only header
        assertTrue(lines[0].contains("ID"));
    }

    @Test
    void exportAsCsv_escapesFieldsWithCommas() {
        EmailHistory recordWithComma = EmailHistory.builder()
                .id(2L)
                .originalContent("Hello, world, this is a test")
                .generatedReply("Hi there")
                .tone("casual")
                .provider("openai")
                .language("English")
                .createdAt(LocalDateTime.now())
                .build();
        when(searchRepository.findAllForExport()).thenReturn(List.of(recordWithComma));

        String csv = searchService.exportAsCsv();
        // The field with commas should be quoted
        assertTrue(csv.contains("\"Hello, world, this is a test\""));
    }

    @Test
    void exportAsCsv_escapesFieldsWithQuotes() {
        EmailHistory recordWithQuotes = EmailHistory.builder()
                .id(3L)
                .originalContent("He said \"hello\"")
                .generatedReply("Hi back")
                .tone("friendly")
                .provider("groq")
                .language("English")
                .createdAt(LocalDateTime.now())
                .build();
        when(searchRepository.findAllForExport()).thenReturn(List.of(recordWithQuotes));

        String csv = searchService.exportAsCsv();
        assertTrue(csv.contains("\"He said \"\"hello\"\"\""));
    }

    @Test
    void exportAsCsv_nullFields_handledGracefully() {
        EmailHistory recordWithNulls = EmailHistory.builder()
                .id(4L)
                .originalContent("Content")
                .generatedReply("Reply")
                .tone(null)
                .provider(null)
                .language(null)
                .createdAt(null)
                .build();
        when(searchRepository.findAllForExport()).thenReturn(List.of(recordWithNulls));

        String csv = searchService.exportAsCsv();
        String[] lines = csv.split("\n");
        assertEquals(2, lines.length); // header + 1 record
    }

    // ---- Facet Tests ----

    @Test
    void getDistinctTones_returnsSorted() {
        when(searchRepository.countByTone()).thenReturn(List.of(
                new Object[]{"casual", 4L}, new Object[]{"professional", 6L}));

        List<String> tones = searchService.getDistinctTones();
        assertEquals(2, tones.size());
        assertEquals("casual", tones.get(0));
        assertEquals("professional", tones.get(1));
    }

    @Test
    void getDistinctProviders_returnsSorted() {
        when(searchRepository.countByProvider()).thenReturn(List.of(
                new Object[]{"groq", 8L}, new Object[]{"openai", 2L}));

        List<String> providers = searchService.getDistinctProviders();
        assertEquals(2, providers.size());
        assertEquals("groq", providers.get(0));
    }

    @Test
    void getDistinctLanguages_returnsSorted() {
        when(searchRepository.countByLanguage()).thenReturn(List.of(
                new Object[]{"English", 9L}, new Object[]{"French", 1L}));

        List<String> languages = searchService.getDistinctLanguages();
        assertEquals(2, languages.size());
        assertEquals("English", languages.get(0));
    }

    @Test
    void getDistinctTones_filtersEmpty() {
        when(searchRepository.countByTone()).thenReturn(List.of(
                new Object[]{"", 1L}, new Object[]{"professional", 6L}));

        List<String> tones = searchService.getDistinctTones();
        assertEquals(1, tones.size());
        assertEquals("professional", tones.get(0));
    }

    // ---- Criteria Validation Tests ----

    @Test
    void searchCriteria_effectivePageSize_capsAt100() {
        EmailHistorySearchCriteria criteria = new EmailHistorySearchCriteria();
        criteria.setPageSize(500);
        assertEquals(100, criteria.effectivePageSize());
    }

    @Test
    void searchCriteria_effectivePageSize_minimumIsOne() {
        EmailHistorySearchCriteria criteria = new EmailHistorySearchCriteria();
        criteria.setPageSize(-5);
        assertEquals(1, criteria.effectivePageSize());
    }

    @Test
    void searchCriteria_effectivePage_floorsAtZero() {
        EmailHistorySearchCriteria criteria = new EmailHistorySearchCriteria();
        criteria.setPage(-3);
        assertEquals(0, criteria.effectivePage());
    }

    @Test
    void searchCriteria_effectiveSortBy_invalidField_fallsBackToCreatedAt() {
        EmailHistorySearchCriteria criteria = new EmailHistorySearchCriteria();
        criteria.setSortBy("nonexistentField");
        assertEquals("createdAt", criteria.effectiveSortBy());
    }

    @Test
    void searchCriteria_effectiveSortBy_validField_returnsField() {
        EmailHistorySearchCriteria criteria = new EmailHistorySearchCriteria();
        criteria.setSortBy("tone");
        assertEquals("tone", criteria.effectiveSortBy());
    }

    @Test
    void searchCriteria_hasFilters_trueWhenQuerySet() {
        EmailHistorySearchCriteria criteria = new EmailHistorySearchCriteria();
        criteria.setQuery("hello");
        assertTrue(criteria.hasFilters());
    }

    @Test
    void searchCriteria_hasFilters_falseWhenAllNull() {
        EmailHistorySearchCriteria criteria = new EmailHistorySearchCriteria();
        assertFalse(criteria.hasFilters());
    }

    // ---- Analytics empty defaults ----

    @Test
    void analyticsEmpty_returnsZeroDefaults() {
        EmailHistoryAnalytics empty = EmailHistoryAnalytics.empty();
        assertEquals(0, empty.getTotalEmails());
        assertEquals(0.0, empty.getAverageReplyLength());
        assertNotNull(empty.getToneBreakdown());
        assertNotNull(empty.getProviderBreakdown());
        assertNotNull(empty.getLanguageBreakdown());
        assertNotNull(empty.getDailyVolume());
    }
}
