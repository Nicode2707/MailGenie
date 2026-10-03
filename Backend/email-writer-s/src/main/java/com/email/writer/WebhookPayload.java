package com.email.writer;

import lombok.Data;
import java.util.Map;

@Data
public class WebhookPayload {
    private String eventType;
    private String triggerSource; // e.g., "stripe", "hubspot"
    private String recipientEmail;
    private String templateId;
    private Map<String, Object> data; // Variables to inject into prompt (customer name, items, etc)
}
