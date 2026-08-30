package com.email.writer;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service that analyzes email content and assigns categories, confidence scores,
 * tags, sentiment, and urgency. Uses a multi-pass keyword/regex analysis approach.
 */
@Service
public class EmailCategorizationService {

    private final EmailCategoryRepository repository;

    public EmailCategorizationService(EmailCategoryRepository repository) {
        this.repository = repository;
    }

    // ---- Category keyword banks ----

    private static final Map<String, List<String>> CATEGORY_KEYWORDS = new LinkedHashMap<>();

    static {
        CATEGORY_KEYWORDS.put("INQUIRY", Arrays.asList(
                "question", "wondering", "could you", "can you", "would you",
                "asking", "seeking", "curious", "clarification", "explain",
                "how", "what", "when", "where", "who", "why", "details",
                "information", "help", "assist", "inquire", "enquiry"));
        CATEGORY_KEYWORDS.put("COMPLAINT", Arrays.asList(
                "complaint", "unhappy", "dissatisfied", "frustrated", "issue",
                "problem", "terrible", "awful", "worst", "unacceptable",
                "poor quality", "not working", "broken", "defect", "refund",
                "disappointed", "upset", "angry", "furious", "escalate",
                "manager", "supervisor", "compensation", "immediately"));
        CATEGORY_KEYWORDS.put("FOLLOW_UP", Arrays.asList(
                "follow up", "following up", "checking in", "just checking",
                "any update", "any news", "status", "reminder", "follow-up",
                " circling back", "wanted to check", "touching base",
                "heard back", "pending", "outstanding", "waiting"));
        CATEGORY_KEYWORDS.put("MEETING", Arrays.asList(
                "meeting", "schedule", "calendar", "available", "conference",
                "call", "discuss", "sync", "catch up", "appointment",
                "invite", "zoom", "teams", "location", "time slot",
                "agenda", "attend", "participant", "reschedule"));
        CATEGORY_KEYWORDS.put("APPRECIATION", Arrays.asList(
                "thank", "thanks", "grateful", "appreciate", "gratitude",
                "wonderful", "excellent", "great job", "well done",
                "impressed", "outstanding", "kudos", "bravo", "congrats",
                "congratulations", "magnificent", "superb"));
        CATEGORY_KEYWORDS.put("REQUEST", Arrays.asList(
                "request", "please send", "could you send", "need",
                "require", "as soon as possible", "asap", "urgent",
                "deadline", "by end of", "by tomorrow", "by monday",
                "share", "provide", "supply", "deliver", "submit"));
        CATEGORY_KEYWORDS.put("DECLINE", Arrays.asList(
                "decline", "unfortunately", "cannot attend", "unable",
                "regret", "apologize", "sorry", "not possible",
                "prior commitments", "conflict", "scheduling conflict",
                "turn down", "pass on", "opt out", "withdraw"));
        CATEGORY_KEYWORDS.put("INTRODUCTION", Arrays.asList(
                "introduce", "introduction", "new team", "joining",
                "pleased to meet", "nice to meet", "new role",
                "onboard", "onboarding", "background", "experience",
                "looking forward", "excited to", "happy to connect"));
        CATEGORY_KEYWORDS.put("FEEDBACK", Arrays.asList(
                "feedback", "suggestion", "recommend", "improve",
                "thoughts on", "opinion", "review", "evaluate",
                "assessment", "critique", "pros and cons", "advice",
                "consider", "propose", "alternatives"));
    }

    // ---- Urgency keywords ----

    private static final List<String> HIGH_URGENCY_KEYWORDS = Arrays.asList(
            "urgent", "asap", "immediately", "critical", "emergency",
            "deadline today", "time sensitive", "expires", "last chance",
            "overdue", "past due", "real quick", "right away");

    private static final List<String> MEDIUM_URGENCY_KEYWORDS = Arrays.asList(
            "by tomorrow", "by end of week", "end of day", "eod",
            "soon", "priority", "important", "timeframe", "尽快");

    // ---- Sentiment keywords ----

