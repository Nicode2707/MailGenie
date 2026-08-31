package com.email.writer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service layer providing advanced search, analytics, and export capabilities
 * for email generation history records.
 */
@Service
public class EmailHistorySearchService {

    private final EmailHistorySearchRepository searchRepository;

    public EmailHistorySearchService(EmailHistorySearchRepository searchRepository) {
        this.searchRepository = searchRepository;
    }

    /**
     * Executes a paginated search with the given criteria.
     * All criteria fields are optional — null fields are ignored.
     */
    public EmailHistorySearchResult search(EmailHistorySearchCriteria criteria) {
        if (criteria == null) {
            criteria = new EmailHistorySearchCriteria();
        }

        Sort sort = buildSort(criteria);
        Pageable pageable = PageRequest.of(criteria.effectivePage(), criteria.effectivePageSize(), sort);

        Page<EmailHistory> page;

        if (criteria.hasFilters()) {
            page = searchRepository.searchWithFilters(
                    criteria.getQuery(),
                    criteria.getTone(),
                    criteria.getProvider(),
                    criteria.getLanguage(),
                    criteria.getCreatedAfter(),
                    criteria.getCreatedBefore(),
                    pageable
            );
        } else if (criteria.getQuery() != null && !criteria.getQuery().trim().isEmpty()) {
            page = searchRepository.searchByQuery(criteria.getQuery(), pageable);
        } else {
            page = searchRepository.findAll(pageable);
        }

        return EmailHistorySearchResult.builder()
                .records(page.getContent())
                .totalRecords(page.getTotalElements())
                .currentPage(page.getNumber())
                .pageSize(page.getSize())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .empty(page.isEmpty())
                .build();
    }

    /**
     * Returns a full analytics summary of email history.
     */
    public EmailHistoryAnalytics getAnalytics() {
        long total = searchRepository.count();

        if (total == 0) {
            return EmailHistoryAnalytics.empty();
        }

        Double avgReplyLen = searchRepository.averageReplyLength();
        Double avgOrigLen = searchRepository.averageOriginalLength();
        long commentsCount = searchRepository.countWithComments();

        List<Object[]> toneCounts = searchRepository.countByTone();
        List<Object[]> providerCounts = searchRepository.countByProvider();
        List<Object[]> languageCounts = searchRepository.countByLanguage();
        List<Object[]> dailyCounts = searchRepository.countByDay();

        return EmailHistoryAnalytics.builder()
                .totalEmails(total)
                .averageReplyLength(avgReplyLen != null ? Math.round(avgReplyLen * 100.0) / 100.0 : 0.0)
                .averageOriginalLength(avgOrigLen != null ? Math.round(avgOrigLen * 100.0) / 100.0 : 0.0)
                .toneBreakdown(toMap(toneCounts))
                .providerBreakdown(toMap(providerCounts))
                .languageBreakdown(toMap(languageCounts))
                .dailyVolume(toMap(dailyCounts))
                .emailsWithComments(commentsCount)
                .commentRate(Math.round(((double) commentsCount / total) * 10000.0) / 100.0)
                .build();
    }

