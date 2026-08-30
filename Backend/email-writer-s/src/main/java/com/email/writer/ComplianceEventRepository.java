package com.email.writer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ComplianceEventRepository extends JpaRepository<ComplianceEvent, Long> {

    List<ComplianceEvent> findByUserId(String userId);

    List<ComplianceEvent> findByEventType(String eventType);

    List<ComplianceEvent> findByStatus(String status);

    List<ComplianceEvent> findByRegulation(String regulation);

    List<ComplianceEvent> findBySeverity(String severity);

    List<ComplianceEvent> findByUserIdAndStatus(String userId, String status);

    List<ComplianceEvent> findByTargetUserId(String targetUserId);

    List<ComplianceEvent> findByEventDateBetween(LocalDateTime start, LocalDateTime end);

    List<ComplianceEvent> findByStatusAndCompletionDeadlineBefore(String status, LocalDateTime deadline);

    @Query("SELECT c FROM ComplianceEvent c WHERE c.status = 'PENDING' AND c.completionDeadline < :now")
    List<ComplianceEvent> findOverdueEvents(@Param("now") LocalDateTime now);

    @Query("SELECT c FROM ComplianceEvent c WHERE c.severity IN ('CRITICAL', 'EMERGENCY') AND c.status != 'COMPLETED'")
    List<ComplianceEvent> findCriticalOpenEvents();

    @Query("SELECT c.eventType, COUNT(c) FROM ComplianceEvent c GROUP BY c.eventType")
    List<Object[]> countByEventTypeGrouped();

    @Query("SELECT c.regulation, COUNT(c) FROM ComplianceEvent c GROUP BY c.regulation")
    List<Object[]> countByRegulationGrouped();

    @Query("SELECT c.status, COUNT(c) FROM ComplianceEvent c GROUP BY c.status")
    List<Object[]> countByStatusGrouped();

    @Query("SELECT c FROM ComplianceEvent c WHERE c.eventType LIKE %:keyword% OR c.description LIKE %:keyword%")
    List<ComplianceEvent> searchEvents(@Param("keyword") String keyword);

    @Query("SELECT c FROM ComplianceEvent c WHERE c.lawfulBasis = :basis")
    List<ComplianceEvent> findByLawfulBasis(@Param("basis") String basis);

    @Query("SELECT COUNT(c) FROM ComplianceEvent c WHERE c.userId = :userId AND c.eventType = :eventType")
    long countByUserIdAndEventType(@Param("userId") String userId, @Param("eventType") String eventType);
}