    private static final List<String> POSITIVE_SENTIMENT = Arrays.asList(
            "great", "excellent", "good", "amazing", "wonderful", "happy",
            "pleased", "thankful", "appreciate", "love", "fantastic",
            "awesome", "perfect", "brilliant", "outstanding", "superb",
            "well done", "congratulations", "excited", "thrilled");

    private static final List<String> NEGATIVE_SENTIMENT = Arrays.asList(
            "bad", "poor", "terrible", "awful", "hate", "angry",
            "frustrated", "disappointed", "unacceptable", "worst",
            "broken", "failed", "issue", "problem", "complaint",
            "unfortunately", "regret", "sorry", "not happy", "upset");

    // ---- Tag extraction patterns ----

    private static final Pattern DEADLINE_PATTERN = Pattern.compile(
            "(?i)(by\\s+(?:monday|tuesday|wednesday|thursday|friday|saturday|sunday)|" +
            "\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}|" +
            "\\b(?:january|february|march|april|may|june|july|august|september|october|november|december)\\s+\\d{1,2})");

    private static final Pattern ATTACHMENT_PATTERN = Pattern.compile(
            "(?i)(attached|attachment|enclosed|enclosure|see attached|find attached)");

    private static final Pattern QUESTION_PATTERN = Pattern.compile("\\?");

    // ---- Core Analysis ----

    /**
     * Categorizes email content and persists the result.
     */
    @Transactional
    public EmailCategorizationResult categorize(String emailContent) {
        if (emailContent == null || emailContent.trim().isEmpty()) {
            throw new IllegalArgumentException("Email content is required for categorization.");
        }

        String normalized = emailContent.toLowerCase().trim();
        int wordCount = normalized.split("\\s+").length;

        // Pass 1: Category scoring
        Map<String, Double> scores = scoreCategories(normalized);
        String primaryCategory = "OTHER";
        double maxScore = 0.0;
        String secondaryCategory = null;
        double secondMax = 0.0;

        for (Map.Entry<String, Double> entry : scores.entrySet()) {
            if (entry.getValue() > maxScore) {
                secondaryCategory = primaryCategory;
                secondMax = maxScore;
                primaryCategory = entry.getKey();
                maxScore = entry.getValue();
            } else if (entry.getValue() > secondMax && !entry.getKey().equals(primaryCategory)) {
                secondaryCategory = entry.getKey();
                secondMax = entry.getValue();
            }
        }

        double confidence = Math.min(maxScore, 1.0);
        if (confidence < 0.3) {
            secondaryCategory = primaryCategory;
            primaryCategory = "OTHER";
            confidence = Math.max(0.1, confidence);
        }

        // Pass 2: Sentiment
        String sentiment = analyzeSentiment(normalized);

        // Pass 3: Urgency
        String urgency = analyzeUrgency(normalized);

        // Pass 4: Tag extraction
        List<String> tags = extractTags(normalized);
        tags.add(0, primaryCategory.toLowerCase());

        // Pass 5: Metadata
        boolean containsQuestions = QUESTION_PATTERN.matcher(emailContent).find();
        boolean mentionsAttachments = ATTACHMENT_PATTERN.matcher(emailContent).find();
        boolean mentionsDeadline = DEADLINE_PATTERN.matcher(emailContent).find();
        String detectedTone = detectTone(normalized, sentiment);

        // Persist
        EmailCategory record = EmailCategory.builder()
                .originalContent(emailContent)
                .category(primaryCategory)
                .secondaryCategory(secondaryCategory)
                .confidence(confidence)
                .tags(String.join(",", tags))
                .sentiment(sentiment)
                .urgency(urgency)
                .detectedLanguage("English")
                .wordCount(wordCount)
                .containsQuestions(containsQuestions)
                .mentionsAttachments(mentionsAttachments)
                .mentionsDeadline(mentionsDeadline)
                .detectedTone(detectedTone)
                .build();

        record = repository.save(record);

        return EmailCategorizationResult.builder()
                .category(primaryCategory)
                .secondaryCategory(secondaryCategory)
                .confidence(confidence)
                .tags(tags)
                .sentiment(sentiment)
                .urgency(urgency)
                .detectedLanguage("English")
                .wordCount(wordCount)
                .containsQuestions(containsQuestions)
                .mentionsAttachments(mentionsAttachments)
                .mentionsDeadline(mentionsDeadline)
                .detectedTone(detectedTone)
                .explanation("Scored " + scores.size() + " categories; best match: "
                        + primaryCategory + " (" + String.format("%.2f", confidence) + ")")
                .persistedId(record.getId())
                .build();
    }

