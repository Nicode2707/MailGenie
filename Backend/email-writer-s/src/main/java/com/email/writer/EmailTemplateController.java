package com.email.writer;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller exposing endpoints for Email Templates CRUD.
 */
@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class EmailTemplateController {

    private final EmailTemplateService service;

    /**
     * Create/Save a new template.
     */
    @PostMapping
    public ResponseEntity<EmailTemplate> createTemplate(@RequestBody EmailTemplate template) {
        EmailTemplate saved = service.saveTemplate(template);
        return ResponseEntity.ok(saved);
    }

    /**
     * Retrieve all saved templates.
     */
    @GetMapping
    public ResponseEntity<List<EmailTemplate>> getAllTemplates() {
        List<EmailTemplate> list = service.getAllTemplates();
        return ResponseEntity.ok(list);
    }

    /**
     * Delete a template by ID.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable Long id) {
        if (service.deleteTemplate(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Render a template by ID with variable substitutions.
     */
    @PostMapping("/{id}/render")
    public ResponseEntity<String> renderTemplate(
            @PathVariable Long id,
            @RequestBody(required = false) java.util.Map<String, String> variables) {
        return service.renderTemplate(id, variables != null ? variables : java.util.Map.of())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
