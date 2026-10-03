package com.email.writer;

import java.time.LocalDateTime;

/**
 * Model representing a queued email item with scheduling, priority, and delivery tracking.
 */
public class EmailSendQueueItem {

    public enum Status { QUEUED, SENDING, SENT, FAILED, CANCELLED }
    public enum Priority { LOW, NORMAL, HIGH, URGENT }

    private String queueId;
    private String recipientEmail;
    private String recipientName;
    private String subjectLine;
    private String bodyContent;
    private String tone;
    private String provider;
    private Status status;
    private Priority priority;
    private LocalDateTime createdAt;
    private LocalDateTime scheduledFor;
    private LocalDateTime sentAt;
    private String errorMessage;
    private int retryCount;

    public EmailSendQueueItem() {}

    public EmailSendQueueItem(String queueId, String recipientEmail, String subjectLine, String bodyContent, Priority priority, LocalDateTime scheduledFor) {
        this.queueId = queueId;
        this.recipientEmail = recipientEmail;
        this.subjectLine = subjectLine;
        this.bodyContent = bodyContent;
        this.priority = priority;
        this.scheduledFor = scheduledFor;
        this.status = Status.QUEUED;
        this.retryCount = 0;
        this.createdAt = LocalDateTime.now();
    }

    public String getQueueId() { return queueId; }
    public void setQueueId(String queueId) { this.queueId = queueId; }
    public String getRecipientEmail() { return recipientEmail; }
    public void setRecipientEmail(String recipientEmail) { this.recipientEmail = recipientEmail; }
    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String recipientName) { this.recipientName = recipientName; }
    public String getSubjectLine() { return subjectLine; }
    public void setSubjectLine(String subjectLine) { this.subjectLine = subjectLine; }
    public String getBodyContent() { return bodyContent; }
    public void setBodyContent(String bodyContent) { this.bodyContent = bodyContent; }
    public String getTone() { return tone; }
    public void setTone(String tone) { this.tone = tone; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getScheduledFor() { return scheduledFor; }
    public void setScheduledFor(LocalDateTime scheduledFor) { this.scheduledFor = scheduledFor; }
    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }
}
