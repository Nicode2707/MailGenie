package com.email.writer;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service managing the email send queue with scheduling, priority ordering, and simulated delivery.
 */
@Service
public class EmailSendQueueService {

    private final Map<String, EmailSendQueueItem> queueStore = new ConcurrentHashMap<>();

    public EmailSendQueueItem enqueue(EmailSendQueueItem item) {
        if (item.getQueueId() == null || item.getQueueId().isEmpty()) {
            item.setQueueId("Q-" + UUID.randomUUID().toString().substring(0, 8));
        }
        if (item.getStatus() == null) item.setStatus(EmailSendQueueItem.Status.QUEUED);
        if (item.getPriority() == null) item.setPriority(EmailSendQueueItem.Priority.NORMAL);
        if (item.getCreatedAt() == null) item.setCreatedAt(LocalDateTime.now());
        queueStore.put(item.getQueueId(), item);
        return item;
    }

    public List<EmailSendQueueItem> getAllQueued() {
        return queueStore.values().stream()
                .sorted(Comparator.comparingInt((EmailSendQueueItem i) -> i.getPriority().ordinal())
                        .thenComparing(EmailSendQueueItem::getScheduledFor, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    public List<EmailSendQueueItem> getByStatus(EmailSendQueueItem.Status status) {
        return getAllQueued().stream().filter(i -> i.getStatus() == status).collect(Collectors.toList());
    }

    public Optional<EmailSendQueueItem> getById(String queueId) {
        return Optional.ofNullable(queueStore.get(queueId));
    }

    public Optional<EmailSendQueueItem> cancelItem(String queueId) {
        EmailSendQueueItem item = queueStore.get(queueId);
        if (item == null || item.getStatus() != EmailSendQueueItem.Status.QUEUED) return Optional.empty();
        item.setStatus(EmailSendQueueItem.Status.CANCELLED);
        return Optional.of(item);
    }

    public Optional<EmailSendQueueItem> updatePriority(String queueId, EmailSendQueueItem.Priority priority) {
        EmailSendQueueItem item = queueStore.get(queueId);
        if (item == null || item.getStatus() != EmailSendQueueItem.Status.QUEUED) return Optional.empty();
        item.setPriority(priority);
        return Optional.of(item);
    }

    public Map<String, Object> processQueue() {
        LocalDateTime now = LocalDateTime.now();
        List<EmailSendQueueItem> ready = queueStore.values().stream()
                .filter(i -> i.getStatus() == EmailSendQueueItem.Status.QUEUED)
                .filter(i -> i.getScheduledFor() != null && !i.getScheduledFor().isAfter(now))
                .sorted(Comparator.comparingInt((EmailSendQueueItem i) -> i.getPriority().ordinal()))
                .limit(10).collect(Collectors.toList());

        int sent = 0, failed = 0;
        for (EmailSendQueueItem item : ready) {
            item.setStatus(EmailSendQueueItem.Status.SENDING);
            try {
                Thread.sleep((long) (Math.random() * 200) + 50);
                if (Math.random() > 0.10) {
                    item.setStatus(EmailSendQueueItem.Status.SENT);
                    item.setSentAt(LocalDateTime.now()); sent++;
                } else {
                    item.setRetryCount(item.getRetryCount() + 1);
                    if (item.getRetryCount() >= 3) {
                        item.setStatus(EmailSendQueueItem.Status.FAILED); item.setErrorMessage("Max retries exceeded");
                    } else {
                        item.setStatus(EmailSendQueueItem.Status.QUEUED); item.setErrorMessage("Retry queued");
                    }
                    failed++;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                item.setStatus(EmailSendQueueItem.Status.FAILED); item.setErrorMessage("Interrupted"); failed++;
            }
        }
        return Map.of("processedCount", ready.size(), "sentCount", sent, "failedCount", failed, "processedAt", LocalDateTime.now().toString());
    }

    public Map<String, Object> getQueueStats() {
        List<EmailSendQueueItem> all = new ArrayList<>(queueStore.values());
        long queued = all.stream().filter(i -> i.getStatus() == EmailSendQueueItem.Status.QUEUED).count();
        long sending = all.stream().filter(i -> i.getStatus() == EmailSendQueueItem.Status.SENDING).count();
        long sent = all.stream().filter(i -> i.getStatus() == EmailSendQueueItem.Status.SENT).count();
        long failed = all.stream().filter(i -> i.getStatus() == EmailSendQueueItem.Status.FAILED).count();
        long cancelled = all.stream().filter(i -> i.getStatus() == EmailSendQueueItem.Status.CANCELLED).count();
        Map<String, Long> priorityDist = all.stream().filter(i -> i.getStatus() == EmailSendQueueItem.Status.QUEUED)
                .collect(Collectors.groupingBy(i -> i.getPriority().name(), Collectors.counting()));
        return Map.of("totalItems", (long) all.size(), "queuedCount", queued, "sendingCount", sending, "sentCount", sent,
                "failedCount", failed, "cancelledCount", cancelled, "priorityDistribution", priorityDist, "lastUpdated", LocalDateTime.now().toString());
    }

    public boolean deleteItem(String queueId) { return queueStore.remove(queueId) != null; }

    public int bulkCancelQueued() {
        int count = 0;
        for (EmailSendQueueItem item : queueStore.values()) {
            if (item.getStatus() == EmailSendQueueItem.Status.QUEUED) { item.setStatus(EmailSendQueueItem.Status.CANCELLED); count++; }
        }
        return count;
    }
}
