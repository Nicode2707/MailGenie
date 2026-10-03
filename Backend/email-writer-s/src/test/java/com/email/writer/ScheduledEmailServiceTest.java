package com.email.writer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the ScheduledEmailService.
 * Verifies scheduling, cancellation, rescheduling, processing, and purge logic.
 */
@ExtendWith(MockitoExtension.class)
class ScheduledEmailServiceTest {

    @Mock
    private ScheduledEmailRepository repository;

    @Mock
    private EmailGeneratorService emailGeneratorService;

    @Mock
    private ApiRequestMetricService apiRequestMetricService;

    @InjectMocks
    private ScheduledEmailService service;

    private ScheduledEmailRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new ScheduledEmailRequest();
        validRequest.setRecipientEmail("jane@example.com");
        validRequest.setRecipientName("Jane Doe");
        validRequest.setEmailContent("Thank you for your proposal.");
        validRequest.setTone("professional");
        validRequest.setLanguage("English");
        validRequest.setProvider("groq");
        validRequest.setScheduledAt(LocalDateTime.now().plusHours(1));
        validRequest.setLabel("work");
        validRequest.setMaxAttempts(3);
        validRequest.setAutoSend(false);
    }

    // --- Validation Tests ---

    @Test
    void createScheduledEmail_validRequest_savesAndReturns() {
        ScheduledEmail saved = ScheduledEmail.builder()
                .id(1L)
                .recipientEmail("jane@example.com")
                .status("PENDING")
                .build();
        when(repository.save(any(ScheduledEmail.class))).thenReturn(saved);

        ScheduledEmail result = service.createScheduledEmail(validRequest);

        assertNotNull(result);
        verify(repository).save(any(ScheduledEmail.class));
    }

    @Test
    void createScheduledEmail_missingRecipient_throwsException() {
        validRequest.setRecipientEmail(null);
        assertThrows(IllegalArgumentException.class,
                () -> service.createScheduledEmail(validRequest));
    }

    @Test
    void createScheduledEmail_missingContent_throwsException() {
        validRequest.setEmailContent("");
        assertThrows(IllegalArgumentException.class,
                () -> service.createScheduledEmail(validRequest));
    }

    @Test
    void createScheduledEmail_pastScheduledTime_throwsException() {
        validRequest.setScheduledAt(LocalDateTime.now().minusHours(1));
        assertThrows(IllegalArgumentException.class,
                () -> service.createScheduledEmail(validRequest));
    }

    // --- Retrieval Tests ---

    @Test
    void getById_existingId_returnsEmail() {
        ScheduledEmail email = ScheduledEmail.builder().id(1L).status("PENDING").build();
        when(repository.findById(1L)).thenReturn(Optional.of(email));

        ScheduledEmail result = service.getById(1L);
        assertEquals(1L, result.getId());
    }

    @Test
    void getById_missingId_throwsException() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(NoSuchElementException.class, () -> service.getById(99L));
    }

    @Test
    void getByStatus_delegatesToRepository() {
        ScheduledEmail email = ScheduledEmail.builder().id(1L).status("PENDING").build();
        when(repository.findByStatusOrderByScheduledAtAsc("PENDING"))
                .thenReturn(List.of(email));

        List<ScheduledEmail> result = service.getByStatus("PENDING");
        assertEquals(1, result.size());
        assertEquals("PENDING", result.get(0).getStatus());
    }

    // --- Cancel Tests ---

    @Test
    void cancelEmail_pendingEmail_cancels() {
        ScheduledEmail email = ScheduledEmail.builder()
                .id(1L).status("PENDING").build();
        when(repository.findById(1L)).thenReturn(Optional.of(email));
        when(repository.save(any())).thenReturn(email);

        ScheduledEmail result = service.cancelEmail(1L);
        assertEquals("CANCELLED", result.getStatus());
    }

    @Test
    void cancelEmail_processingEmail_throwsException() {
        ScheduledEmail email = ScheduledEmail.builder()
                .id(1L).status("PROCESSING").build();
        when(repository.findById(1L)).thenReturn(Optional.of(email));

        assertThrows(IllegalStateException.class, () -> service.cancelEmail(1L));
    }

    @Test
    void cancelAllPending_cancelsAllPending() {
        ScheduledEmail e1 = ScheduledEmail.builder().id(1L).status("PENDING").build();
        ScheduledEmail e2 = ScheduledEmail.builder().id(2L).status("PENDING").build();
        when(repository.findByStatusOrderByScheduledAtAsc("PENDING"))
                .thenReturn(List.of(e1, e2));
        when(repository.save(any())).thenReturn(e1);

        int cancelled = service.cancelAllPending();
        assertEquals(2, cancelled);
    }

    // --- Reschedule Tests ---

    @Test
    void rescheduleEmail_pendingEmail_updatesTime() {
        ScheduledEmail email = ScheduledEmail.builder()
                .id(1L).status("PENDING")
                .scheduledAt(LocalDateTime.now().plusDays(1))
                .build();
        when(repository.findById(1L)).thenReturn(Optional.of(email));
        when(repository.save(any())).thenReturn(email);

        LocalDateTime newTime = LocalDateTime.now().plusDays(5);
        ScheduledEmail result = service.rescheduleEmail(1L, newTime);
        assertEquals(newTime, result.getScheduledAt());
    }

    @Test
    void rescheduleEmail_pastTime_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> service.rescheduleEmail(1L, LocalDateTime.now().minusHours(1)));
    }

    @Test
    void rescheduleEmail_completedEmail_throwsException() {
        ScheduledEmail email = ScheduledEmail.builder()
                .id(1L).status("COMPLETED").build();
        when(repository.findById(1L)).thenReturn(Optional.of(email));

        assertThrows(IllegalStateException.class,
                () -> service.rescheduleEmail(1L, LocalDateTime.now().plusDays(1)));
    }

    // --- Processing Tests ---

    @Test
    void processEmail_successfulGeneration_completesEmail() {
        ScheduledEmail email = ScheduledEmail.builder()
                .id(1L).status("PENDING").attempts(0).maxAttempts(3)
                .emailContent("Test content")
                .provider("groq")
                .build();
        when(repository.save(any())).thenReturn(email);
        when(emailGeneratorService.generateEmailReply(any()))
                .thenReturn("Generated reply content");

        ScheduledEmail result = service.processEmail(email);
        assertEquals("COMPLETED", result.getStatus());
        assertEquals("Generated reply content", result.getGeneratedReply());
    }

    @Test
    void processEmail_generationFails_marksFailed() {
        ScheduledEmail email = ScheduledEmail.builder()
                .id(1L).status("PENDING").attempts(0).maxAttempts(3)
                .emailContent("Test content")
                .build();
        when(repository.save(any())).thenReturn(email);
        when(emailGeneratorService.generateEmailReply(any()))
                .thenThrow(new RuntimeException("API error"));

        ScheduledEmail result = service.processEmail(email);
        assertEquals("FAILED", result.getStatus());
        assertEquals("API error", result.getLastError());
    }

    @Test
    void processEmail_retryableFailure_staysPending() {
        ScheduledEmail email = ScheduledEmail.builder()
                .id(1L).status("PENDING").attempts(1).maxAttempts(3)
                .emailContent("Test content")
                .build();
        when(repository.save(any())).thenReturn(email);
        when(emailGeneratorService.generateEmailReply(any()))
                .thenThrow(new RuntimeException("Timeout"));

        ScheduledEmail result = service.processEmail(email);
        // attempts was 1, becomes 2 after markProcessing, still < maxAttempts 3
        assertEquals("PENDING", result.getStatus());
    }

    // --- Process Due Emails ---

    @Test
    void processDueEmails_emptyList_returnsEmpty() {
        when(repository.findDueForProcessing(any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        List<ScheduledEmail> result = service.processDueEmails();
        assertTrue(result.isEmpty());
    }

    @Test
    void processDueEmails_mixedResults_returnsOnlyProcessed() {
        ScheduledEmail email1 = ScheduledEmail.builder()
                .id(1L).status("PENDING").attempts(0).maxAttempts(3)
                .emailContent("Content 1").provider("groq").build();
        ScheduledEmail email2 = ScheduledEmail.builder()
                .id(2L).status("PENDING").attempts(0).maxAttempts(3)
                .emailContent("Content 2").provider("groq").build();

        when(repository.findDueForProcessing(any(LocalDateTime.class)))
                .thenReturn(List.of(email1, email2));
        when(repository.save(any())).thenReturn(email1);
        when(emailGeneratorService.generateEmailReply(any()))
                .thenReturn("Reply 1")
                .thenThrow(new RuntimeException("Fail"));

        List<ScheduledEmail> result = service.processDueEmails();
        assertEquals(1, result.size());
    }

    // --- Status Summary Tests ---

    @Test
    void getStatusSummary_returnsCountsByStatus() {
        when(repository.countByStatus()).thenReturn(List.of(
                new Object[]{"PENDING", 5L},
                new Object[]{"COMPLETED", 12L}
        ));

        Map<String, Long> summary = service.getStatusSummary();
        assertEquals(5L, summary.get("PENDING"));
        assertEquals(12L, summary.get("COMPLETED"));
        assertEquals(0L, summary.get("FAILED"));
    }

    // --- Upcoming Tests ---

    @Test
    void getUpcoming_returnsPendingWithinSevenDays() {
        ScheduledEmail nearFuture = ScheduledEmail.builder()
                .id(1L).status("PENDING")
                .scheduledAt(LocalDateTime.now().plusDays(3))
                .build();
        ScheduledEmail farFuture = ScheduledEmail.builder()
                .id(2L).status("PENDING")
                .scheduledAt(LocalDateTime.now().plusDays(14))
                .build();
        when(repository.findByStatusOrderByScheduledAtAsc("PENDING"))
                .thenReturn(List.of(nearFuture, farFuture));

        List<ScheduledEmail> upcoming = service.getUpcoming();
        assertEquals(1, upcoming.size());
        assertEquals(1L, upcoming.get(0).getId());
    }

    // --- Purge Tests ---

    @Test
    void purgeOldCompleted_delegatesToRepository() {
        when(repository.purgeCompletedBefore(any())).thenReturn(5);

        int purged = service.purgeOldCompleted(30);
        assertEquals(5, purged);
    }

    // --- ScheduledEmail Entity Unit Tests ---

    @Test
    void scheduledEmail_markProcessing_incrementsAttempts() {
        ScheduledEmail email = ScheduledEmail.builder()
                .status("PENDING").attempts(0).build();
        email.markProcessing();
        assertEquals("PROCESSING", email.getStatus());
        assertEquals(1, email.getAttempts());
    }

    @Test
    void scheduledEmail_markCompleted_setsReply() {
        ScheduledEmail email = ScheduledEmail.builder()
                .status("PROCESSING").attempts(1).build();
        email.markCompleted("Hello!");
        assertEquals("COMPLETED", email.getStatus());
        assertEquals("Hello!", email.getGeneratedReply());
    }

    @Test
    void scheduledEmail_markFailed_belowMaxRetries_staysPending() {
        ScheduledEmail email = ScheduledEmail.builder()
                .status("PROCESSING").attempts(1).maxAttempts(3).build();
        email.markFailed("error");
        assertEquals("PENDING", email.getStatus());
        assertEquals("error", email.getLastError());
    }

    @Test
    void scheduledEmail_markFailed_atMaxRetries_becomesFailed() {
        ScheduledEmail email = ScheduledEmail.builder()
                .status("PROCESSING").attempts(3).maxAttempts(3).build();
        email.markFailed("final error");
        assertEquals("FAILED", email.getStatus());
    }

    @Test
    void scheduledEmail_cancel_pending_returnsTrue() {
        ScheduledEmail email = ScheduledEmail.builder().status("PENDING").build();
        assertTrue(email.cancel());
        assertEquals("CANCELLED", email.getStatus());
    }

    @Test
    void scheduledEmail_cancel_completed_returnsFalse() {
        ScheduledEmail email = ScheduledEmail.builder().status("COMPLETED").build();
        assertFalse(email.cancel());
    }

    @Test
    void scheduledEmail_isEligibleForProcessing_pendingAndDue_returnsTrue() {
        ScheduledEmail email = ScheduledEmail.builder()
                .status("PENDING").attempts(0).maxAttempts(3)
                .scheduledAt(LocalDateTime.now().minusMinutes(5))
                .build();
        assertTrue(email.isEligibleForProcessing());
    }

    @Test
    void scheduledEmail_isEligibleForProcessing_futureTime_returnsFalse() {
        ScheduledEmail email = ScheduledEmail.builder()
                .status("PENDING").attempts(0).maxAttempts(3)
                .scheduledAt(LocalDateTime.now().plusDays(1))
                .build();
        assertFalse(email.isEligibleForProcessing());
    }

    @Test
    void scheduledEmail_isEligibleForProcessing_maxAttemptsExceeded_returnsFalse() {
        ScheduledEmail email = ScheduledEmail.builder()
                .status("PENDING").attempts(3).maxAttempts(3)
                .scheduledAt(LocalDateTime.now().minusMinutes(1))
                .build();
        assertFalse(email.isEligibleForProcessing());
    }

    // --- ScheduledEmailRequest Validation Tests ---

    @Test
    void scheduledEmailRequest_isValid_validRequest_returnsTrue() {
        ScheduledEmailRequest req = new ScheduledEmailRequest();
        req.setRecipientEmail("user@test.com");
        req.setEmailContent("Hello");
        req.setScheduledAt(LocalDateTime.now().plusHours(1));
        assertTrue(req.isValid());
    }

    @Test
    void scheduledEmailRequest_isValid_nullEmail_returnsFalse() {
        ScheduledEmailRequest req = new ScheduledEmailRequest();
        req.setEmailContent("Hello");
        req.setScheduledAt(LocalDateTime.now().plusHours(1));
        assertFalse(req.isValid());
    }

    @Test
    void scheduledEmailRequest_isValid_pastTime_returnsFalse() {
        ScheduledEmailRequest req = new ScheduledEmailRequest();
        req.setRecipientEmail("user@test.com");
        req.setEmailContent("Hello");
        req.setScheduledAt(LocalDateTime.now().minusHours(1));
        assertFalse(req.isValid());
    }
}
