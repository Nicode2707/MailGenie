package com.email.writer;

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
 * Unit tests for EmailCategorizationService covering classification accuracy,
 * sentiment analysis, urgency detection, tag extraction, and analytics.
 */
@ExtendWith(MockitoExtension.class)
class EmailCategorizationServiceTest {

    @Mock
    private EmailCategoryRepository repository;

    @InjectMocks
    private EmailCategorizationService service;

    // ---- Category Classification Tests ----

    @Test
    void categorize_inquiryEmail_detectsInquiry() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String content = "I have a question about the project timeline. Could you explain the milestones?";
        EmailCategorizationResult result = service.categorize(content);

        assertEquals("INQUIRY", result.getCategory());
        assertTrue(result.isContainsQuestions());
        verify(repository).save(any(EmailCategory.class));
    }

    @Test
    void categorize_complaintEmail_detectsComplaint() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String content = "I am very unhappy with the service. This is unacceptable and I want a refund immediately.";
        EmailCategorizationResult result = service.categorize(content);

        assertEquals("COMPLAINT", result.getCategory());
        assertEquals("NEGATIVE", result.getSentiment());
        assertEquals("HIGH", result.getUrgency());
    }

    @Test
    void categorize_followUpEmail_detectsFollowUp() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String content = "Just following up on my previous email. Any update on the status?";
        EmailCategorizationResult result = service.categorize(content);

        assertEquals("FOLLOW_UP", result.getCategory());
    }

    @Test
    void categorize_meetingEmail_detectsMeeting() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String content = "Can we schedule a meeting to discuss the calendar for next week? Are you available?";
        EmailCategorizationResult result = service.categorize(content);

        assertEquals("MEETING", result.getCategory());
        assertTrue(result.isContainsQuestions());
    }

    @Test
    void categorize_thankYouEmail_detectsAppreciation() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String content = "Thank you so much for your excellent work on the presentation. Truly outstanding!";
        EmailCategorizationResult result = service.categorize(content);

        assertEquals("APPRECIATION", result.getCategory());
        assertEquals("POSITIVE", result.getSentiment());
    }

    @Test
    void categorize_requestEmail_detectsRequest() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String content = "Please send the quarterly report by end of day. I need it as soon as possible.";
        EmailCategorizationResult result = service.categorize(content);

        assertEquals("REQUEST", result.getCategory());
    }

    @Test
    void categorize_declineEmail_detectsDecline() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String content = "Unfortunately I cannot attend the meeting. I have a scheduling conflict that day.";
        EmailCategorizationResult result = service.categorize(content);

        assertEquals("DECLINE", result.getCategory());
    }

    @Test
    void categorize_introductionEmail_detectsIntroduction() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String content = "I would like to introduce myself — I am joining the new team starting Monday. Nice to meet everyone!";
        EmailCategorizationResult result = service.categorize(content);

        assertEquals("INTRODUCTION", result.getCategory());
    }

    @Test
    void categorize_feedbackEmail_detectsFeedback() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String content = "I would like to provide some feedback on the latest release. My suggestion is to improve the UX.";
        EmailCategorizationResult result = service.categorize(content);

        assertEquals("FEEDBACK", result.getCategory());
    }

    @Test
    void categorize_emptyContent_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> service.categorize(""));
    }

    @Test
    void categorize_nullContent_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> service.categorize(null));
    }

    // ---- Sentiment Tests ----

    @Test
    void categorize_positiveEmail_detectsPositiveSentiment() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String content = "Great news! The project is excellent and we are thrilled with the results.";
        EmailCategorizationResult result = service.categorize(content);

        assertEquals("POSITIVE", result.getSentiment());
    }

    @Test
    void categorize_mixedEmail_detectsMixedSentiment() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String content = "The good news is the project is great. Unfortunately the issue is terrible and unacceptable.";
        EmailCategorizationResult result = service.categorize(content);

        assertEquals("MIXED", result.getSentiment());
    }

    // ---- Urgency Tests ----

    @Test
    void categorize_urgentEmail_detectsHighUrgency() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String content = "This is urgent! Please respond immediately. The deadline is today.";
        EmailCategorizationResult result = service.categorize(content);

        assertEquals("HIGH", result.getUrgency());
    }

    @Test
    void categorize_mediumUrgencyEmail() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String content = "Please review this by end of week. It is a priority task.";
        EmailCategorizationResult result = service.categorize(content);

        assertEquals("MEDIUM", result.getUrgency());
    }

    @Test
    void categorize_noUrgency() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String content = "Just wanted to say hello and share some general thoughts.";
        EmailCategorizationResult result = service.categorize(content);

        assertEquals("NONE", result.getUrgency());
    }

    // ---- Tag Extraction Tests ----

    @Test
    void categorize_emailWithAttachment_mention_extractTag() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String content = "Please find attached the quarterly report.pdf for your review.";
        EmailCategorizationResult result = service.categorize(content);

        assertTrue(result.getMentionsAttachments());
        assertTrue(result.getTags().contains("review"));
    }

    @Test
    void categorize_emailWithDeadline_extractTag() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String content = "Please submit the report by Monday. It is due by end of day.";
        EmailCategorizationResult result = service.categorize(content);

        assertTrue(result.getMentionsDeadline());
    }

    @Test
    void categorize_actionRequiredTag() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String content = "Please review the document and provide your feedback by Friday.";
        EmailCategorizationResult result = service.categorize(content);

        assertTrue(result.getTags().contains("action-required"));
    }

    // ---- Dry Run Tests ----

    @Test
    void categorizeDryRun_doesNotPersist() {
        String content = "I have a question about your pricing. Could you explain?";
        EmailCategorizationResult result = service.categorizeDryRun(content);

        assertEquals("INQUIRY", result.getCategory());
        assertNull(result.getPersistedId());
        verify(repository, never()).save(any());
    }

    @Test
    void categorizeDryRun_emptyContent_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> service.categorizeDryRun(""));
    }

    // ---- Confidence Tests ----

    @Test
    void categorize_multipleKeywords_highConfidence() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String content = "I have a question. Could you explain the timeline? I am seeking clarification on details.";
        EmailCategorizationResult result = service.categorize(content);

        assertTrue(result.getConfidence() >= 0.3);
    }

    @Test
    void categorize_vagueContent_lowConfidence() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String content = "Hello world test message here nothing specific.";
        EmailCategorizationResult result = service.categorize(content);

        assertTrue(result.getConfidence() < 0.7);
    }

    // ---- Tone Detection Tests ----

    @Test
    void categorize_formalEmail_detectsProfessionalTone() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String content = "Dear Sir, I am writing to inquire about the project. Sincerely, John.";
        EmailCategorizationResult result = service.categorize(content);

        assertEquals("professional", result.getDetectedTone());
    }

    @Test
    void categorize_casualEmail_detectsCasualTone() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String content = "Hey team, just wanted to share a quick update lol.";
        EmailCategorizationResult result = service.categorize(content);

        assertEquals("casual", result.getDetectedTone());
    }

    // ---- Retrieval & Analytics Tests ----

    @Test
    void getAnalytics_emptyData_returnsZeroDefaults() {
        when(repository.count()).thenReturn(0L);

        Map<String, Object> analytics = service.getAnalytics();
        assertEquals(0L, analytics.get("totalCategorized"));
        assertEquals(0.0, analytics.get("averageConfidence"));
    }

    @Test
    void getAnalytics_withData_returnsAggregates() {
        when(repository.count()).thenReturn(20L);
        when(repository.countByCategory()).thenReturn(List.of(
                new Object[]{"INQUIRY", 8L}, new Object[]{"REQUEST", 12L}));
        when(repository.countBySentiment()).thenReturn(List.of(
                new Object[]{"POSITIVE", 15L}, new Object[]{"NEGATIVE", 5L}));
        when(repository.countByUrgency()).thenReturn(List.of(
                new Object[]{"HIGH", 3L}, new Object[]{"LOW", 17L}));
        when(repository.averageConfidence()).thenReturn(0.78);
        when(repository.countHighConfidence()).thenReturn(14L);
        when(repository.countByContainsQuestionsTrue()).thenReturn(6L);
        when(repository.countByMentionsDeadlineTrue()).thenReturn(4L);
        when(repository.countActionable()).thenReturn(16L);

        Map<String, Object> analytics = service.getAnalytics();
        assertEquals(20L, analytics.get("totalCategorized"));
        assertEquals(0.78, analytics.get("averageConfidence"));
        assertEquals(70.0, analytics.get("highConfidenceRate"));
        assertEquals(80.0, analytics.get("actionableRate"));
        assertEquals(30.0, analytics.get("questionRate"));
        assertEquals(20.0, analytics.get("deadlineRate"));
    }

    @Test
    void getByCategory_delegatesToRepository() {
        when(repository.findByCategoryOrderByCreatedAtDesc("INQUIRY"))
                .thenReturn(List.of());

        List<EmailCategory> result = service.getByCategory("INQUIRY");
        assertNotNull(result);
    }

    @Test
    void updateLabel_updatesAndReturns() {
        EmailCategory record = EmailCategory.builder().id(1L).category("INQUIRY").build();
        when(repository.findById(1L)).thenReturn(Optional.of(record));
        when(repository.save(any())).thenReturn(record);

        EmailCategory result = service.updateLabel(1L, "important");
        assertEquals("important", result.getUserLabel());
    }

    @Test
    void updateLabel_missingRecord_throwsException() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(NoSuchElementException.class, () -> service.updateLabel(99L, "label"));
    }

    @Test
    void deleteRecord_existingRecord_returnsTrue() {
        when(repository.existsById(1L)).thenReturn(true);
        assertTrue(service.deleteRecord(1L));
        verify(repository).deleteById(1L);
    }

    @Test
    void deleteRecord_missingRecord_returnsFalse() {
        when(repository.existsById(99L)).thenReturn(false);
        assertFalse(service.deleteRecord(99L));
    }

    @Test
    void purgeOld_delegatesToRepository() {
        when(repository.purgeOlderThan(any())).thenReturn(5);
        assertEquals(5, service.purgeOld(30));
    }

    // ---- EmailCategory Entity Tests ----

    @Test
    void emailCategory_getTagList_parsesCorrectly() {
        EmailCategory cat = EmailCategory.builder().tags("inquiry,deadline,project-alpha").build();
        List<String> tags = cat.getTagList();
        assertEquals(3, tags.size());
        assertEquals("inquiry", tags.get(0));
        assertEquals("deadline", tags.get(1));
        assertEquals("project-alpha", tags.get(2));
    }

    @Test
    void emailCategory_getTagList_emptyReturnsEmptyList() {
        EmailCategory cat = EmailCategory.builder().tags("").build();
        assertTrue(cat.getTagList().isEmpty());
    }

    @Test
    void emailCategory_getTagList_nullReturnsEmptyList() {
        EmailCategory cat = EmailCategory.builder().tags(null).build();
        assertTrue(cat.getTagList().isEmpty());
    }

    @Test
    void emailCategory_isHighConfidence_true() {
        EmailCategory cat = EmailCategory.builder().confidence(0.85).build();
        assertTrue(cat.isHighConfidence());
    }

    @Test
    void emailCategory_isHighConfidence_false() {
        EmailCategory cat = EmailCategory.builder().confidence(0.4).build();
        assertFalse(cat.isHighConfidence());
    }

    @Test
    void emailCategory_isActionable_inquiry() {
        EmailCategory cat = EmailCategory.builder().category("INQUIRY").build();
        assertTrue(cat.isActionable());
    }

    @Test
    void emailCategory_isActionable_request() {
        EmailCategory cat = EmailCategory.builder().category("REQUEST").build();
        assertTrue(cat.isActionable());
    }

    @Test
    void emailCategory_isActionable_appreciation() {
        EmailCategory cat = EmailCategory.builder().category("APPRECIATION").build();
        assertFalse(cat.isActionable());
    }
}
