package com.email.writer;

import lombok.Data;

/**
 * Request DTO for recording a deliverability event.
 */
@Data
public class DeliverabilityRecordRequest {
    private String senderEmail;
    private String recipientEmail;
    private String eventType;
    private String bounceType;
    private Integer smtpCode;
    private String smtpDiagnostic;
    private Long campaignId;
    private Long variantId;
    private String sendingIp;
    private String contentType;
    private String subjectLine;
    private Double spamScore;
    private Boolean spfPass;
    private Boolean dkimPass;
    private Boolean dmarcPass;
    private Long latencyMs;
}
