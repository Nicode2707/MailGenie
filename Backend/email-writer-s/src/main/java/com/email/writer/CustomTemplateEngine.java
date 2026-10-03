package com.email.writer;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CustomTemplateEngine {

    /**
     * Very basic XSS sanitization (encoding HTML entities).
     * For production, a robust library like OWASP Java HTML Sanitizer is recommended.
     */
    public String sanitize(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#x27;");
    }

    /**
     * Replaces {{variableName}} with the corresponding sanitized value from the map.
     */
    public String compile(String template, Map<String, String> variables) {
        if (template == null || template.isEmpty()) return "";
        if (variables == null || variables.isEmpty()) return template;

        String result = template;
        Pattern pattern = Pattern.compile("\\{\\{([^}]+)\\}\\}");
        Matcher matcher = pattern.matcher(template);

        while (matcher.find()) {
            String key = matcher.group(1).trim();
            String rawValue = variables.getOrDefault(key, "");
            String sanitizedValue = sanitize(rawValue);
            result = result.replace("{{" + matcher.group(1) + "}}", sanitizedValue);
        }
        return result;
    }
}
