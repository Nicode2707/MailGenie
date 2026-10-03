package com.email.writer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for managing the email send queue.
 * Provides endpoints for enqueueing, scheduling, processing, and monitoring.
 */
@RestController
@RequestMapping("/api/send-queue")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class EmailSendQueueController {

    @Autowired
    private EmailSendQueueService queueService;

    /**
     * Enqueue a new email for scheduled delivery.
     */
    @PostMapping("/enqueue")
    public ResponseEntity<EmailSendQueueItem> enqueue(@RequestBody Map<String, Object> payload) {
        EmailSendQueueItem item = new EmailSendQueueItem();
        item.setRecipientEmail((String) payload.getOrDefault("recipientEmail", ""));
        item.setRecipientName((String) payload.getOrDefault("recipientName", ""));
        item.setSubjectLine((String) payload.getOrDefault("subjectLine", ""));
        item.setBodyContent((String) payload.getOrDefault("bodyContent", ""));
        item.setTone((String) payload.getOrDefault("tone", "professional"));
        item.setProvider((String) payload.getOrDefault("provider", "groq"));

        String priorityStr = (String) payload.getOrDefault("priority", "NORMAL");
        try {
            item.setPriority(EmailSendQueueItem.Priority.valueOf(priorityStr.toUpperCase()));
        } catch (IllegalArgumentException e) {
            item.setPriority(EmailSendQueueItem.Priority.NORMAL);
        }

        String scheduledStr = (String) payload.get("scheduledFor");
        if (scheduledStr != null && !scheduledStr.isEmpty()) {
            item.setScheduledFor(LocalDateTime.parse(scheduledStr));
        } else {
            item.setScheduledFor(LocalDateTime.now());
        }

        EmailSendQueueItem saved = queueService.enqueue(item);
        return ResponseEntity.ok(saved);
    }

    /**
     * Get all queue items.
     */
    @GetMapping
    public ResponseEntity<List<EmailSendQueueItem>> getAllItems(
            @RequestParam(required = false) String status) {
        if (status != null && !status.isEmpty()) {
            try {
                EmailSendQueueItem.Status s = EmailSendQueueItem.Status.valueOf(status.toUpperCase());
                return ResponseEntity.ok(queueService.getByStatus(s));
            } catch (IllegalArgumentException e) {
                // fall through to return all
            }
        }
        return ResponseEntity.ok(queueService.getAllQueued());
    }

    /**
     * Get a single queue item.
     */
    @GetMapping("/{queueId}")
    public ResponseEntity<EmailSendQueueItem> getItem(@PathVariable String queueId) {
        return queueService.getById(queueId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Cancel a queued item.
     */
    @PostMapping("/{queueId}/cancel")
    public ResponseEntity<Map<String, Object>> cancelItem(@PathVariable String queueId) {
        return queueService.cancelItem(queueId)
                .map(item -> {
                    Map<String, Object> res = new LinkedHashMap<>();
                    res.put("queueId", item.getQueueId());
                    res.put("status", item.getStatus().name());
                    res.put("message", "Item cancelled successfully");
                    return ResponseEntity.ok(res);
                })
                .orElse(ResponseEntity.badRequest().body(Map.of("error", "Item not found or not cancellable")));
    }

    /**
     * Update priority of a queued item.
     */
    @PutMapping("/{queueId}/priority")
    public ResponseEntity<Map<String, Object>> updatePriority(
            @PathVariable String queueId, @RequestBody Map<String, String> payload) {
        String priorityStr = payload.getOrDefault("priority", "NORMAL");
        try {
            EmailSendQueueItem.Priority p = EmailSendQueueItem.Priority.valueOf(priorityStr.toUpperCase());
            return queueService.updatePriority(queueId, p)
                    .map(item -> {
                        Map<String, Object> res = new LinkedHashMap<>();
                        res.put("queueId", item.getQueueId());
                        res.put("priority", item.getPriority().name());
                        res.put("message", "Priority updated");
                        return ResponseEntity.ok(res);
                    })
                    .orElse(ResponseEntity.badRequest().body(Map.of("error", "Item not found or not updatable")));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid priority value"));
        }
    }

    /**
     * Process the queue — simulate sending all ready items.
     */
    @PostMapping("/process")
    public ResponseEntity<Map<String, Object>> processQueue() {
        return ResponseEntity.ok(queueService.processQueue());
    }

    /**
     * Get queue statistics.
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(queueService.getQueueStats());
    }

    /**
     * Delete a queue item.
     */
    @DeleteMapping("/{queueId}")
    public ResponseEntity<Void> deleteItem(@PathVariable String queueId) {
        if (queueService.deleteItem(queueId)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Bulk cancel all queued items.
     */
    @PostMapping("/bulk-cancel")
    public ResponseEntity<Map<String, Object>> bulkCancel() {
        int cancelled = queueService.bulkCancelQueued();
        return ResponseEntity.ok(Map.of(
                "cancelledCount", cancelled,
                "message", cancelled + " items cancelled"
        ));
    }
}
