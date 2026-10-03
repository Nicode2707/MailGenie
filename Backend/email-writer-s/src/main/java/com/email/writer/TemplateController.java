package com.email.writer;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final HtmlTemplateRepository templateRepository;

    @GetMapping
    public ResponseEntity<List<HtmlTemplate>> getAllTemplates() {
        return ResponseEntity.ok(templateRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<HtmlTemplate> createTemplate(@RequestBody HtmlTemplate template) {
        // Basic sanitization check can go here
        return ResponseEntity.ok(templateRepository.save(template));
    }

    @PutMapping("/{id}")
    public ResponseEntity<HtmlTemplate> updateTemplate(@PathVariable Long id, @RequestBody HtmlTemplate updatedTemplate) {
        return templateRepository.findById(id).map(existing -> {
            existing.setName(updatedTemplate.getName());
            existing.setContent(updatedTemplate.getContent());
            return ResponseEntity.ok(templateRepository.save(existing));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable Long id) {
        if (templateRepository.existsById(id)) {
            templateRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}
