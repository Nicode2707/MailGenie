package com.email.writer;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Analytics summary DTO aggregating usage statistics across email history records.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailHistoryAnalytics {

    /**
     * Total number of generated emails.
     */
    private long totalEmails;

    /**
     * Average length of generated replies in characters.
     */
    private double averageReplyLength;

    /**
     * Average length of original email content in characters.
     */
    private double averageOriginalLength;

    /**
     * Count of emails grouped by tone.
     */
    private Map<String, Long> toneBreakdown;

    /**
     * Count of emails grouped by provider.
     */
    private Map<String, Long> providerBreakdown;

    /**
     * Count of emails grouped by language.
     */
    private Map<String, Long> languageBreakdown;

    /**
     * Number of emails created per day in the last 30 days.
     */
    private Map<String, Long> dailyVolume;

    /**
     * Number of emails with a user comment attached.
     */
    private long emailsWithComments;

    /**
     * Percentage of emails that have comments.
     */
    private double commentRate;

    /**
     * Returns a default empty analytics object.
     */
    public static EmailHistoryAnalytics empty() {
        return EmailHistoryAnalytics.builder()
                .totalEmails(0)
                .averageReplyLength(0.0)
                .averageOriginalLength(0.0)
                .toneBreakdown(new LinkedHashMap<>())
                .providerBreakdown(new LinkedHashMap<>())
                .languageBreakdown(new LinkedHashMap<>())
                .dailyVolume(new LinkedHashMap<>())
                .emailsWithComments(0)
                .commentRate(0.0)
                .build();
    }
}