    /**
     * Categorizes without persisting (dry run).
     */
    public EmailCategorizationResult categorizeDryRun(String emailContent) {
        if (emailContent == null || emailContent.trim().isEmpty()) {
            throw new IllegalArgumentException("Email content is required for categorization.");
        }

        String normalized = emailContent.toLowerCase().trim();
        int wordCount = normalized.split("\\s+").length;

        Map<String, Double> scores = scoreCategories(normalized);
        String primaryCategory = "OTHER";
        double maxScore = 0.0;
        String secondaryCategory = null;

        for (Map.Entry<String, Double> entry : scores.entrySet()) {
            if (entry.getValue() > maxScore) {
                secondaryCategory = primaryCategory;
                primaryCategory = entry.getKey();
                maxScore = entry.getValue();
            }
        }

        double confidence = Math.min(maxScore, 1.0);
        if (confidence < 0.3) {
            secondaryCategory = primaryCategory;
            primaryCategory = "OTHER";
        }

        return EmailCategorizationResult.builder()
                .category(primaryCategory)
                .secondaryCategory(secondaryCategory)
                .confidence(confidence)
                .tags(extractTags(normalized))
                .sentiment(analyzeSentiment(normalized))
                .urgency(analyzeUrgency(normalized))
                .wordCount(wordCount)
                .containsQuestions(QUESTION_PATTERN.matcher(emailContent).find())
                .mentionsAttachments(ATTACHMENT_PATTERN.matcher(emailContent).find())
                .mentionsDeadline(DEADLINE_PATTERN.matcher(emailContent).find())
                .detectedTone(detectTone(normalized, analyzeSentiment(normalized)))
                .explanation("Dry run - not persisted")
                .build();
    }

    // ---- Category Scoring ----

    private Map<String, Double> scoreCategories(String normalized) {
        Map<String, Double> scores = new LinkedHashMap<>();
        String[] words = normalized.split("\\s+");

        for (Map.Entry<String, List<String>> entry : CATEGORY_KEYWORDS.entrySet()) {
            double score = 0.0;
            for (String keyword : entry.getValue()) {
                if (keyword.contains(" ")) {
                    if (normalized.contains(keyword)) {
                        score += 0.25;
                    }
                } else {
                    for (String word : words) {
                        if (word.equals(keyword)) {
                            score += 0.15;
                        } else if (word.startsWith(keyword)) {
                            score += 0.10;
                        }
                    }
                }
            }
            if (score > 0.0) {
                scores.put(entry.getKey(), Math.min(score, 1.0));
            }
        }
        return scores;
    }

    // ---- Sentiment Analysis ----

    private String analyzeSentiment(String normalized) {
        int positive = 0;
        int negative = 0;

        for (String word : POSITIVE_SENTIMENT) {
            if (normalized.contains(word)) positive++;
        }
        for (String word : NEGATIVE_SENTIMENT) {
            if (normalized.contains(word)) negative++;
        }

        if (positive > 0 && negative > 0 && Math.abs(positive - negative) <= 1) {
            return "MIXED";
        } else if (positive > negative) {
            return "POSITIVE";
        } else if (negative > positive) {
            return "NEGATIVE";
        }
        return "NEUTRAL";
    }

    // ---- Urgency Analysis ----

    private String analyzeUrgency(String normalized) {
        for (String kw : HIGH_URGENCY_KEYWORDS) {
            if (normalized.contains(kw)) return "HIGH";
        }
        for (String kw : MEDIUM_URGENCY_KEYWORDS) {
            if (normalized.contains(kw)) return "MEDIUM";
        }
        if (normalized.contains("please") || normalized.contains("need")) {
            return "LOW";
        }
        return "NONE";
    }

