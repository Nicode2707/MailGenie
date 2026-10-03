package com.email.writer;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/email")
@CrossOrigin("*")
public class EmailSummarizeController {

    private final EmailGeneratorService emailGeneratorService;
    private final EmailSummaryRepository summaryRepository;

    public EmailSummarizeController(EmailGeneratorService emailGeneratorService, EmailSummaryRepository summaryRepository) {
        this.emailGeneratorService = emailGeneratorService;
        this.summaryRepository = summaryRepository;
    }

    @PostMapping("/summarize")
    public ResponseEntity<EmailSummarizeResponse> summarizeEmail(@RequestBody EmailSummarizeRequest request) {
        // Check cache first if threadId is provided
        if (request.getThreadId() != null && !request.getThreadId().isEmpty()) {
            Optional<EmailSummary> cachedSummary = summaryRepository.findByThreadId(request.getThreadId());
            if (cachedSummary.isPresent()) {
                return ResponseEntity.ok(new EmailSummarizeResponse(
                        request.getThreadId(),
                        cachedSummary.get().getSummaryText(),
                        true
                ));
            }
        }

        // Generate summary using LLM
        String summary = emailGeneratorService.generateEmailSummary(request);

        // Cache the result
        if (request.getThreadId() != null && !request.getThreadId().isEmpty()) {
            EmailSummary newSummary = new EmailSummary();
            newSummary.setThreadId(request.getThreadId());
            newSummary.setSummaryText(summary);
            summaryRepository.save(newSummary);
        }

        return ResponseEntity.ok(new EmailSummarizeResponse(request.getThreadId(), summary, false));
    }
}
