package com.email.writer;

import lombok.Data;

@Data
public class EmailSummarizeRequest {
    private String threadId;
    private String threadContent;
    private String provider; // e.g. "openai", "groq", "gemini"
    private String apiKey;
    private String model;
}