    // ---- Tag Extraction ----

    private List<String> extractTags(String normalized) {
        List<String> tags = new ArrayList<>();

        // Extract date-related tags
        Matcher deadlineMatcher = DEADLINE_PATTERN.matcher(normalized);
        while (deadlineMatcher.find()) {
            tags.add("deadline");
            break;
        }

        // Extract people references
        Pattern peoplePattern = Pattern.compile("(?i)\\b(dear|hi|hello|hey)\\s+([A-Z][a-z]+)");
        Matcher peopleMatcher = peoplePattern.matcher(
                normalized.substring(0, Math.min(200, normalized.length())));
        if (peopleMatcher.find()) {
            tags.add("personalized");
        }

        // Extract project/brand mentions (capitalized multi-word phrases)
        Pattern projectPattern = Pattern.compile("\\b([A-Z][a-z]+(?:\\s+[A-Z][a-z]+)+)\\b");
        Matcher projectMatcher = projectPattern.matcher(
                normalized.substring(0, Math.min(500, normalized.length())));
        while (projectMatcher.find() && tags.size() < 8) {
            String phrase = projectMatcher.group().toLowerCase();
            if (!tags.contains(phrase)) {
                tags.add(phrase);
            }
        }

        // Extract file type mentions
        Pattern filePattern = Pattern.compile("(?i)\\b(\\w+\\.(?:pdf|doc|docx|xlsx|xls|csv|ppt|txt|zip))\\b");
        Matcher fileMatcher = filePattern.matcher(normalized);
        while (fileMatcher.find() && tags.size() < 10) {
            tags.add(fileMatcher.group(1).toLowerCase());
        }

        // Extract action tags
        if (normalized.contains("please")) tags.add("action-required");
        if (normalized.contains("thank")) tags.add("gratitude");
        if (normalized.contains("invite") || normalized.contains("attend")) tags.add("event");
        if (normalized.contains("review") || normalized.contains("feedback")) tags.add("review");
        if (normalized.contains("update") || modifiedContains(normalized, "status")) tags.add("status-update");

        // Deduplicate and limit
        Set<String> unique = new LinkedHashSet<>(tags);
        return new ArrayList<>(unique).subList(0, Math.min(unique.size(), 10));
    }

    private boolean modifiedContains(String text, String word) {
        return text.contains(word);
    }

    // ---- Tone Detection ----

    private String detectTone(String normalized, String sentiment) {
        if (normalized.contains("dear") || normalized.contains("sincerely")
                || normalized.contains("regards")) {
            return "professional";
        }
        if (normalized.contains("hey") || normalized.contains("yo")
                || normalized.contains("lol") || normalized.contains("haha")) {
            return "casual";
        }
        if ("NEGATIVE".equals(sentiment) || "MIXED".equals(sentiment)) {
            if (normalized.contains("sorry") || normalized.contains("apologize")) {
                return "empathetic";
            }
            if (normalized.contains("urgent") || normalized.contains("immediately")) {
                return "urgent";
            }
        }
        if ("POSITIVE".equals(sentiment)) {
            if (normalized.contains("thank") || normalized.contains("appreciate")) {
                return "friendly";
            }
            if (normalized.contains("suggest") || normalized.contains("recommend")) {
                return "persuasive";
            }
        }
        return "professional";
    }

    // ---- Retrieval & Analytics ----

    /**
     * Returns all categorization records.
     */
    public List<EmailCategory> getAllCategories() {
        return repository.findAll();
    }

    /**
     * Returns records for a specific category.
     */
    public List<EmailCategory> getByCategory(String category) {
        return repository.findByCategoryOrderByCreatedAtDesc(category);
    }

    /**
     * Returns records with a specific sentiment.
     */
    public List<EmailCategory> getBySentiment(String sentiment) {
        return repository.findBySentimentOrderByCreatedAtDesc(sentiment);
    }

