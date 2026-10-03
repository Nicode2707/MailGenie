package com.email.writer;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * DTO representing the diff comparison between two template versions.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateDiffResult {

    private Integer addedLines;
    private Integer removedLines;
    private Integer modifiedLines;
    private Integer unchangedLines;
    private Double similarityPercent;
    private String diffType; // CREATED, ADDITIVE, REDUCTIVE, MODIFIED, UNCHANGED
    private String previousVersionSummary;
    private String currentVersionSummary;
}
