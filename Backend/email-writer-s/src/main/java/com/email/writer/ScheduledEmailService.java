package com.email.writer;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service layer managing the full lifecycle of scheduled emails.
 * Provides CRUD operations, rescheduling, bulk operations, and
 * processing pipeline orchestration.
 */
@Service
public class ScheduledEmailService {

    private final ScheduledEmailRepository repository;
    private final EmailGeneratorService emailGeneratorService;
    private final ApiRequestMetricService apiRequestMetricService;

    public ScheduledEmailService(ScheduledEmailRepository repository,
                                 EmailGeneratorService emailGeneratorService,
                                 ApiRequestMetricService apiRequestMetricService) {
        this.repository = repository;
        this.emailGeneratorService = emailGeneratorService;
        this.apiRequestMetricService = apiRequestMetricService;
    }

    /**
     * Creates a new scheduled email from the given request.
     */
    @Transactional
    public ScheduledEmail createScheduledEmail(ScheduledEmailRequest request) {
        if (!request.isValid()) {
            throw new IllegalArgumentException(
                    "Invalid scheduled email request: recipientEmail, emailContent, " +
                    "and a future scheduledAt time are required.");
        }

        ScheduledEmail email = ScheduledEmail.builder()
                .recipientEmail(request.getRecipientEmail())
                .recipientName(request.getRecipientName() != null ? request.getRecipientName() : "")
                .emailContent(request.getEmailContent())
                .tone(request.getTone())
                .language(request.getLanguage())
                .provider(request.getProvider())
                .model(request.getModel())
                .customInstructions(request.getCustomInstructions())
                .composeMode(request.isComposeMode())
                .scheduledAt(request.getScheduledAt())
                .label(request.getLabel())
                .maxAttempts(Math.max(1, request.getMaxAttempts()))
                .autoSend(request.isAutoSend())
                .status("PENDING")
                .attempts(0)
                .build();

        return repository.save(email);
    }