    /**
     * Returns analytics scoped to a specific date range.
     */
    public EmailHistoryAnalytics getAnalyticsForRange(LocalDateTime start, LocalDateTime end) {
        EmailHistorySearchCriteria range = new EmailHistorySearchCriteria();
        range.setCreatedAfter(start);
        range.setCreatedBefore(end);
        range.setPageSize(1000);

        EmailHistorySearchResult result = search(range);
        List<EmailHistory> records = result.getRecords();

        if (records.isEmpty()) {
            return EmailHistoryAnalytics.empty();
        }

        long total = records.size();
        double avgReply = records.stream()
                .filter(h -> h.getGeneratedReply() != null)
                .mapToInt(h -> h.getGeneratedReply().length())
                .average().orElse(0.0);
        double avgOrig = records.stream()
                .filter(h -> h.getOriginalContent() != null)
                .mapToInt(h -> h.getOriginalContent().length())
                .average().orElse(0.0);
        long commentsCount = records.stream()
                .filter(h -> h.getUserComment() != null && !h.getUserComment().trim().isEmpty())
                .count();

        Map<String, Long> toneBreakdown = records.stream()
                .filter(h -> h.getTone() != null)
                .collect(Collectors.groupingBy(EmailHistory::getTone, Collectors.counting()));
        Map<String, Long> providerBreakdown = records.stream()
                .filter(h -> h.getProvider() != null)
                .collect(Collectors.groupingBy(EmailHistory::getProvider, Collectors.counting()));
        Map<String, Long> languageBreakdown = records.stream()
                .filter(h -> h.getLanguage() != null)
                .collect(Collectors.groupingBy(EmailHistory::getLanguage, Collectors.counting()));

        return EmailHistoryAnalytics.builder()
                .totalEmails(total)
                .averageReplyLength(Math.round(avgReply * 100.0) / 100.0)
                .averageOriginalLength(Math.round(avgOrig * 100.0) / 100.0)
                .toneBreakdown(toneBreakdown)
                .providerBreakdown(providerBreakdown)
                .languageBreakdown(languageBreakdown)
                .dailyVolume(new LinkedHashMap<>())
                .emailsWithComments(commentsCount)
                .commentRate(Math.round(((double) commentsCount / total) * 10000.0) / 100.0)
                .build();
    }

    /**
     * Exports all email history records as CSV-formatted text.
     */
    public String exportAsCsv() {
        List<EmailHistory> records = searchRepository.findAllForExport();
        StringBuilder csv = new StringBuilder();
        csv.append("ID,Created At,Tone,Provider,Language,Original Content,Generated Reply,User Comment\n");

        for (EmailHistory h : records) {
            csv.append(escapeCsvField(String.valueOf(h.getId()))).append(",");
            csv.append(escapeCsvField(h.getCreatedAt() != null ? h.getCreatedAt().toString() : "")).append(",");
            csv.append(escapeCsvField(h.getTone())).append(",");
            csv.append(escapeCsvField(h.getProvider())).append(",");
            csv.append(escapeCsvField(h.getLanguage())).append(",");
            csv.append(escapeCsvField(h.getOriginalContent())).append(",");
            csv.append(escapeCsvField(h.getGeneratedReply())).append(",");
            csv.append(escapeCsvField(h.getUserComment())).append("\n");
        }

        return csv.toString();
    }

    /**
     * Returns the distinct list of tones used across all history records.
     */
    public List<String> getDistinctTones() {
        return searchRepository.countByTone().stream()
                .map(row -> (String) row[0])
                .filter(t -> t != null && !t.trim().isEmpty())
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Returns the distinct list of providers used across all history records.
     */
    public List<String> getDistinctProviders() {
        return searchRepository.countByProvider().stream()
                .map(row -> (String) row[0])
                .filter(p -> p != null && !p.trim().isEmpty())
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Returns the distinct list of languages used across all history records.
     */
    public List<String> getDistinctLanguages() {
        return searchRepository.countByLanguage().stream()
                .map(row -> (String) row[0])
                .filter(l -> l != null && !l.trim().isEmpty())
                .sorted()
                .collect(Collectors.toList());
    }

    // ---- Internal helpers ----

    private Sort buildSort(EmailHistorySearchCriteria criteria) {
        String field = criteria.effectiveSortBy();
        String dir = criteria.getSortDirection();
        Sort.Direction direction = "asc".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(direction, field);
    }

    private static Map<String, Long> toMap(List<Object[]> rows) {
        Map<String, Long> map = new LinkedHashMap<>();
        for (Object[] row : rows) {
            String key = row[0] != null ? String.valueOf(row[0]) : "unknown";
            Long value = row[1] instanceof Long ? (Long) row[1] : ((Number) row[1]).longValue();
            map.put(key, value);
        }
        return map;
    }

    private static String escapeCsvField(String value) {
        if (value == null) return "";
        String cleaned = value.replace("\r\n", " ").replace("\n", " ").replace("\r", " ");
        if (cleaned.contains(",") || cleaned.contains("\"") || cleaned.contains("\n")) {
            return "\"" + cleaned.replace("\"", "\"\"") + "\"";
        }
        return cleaned;
    }
}
