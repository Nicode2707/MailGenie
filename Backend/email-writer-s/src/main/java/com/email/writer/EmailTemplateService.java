package com.email.writer;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service class managing business logic and transactions for email templates.
 */
@Service
@RequiredArgsConstructor
public class EmailTemplateService {

    private final EmailTemplateRepository repository;

    /**
     * Save a new or updated email template.
     */
    @Transactional
    public EmailTemplate saveTemplate(EmailTemplate template) {
        return repository.save(template);
    }

    /**
     * Retrieve all templates sorted by creation date descending.
     */
    public List<EmailTemplate> getAllTemplates() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    /**
     * Retrieve a specific template by ID.
     */
    public Optional<EmailTemplate> getTemplateById(Long id) {
        return repository.findById(id);
    }

    /**
     * Delete a template by ID.
     */
    @Transactional
    public boolean deleteTemplate(Long id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
            return true;
        }
        return false;
    }

    /**
     * Renders a template by ID with variable interpolation.
     */
    public Optional<String> renderTemplate(Long id, java.util.Map<String, String> variables) {
        return repository.findById(id)
                .map(t -> com.email.writer.util.TemplateVariableReplacer.replaceVariables(t.getBody(), variables));
    }
}