    /**
     * Retrieves a scheduled email by its ID.
     */
    public ScheduledEmail getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(
                        "Scheduled email not found with id: " + id));
    }

    /**
     * Returns all scheduled emails, ordered by scheduled time descending.
     */
    public List<ScheduledEmail> getAllScheduledEmails() {
        return repository.findAll(SortBy.scheduledAtDesc());
    }

    /**
     * Returns all emails with a given status.
     */
    public List<ScheduledEmail> getByStatus(String status) {
        return repository.findByStatusOrderByScheduledAtAsc(status);
    }

    /**
     * Returns emails filtered by label.
     */
    public List<ScheduledEmail> getByLabel(String label) {
        return repository.findByLabelOrderByScheduledAtDesc(label);
    }

    /**
     * Returns emails scheduled within a date range.
     */
    public List<ScheduledEmail> getByDateRange(LocalDateTime start, LocalDateTime end) {
        return repository.findByCreatedAtBetweenOrderByCreatedAtDesc(start, end);
    }

    /**
     * Cancels a pending scheduled email. Only PENDING emails can be cancelled.
     */
    @Transactional
    public ScheduledEmail cancelEmail(Long id) {
        ScheduledEmail email = getById(id);
        if (!email.cancel()) {
            throw new IllegalStateException(
                    "Cannot cancel email with status: " + email.getStatus()
                    + ". Only PENDING emails can be cancelled.");
        }
        return repository.save(email);
    }

    /**
     * Bulk cancels all pending emails.
     */
    @Transactional
    public int cancelAllPending() {
        List<ScheduledEmail> pending = repository.findByStatusOrderByScheduledAtAsc("PENDING");
        int count = 0;
        for (ScheduledEmail email : pending) {
            email.cancel();
            repository.save(email);
            count++;
        }
        return count;
    }

    /**
     * Reschedules a pending email to a new future time.
     */
    @Transactional
    public ScheduledEmail rescheduleEmail(Long id, LocalDateTime newScheduledAt) {
        if (newScheduledAt == null || newScheduledAt.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("New scheduled time must be in the future.");
        }

        ScheduledEmail email = getById(id);
        if (!"PENDING".equals(email.getStatus())) {
            throw new IllegalStateException(
                    "Cannot reschedule email with status: " + email.getStatus());
        }

        email.setScheduledAt(newScheduledAt);
        return repository.save(email);
    }

    /**
     * Updates the label of a scheduled email.
     */
    @Transactional
    public ScheduledEmail updateLabel(Long id, String newLabel) {
        ScheduledEmail email = getById(id);
        email.setLabel(newLabel);
        return repository.save(email);
    }

    /**
     * Returns a summary map of email counts by status.
     */
    public Map<String, Long> getStatusSummary() {
        List<Object[]> counts = repository.countByStatus();
        Map<String, Long> summary = new LinkedHashMap<>();
        summary.put("PENDING", 0L);
        summary.put("PROCESSING", 0L);
        summary.put("COMPLETED", 0L);
        summary.put("FAILED", 0L);
        summary.put("CANCELLED", 0L);
        for (Object[] row : counts) {
            summary.put((String) row[0], (Long) row[1]);
        }
        return summary;
    }

    /**
     * Returns the next N pending emails.
     */
    public List<ScheduledEmail> getNextPending(int limit) {
        return repository.findNextPending(Math.max(1, limit));
    }

    /**
     * Purges completed emails older than the given number of days.
     */
    @Transactional
    public int purgeOldCompleted(int olderThanDays) {
        LocalDateTime threshold = LocalDateTime.now().minusDays(olderThanDays);
        return repository.purgeCompletedBefore(threshold);
    }

    /**
     * Finds all emails due for processing right now.
     */
    public List<ScheduledEmail> findDueEmails() {
        return repository.findDueForProcessing(LocalDateTime.now());
    }

    /**
     * Processes a single scheduled email: generates the reply using the LLM service
     * and persists the result.
     */
    @Transactional
    public ScheduledEmail processEmail(ScheduledEmail email) {
        email.markProcessing();
        repository.save(email);

        long startTime = System.currentTimeMillis();
        String status = "SUCCESS";

        try {
            EmailRequest emailRequest = new EmailRequest();
            emailRequest.setEmailContent(email.getEmailContent());
            emailRequest.setTone(email.getTone());
            emailRequest.setLanguage(email.getLanguage());
            emailRequest.setProvider(email.getProvider());
            emailRequest.setModel(email.getModel());
            emailRequest.setCustomInstructions(email.getCustomInstructions());
            emailRequest.setComposeMode(email.isComposeMode());

            String generatedReply = emailGeneratorService.generateEmailReply(emailRequest);
            email.markCompleted(generatedReply);
            status = "SUCCESS";

        } catch (Exception e) {
            email.markFailed(e.getMessage());
            status = "ERROR";
        }

        long duration = System.currentTimeMillis() - startTime;

        // Log metric for this scheduled processing
        try {
            ApiRequestMetric metric = ApiRequestMetric.builder()
                    .provider(email.getProvider() != null ? email.getProvider() : "scheduled")
                    .model(email.getModel() != null ? email.getModel() : "scheduled")
                    .durationMs(duration)
                    .status(status)
                    .characterCount(email.getGeneratedReply() != null
                            ? email.getGeneratedReply().length() : 0)
                    .build();
            apiRequestMetricService.saveMetric(metric);
        } catch (Exception ignored) {
            // Metric logging is best-effort
        }

        return repository.save(email);
    }

    /**
     * Processes all due emails in batch. Returns the list of processed emails.
     */
    @Transactional
    public List<ScheduledEmail> processDueEmails() {
        List<ScheduledEmail> dueEmails = findDueEmails();
        List<ScheduledEmail> processed = new ArrayList<>();

        for (ScheduledEmail email : dueEmails) {
            try {
                ScheduledEmail processedEmail = processEmail(email);
                processed.add(processedEmail);
            } catch (Exception e) {
                System.err.println("Failed to process scheduled email " + email.getId()
                        + ": " + e.getMessage());
            }
        }

        return processed;
    }

    /**
     * Returns upcoming emails (next 7 days).
     */
    public List<ScheduledEmail> getUpcoming() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime weekLater = now.plusDays(7);
        List<ScheduledEmail> allPending = repository.findByStatusOrderByScheduledAtAsc("PENDING");
        return allPending.stream()
                .filter(e -> e.getScheduledAt().isAfter(now) && e.getScheduledAt().isBefore(weekLater))
                .collect(Collectors.toList());
    }
}
