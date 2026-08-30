package com.email.writer;

import lombok.Data;

/**
 * Request DTO for creating a variant within a campaign.
 */
@Data
public class VariantRequest {
    private String label;
    private String subject;
    private String body;
    private String tone;
    private String language;
    private Double trafficPercent;
    private String notes;
    private String generationPrompt;
}
