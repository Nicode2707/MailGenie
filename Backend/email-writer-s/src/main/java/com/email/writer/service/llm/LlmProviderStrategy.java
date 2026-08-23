package com.email.writer.service.llm;

import reactor.core.publisher.Flux;

public interface LlmProviderStrategy {
    
    /**
     * Generates a streaming response for the given email prompt.
     * @param prompt The composed email generation prompt
     * @return A reactive stream of text chunks
     */
    Flux<String> generateEmailStream(String prompt);
    
    /**
     * Returns the identifier of this LLM provider (e.g., "GROQ", "GEMINI")
     */
    String getProviderName();
}
