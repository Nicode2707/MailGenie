package com.email.writer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailSummaryRepository extends JpaRepository<EmailSummary, Long> {
    Optional<EmailSummary> findByThreadId(String threadId);
}
