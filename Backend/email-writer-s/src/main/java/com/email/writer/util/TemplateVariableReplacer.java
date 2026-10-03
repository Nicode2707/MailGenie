package com.email.writer.util;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for parsing and replacing template variables using Mustache-style {{variable}} syntax
 * and supporting legacy [Placeholder] notations.
 */
public class TemplateVariableReplacer {

    private static final Pattern MUSTACHE_PATTERN = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_-]+)\\s*\\}\\}");
    private static final Pattern BRACKET_PATTERN = Pattern.compile("\\[\\s*([a-zA-Z0-9_ -]+)\\s*\\]");

    /**
     * Replaces variable placeholders in a template string with values from the context map.
     * Case-insensitive matching is supported across common variable aliases.
     *
     * @param template the raw template string
     * @param variables the context map of key-value pairs
     * @return the rendered template with variables replaced
     */
    public static String replaceVariables(String template, Map<String, String> variables) {
        if (template == null || template.isEmpty()) {
            return "";
        }
        if (variables == null || variables.isEmpty()) {
            return template;
        }

        // Build case-insensitive lookup map
        Map<String, String> normalizedMap = new java.util.HashMap<>();
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            if (entry.getKey() != null) {
                normalizedMap.put(entry.getKey().toLowerCase().replaceAll("[\\s_-]+", ""), entry.getValue());
            }
        }

        // 1. Process mustache-style placeholders: {{var}}
        Matcher mustacheMatcher = MUSTACHE_PATTERN.matcher(template);
        StringBuffer sb = new StringBuffer();
        while (mustacheMatcher.find()) {
            String varName = mustacheMatcher.group(1);
            String normalizedKey = varName.toLowerCase().replaceAll("[\\s_-]+", "");
            if (normalizedMap.containsKey(normalizedKey)) {
                String replacement = normalizedMap.get(normalizedKey);
                mustacheMatcher.appendReplacement(sb, Matcher.quoteReplacement(replacement != null ? replacement : ""));
            } else {
                mustacheMatcher.appendReplacement(sb, Matcher.quoteReplacement(mustacheMatcher.group(0)));
            }
        }
        mustacheMatcher.appendTail(sb);
        String intermediate = sb.toString();

        // 2. Process legacy bracket placeholders: [Name], [Topic], etc.
        Matcher bracketMatcher = BRACKET_PATTERN.matcher(intermediate);
        StringBuffer sb2 = new StringBuffer();
        while (bracketMatcher.find()) {
            String varName = bracketMatcher.group(1);
            String normalizedKey = varName.toLowerCase().replaceAll("[\\s_-]+", "");
            if (normalizedMap.containsKey(normalizedKey)) {
                String replacement = normalizedMap.get(normalizedKey);
                bracketMatcher.appendReplacement(sb2, Matcher.quoteReplacement(replacement != null ? replacement : ""));
            } else {
                bracketMatcher.appendReplacement(sb2, Matcher.quoteReplacement(bracketMatcher.group(0)));
            }
        }
        bracketMatcher.appendTail(sb2);

        return sb2.toString();
    }
}
