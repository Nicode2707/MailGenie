package com.email.writer;

import lombok.Data;

@Data
public class EmailRequest {
    private String emailContent;
    private String tone;
    private String provider; // e.g., "groq", "openai", "gemini", "claude"
    private String model;    // e.g., specific model name
    private String language; // e.g., "English", "Spanish", "French"
    private String apiKey;   // Optional API key passed from frontend
    private String subject; // Optional subject line extracted from email thread or compose dialog
    private String customInstructions; // Optional user custom prompt or template instructions
    private java.util.Map<String, String> templateVariables; // Dynamic variables for template interpolation
    private boolean composeMode; // true if writing a new email, false/default if generating a reply
    private int variantsCount = 1; // Default to 1 variant
}
