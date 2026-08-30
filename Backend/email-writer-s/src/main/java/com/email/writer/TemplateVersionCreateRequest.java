package com.email.writer;

import lombok.Data;
import java.util.List;

/**
 * Request DTO for creating a new template version.
 */
@Data
public class TemplateVersionCreateRequest {
    private Long templateId;
    private String title;
    private String body;
    private String changeDescription;
    private String author;
    private List<String> tags;
}
