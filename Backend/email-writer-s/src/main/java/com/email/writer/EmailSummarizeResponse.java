package com.email.writer;

import lombok.Data;
import lombok.AllArgsConstructor;

@Data
@AllArgsConstructor
public class EmailSummarizeResponse {
    private String threadId;
    private String summary;
    private boolean cached;
}
