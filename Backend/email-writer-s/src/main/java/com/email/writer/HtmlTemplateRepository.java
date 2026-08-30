package com.email.writer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HtmlTemplateRepository extends JpaRepository<HtmlTemplate, Long> {
    Optional<HtmlTemplate> findByName(String name);
}