    /**
     * Returns records with a specific urgency.
     */
    public List<EmailCategory> getByUrgency(String urgency) {
        return repository.findByUrgencyOrderByCreatedAtDesc(urgency);
    }

    /**
     * Returns records matching a tag.
     */
    public List<EmailCategory> getByTag(String tag) {
        return repository.findByTag(tag);
    }

    /**
     * Returns high-confidence records for a category.
     */
    public List<EmailCategory> getHighConfidenceByCategory(String category) {
        return repository.findByCategoryWithMinConfidence(category, 0.7);
    }

    /**
     * Returns records within a date range.
     */
    public List<EmailCategory> getByDateRange(LocalDateTime start, LocalDateTime end) {
        return repository.findByCreatedAtBetweenOrderByCreatedAtDesc(start, end);
    }

    /**
     * Returns the 10 most recent records.
     */
    public List<EmailCategory> getRecent() {
        return repository.findTop10ByOrderByCreatedAtDesc();
    }

    /**
     * Returns records that have user labels.
     */
    public List<EmailCategory> getLabeled() {
        return repository.findByUserLabelIsNotNullOrderByCreatedAtDesc();
    }

    /**
     * Returns records by detected language.
     */
    public List<EmailCategory> getByLanguage(String language) {
        return repository.findByDetectedLanguageOrderByCreatedAtDesc(language);
    }

    /**
     * Builds a full analytics summary.
     */
    public Map<String, Object> getAnalytics() {
        long total = repository.count();
        Map<String, Object> analytics = new LinkedHashMap<>();
        analytics.put("totalCategorized", total);

        if (total == 0) {
            analytics.put("categoryBreakdown", new LinkedHashMap<>());
            analytics.put("sentimentBreakdown", new LinkedHashMap<>());
            analytics.put("urgencyBreakdown", new LinkedHashMap<>());
            analytics.put("averageConfidence", 0.0);
            analytics.put("highConfidenceRate", 0.0);
            analytics.put("actionableRate", 0.0);
            analytics.put("questionRate", 0.0);
            analytics.put("deadlineRate", 0.0);
            return analytics;
        }

        analytics.put("categoryBreakdown", toMap(repository.countByCategory()));
        analytics.put("sentimentBreakdown", toMap(repository.countBySentiment()));
        analytics.put("urgencyBreakdown", toMap(repository.countByUrgency()));

        Double avgConf = repository.averageConfidence();
        analytics.put("averageConfidence", avgConf != null ? Math.round(avgConf * 100.0) / 100.0 : 0.0);

        long highConf = repository.countHighConfidence();
        analytics.put("highConfidenceRate", Math.round(((double) highConf / total) * 10000.0) / 100.0);

        long actionable = repository.countActionable();
        analytics.put("actionableRate", Math.round(((double) actionable / total) * 10000.0) / 100.0);

        long withQuestions = repository.countByContainsQuestionsTrue();
        analytics.put("questionRate", Math.round(((double) withQuestions / total) * 10000.0) / 100.0);

        long withDeadlines = repository.countByMentionsDeadlineTrue();
        analytics.put("deadlineRate", Math.round(((double) withDeadlines / total) * 10000.0) / 100.0);

        return analytics;
    }

    /**
     * Updates the user label on a record.
     */
    @Transactional
    public EmailCategory updateLabel(Long id, String label) {
        EmailCategory record = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Category record not found: " + id));
        record.setUserLabel(label);
        return repository.save(record);
    }

    /**
     * Deletes a category record.
     */
    @Transactional
    public boolean deleteRecord(Long id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
            return true;
        }
        return false;
    }

    /**
     * Purges old records.
     */
    @Transactional
    public int purgeOld(int olderThanDays) {
        return repository.purgeOlderThan(LocalDateTime.now().minusDays(olderThanDays));
    }

    // ---- Helpers ----

    private static Map<String, Long> toMap(List<Object[]> rows) {
        Map<String, Long> map = new LinkedHashMap<>();
        for (Object[] row : rows) {
            map.put(String.valueOf(row[0]), row[1] instanceof Long ? (Long) row[1] : ((Number) row[1]).longValue());
        }
        return map;
    }
}
