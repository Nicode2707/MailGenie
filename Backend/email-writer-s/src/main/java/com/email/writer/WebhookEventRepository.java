package com.email.writer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.time.LocalDateTime;

@Repository
public interface WebhookEventRepository extends JpaRepository<WebhookEvent, Long> {
    
    @Query("SELECT e FROM WebhookEvent e WHERE e.status IN ('PENDING', 'FAILED') AND e.retryCount < 5 AND e.nextRetryAt <= :now")
    List<WebhookEvent> findEventsToProcess(LocalDateTime now);
}
