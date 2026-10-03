package com.email.writer;

import lombok.Data;

/**
 * Request DTO for recording a campaign metric event.
 */
@Data
public class MetricEventRequest {
    private Long variantId;
    private String eventType;
    private String recipientEmail;
    private String deviceType;
    private String region;
    private Long timeToEventMs;
    private Double revenue;
    private String metadata;
}
