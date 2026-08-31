package com.email.writer;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Background processor that automatically picks up due scheduled emails
 * and triggers LLM generation. Runs on a fixed interval and is idempotent.
 *
 * The processor is designed for a single-instance deployment. For multi-instance
 * deployments, a distributed lock (e.g., ShedLock or Redis-based) should be used.
 */
@Component
public class ScheduledEmailProcessor {

    private final ScheduledEmailService scheduledEmailService;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ScheduledEmailProcessor(ScheduledEmailService scheduledEmailService) {
        this.scheduledEmailService = scheduledEmailService;
    }

    /**
     * Polls for due scheduled emails every 30 seconds.
     * Only processes emails whose scheduledAt time has passed
     * and that haven't exceeded their max retry count.
     */
    @Scheduled(fixedDelayString = "${scheduler.poll-interval:30000}")
    public void pollAndProcess() {
        try {
            List<ScheduledEmail> dueEmails = scheduledEmailService.findDueEmails();

            if (dueEmails.isEmpty()) {
                return; // No work to do
            }

            System.out.println("[Scheduler] Found " + dueEmails.size()
                    + " email(s) due for processing at "
                    + LocalDateTime.now().format(FORMATTER));

            for (ScheduledEmail email : dueEmails) {
                processWithLogging(email);
            }

        } catch (Exception e) {
            System.err.println("[Scheduler] Error during polling cycle: " + e.getMessage());
        }
    }

    /**
     * Processes a single email with structured logging.
     */
    private void processWithLogging(ScheduledEmail email) {
        System.out.println("[Scheduler] Processing email #" + email.getId()
                + " → " + email.getRecipientEmail()
                + " (attempt " + (email.getAttempts() + 1) + "/" + email.getMaxAttempts() + ")");

        long startTime = System.currentTimeMillis();

        try {
            ScheduledEmail processed = scheduledEmailService.processEmail(email);
            long duration = System.currentTimeMillis() - startTime;

            if ("COMPLETED".equals(processed.getStatus())) {
                System.out.println("[Scheduler] Email #" + processed.getId()
                        + " completed successfully in " + duration + "ms"
                        + " → reply length: "
                        + (processed.getGeneratedReply() != null
                                ? processed.getGeneratedReply().length() : 0) + " chars");
            } else if ("FAILED".equals(processed.getStatus())) {
                System.err.println("[Scheduler] Email #" + processed.getId()
                        + " permanently failed after " + processed.getAttempts()
                        + " attempts: " + processed.getLastError());
            } else {
                System.out.println("[Scheduler] Email #" + processed.getId()
                        + " still in status: " + processed.getStatus());
            }

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            System.err.println("[Scheduler] Exception processing email #" + email.getId()
                    + " after " + duration + "ms: " + e.getMessage());
        }
    }

    /**
     * Weekly cleanup: purges completed emails older than 30 days.
     * Runs every Sunday at 03:00 AM.
     */
    @Scheduled(cron = "0 0 3 ? * SUN")
    public void weeklyCleanup() {
        System.out.println("[Scheduler] Running weekly purge of old completed emails...");
        try {
            int purged = scheduledEmailService.purgeOldCompleted(30);
            System.out.println("[Scheduler] Purged " + purged + " old completed email(s).");
        } catch (Exception e) {
            System.err.println("[Scheduler] Cleanup failed: " + e.getMessage());
        }
    }

    /**
     * Daily status report logging.
     * Runs every day at 08:00 AM.
     */
    @Scheduled(cron = "0 0 8 * * *")
    public void dailyStatusReport() {
        try {
            System.out.println("[Scheduler] === Daily Status Report ===");
            System.out.println("[Scheduler] Timestamp: " + LocalDateTime.now().format(FORMATTER));

            List<ScheduledEmail> upcoming = scheduledEmailService.getUpcoming();
            System.out.println("[Scheduler] Upcoming emails (next 7 days): " + upcoming.size());

            for (ScheduledEmail email : upcoming) {
                System.out.println("[Scheduler]   → #" + email.getId()
                        + " scheduled for " + email.getScheduledAt().format(FORMATTER)
                        + " | " + email.getRecipientEmail()
                        + " | label: " + (email.getLabel() != null ? email.getLabel() : "none"));
            }

            List<ScheduledEmail> failed = scheduledEmailService.getByStatus("FAILED");
            if (!failed.isEmpty()) {
                System.out.println("[Scheduler] ⚠ Failed emails requiring attention: " + failed.size());
                for (ScheduledEmail email : failed) {
                    System.out.println("[Scheduler]   → #" + email.getId()
                            + " | " + email.getRecipientEmail()
                            + " | error: " + email.getLastError());
                }
            }

            System.out.println("[Scheduler] === End Report ===");
        } catch (Exception e) {
            System.err.println("[Scheduler] Status report failed: " + e.getMessage());
        }
    }
}
