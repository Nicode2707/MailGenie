package com.email.writer.util;

import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TemplateVariableReplacerTest {

    @Test
    void testMustacheVariableReplacement() {
        String template = "Hello {{recipient_name}},\n\nWelcome to {{company}}! Your trial ends on {{end_date}}.";
        Map<String, String> vars = new HashMap<>();
        vars.put("recipient_name", "Sarah");
        vars.put("company", "TechCorp");
        vars.put("end_date", "October 1st");

        String rendered = TemplateVariableReplacer.replaceVariables(template, vars);

        assertEquals("Hello Sarah,\n\nWelcome to TechCorp! Your trial ends on October 1st.", rendered);
    }

    @Test
    void testCaseInsensitiveAndAliasMatching() {
        String template = "Dear {{Name}}, regarding {{Project_Topic}} from {{Sender}}.";
        Map<String, String> vars = new HashMap<>();
        vars.put("name", "Bob");
        vars.put("projecttopic", "AI Migration");
        vars.put("sender_name", "Alice"); // matching via normalized alias if sender key is used
        vars.put("sender", "Alice Smith");

        String rendered = TemplateVariableReplacer.replaceVariables(template, vars);

        assertEquals("Dear Bob, regarding AI Migration from Alice Smith.", rendered);
    }

    @Test
    void testLegacyBracketPlaceholderReplacement() {
        String template = "Hi [Name], please review [Document] by [Due Date].";
        Map<String, String> vars = new HashMap<>();
        vars.put("name", "John");
        vars.put("document", "Security Audit");
        vars.put("due_date", "Friday");

        String rendered = TemplateVariableReplacer.replaceVariables(template, vars);

        assertEquals("Hi John, please review Security Audit by Friday.", rendered);
    }

    @Test
    void testMissingVariablesPreservePlaceholders() {
        String template = "Hello {{name}}, your balance is {{balance}}.";
        Map<String, String> vars = new HashMap<>();
        vars.put("name", "David");

        String rendered = TemplateVariableReplacer.replaceVariables(template, vars);

        assertEquals("Hello David, your balance is {{balance}}.", rendered);
    }

    @Test
    void testNullAndEmptyInputs() {
        assertEquals("", TemplateVariableReplacer.replaceVariables(null, Map.of("a", "b")));
        assertEquals("", TemplateVariableReplacer.replaceVariables("", Map.of("a", "b")));
        assertEquals("Hello {{name}}", TemplateVariableReplacer.replaceVariables("Hello {{name}}", null));
        assertEquals("Hello {{name}}", TemplateVariableReplacer.replaceVariables("Hello {{name}}", Map.of()));
    }
}
