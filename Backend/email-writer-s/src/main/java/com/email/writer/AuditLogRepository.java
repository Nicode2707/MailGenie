package com.email.writer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByUserId(String userId);

    List<AuditLog> findByAction(String action);

    List<AuditLog> findByResourceType(String resourceType);

    List<AuditLog> findByOutcome(String outcome);

    List<AuditLog> findByUserIdAndAction(String userId, String action);

    List<AuditLog> findByResourceIdAndResourceType(Long resourceId, String resourceType);

    List<AuditLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

    List<AuditLog> findByUserIdAndTimestampBetween(String userId, LocalDateTime start, LocalDateTime end);

    List<AuditLog> findByRequiresReview(Boolean requiresReview);

    List<AuditLog> findByContainsSensitiveData(Boolean containsSensitiveData);

    List<AuditLog> findTop50ByOrderByTimestampDesc();

    List<AuditLog> findTop50ByUserIdOrderByTimestampDesc(String userId);

    @Query("SELECT a.action, COUNT(a) FROM AuditLog a GROUP BY a.action")
    List<Object[]> countByActionGrouped();

    @Query("SELECT a.resourceType, COUNT(a) FROM AuditLog a GROUP BY a.resourceType")
    List<Object[]> countByResourceTypeGrouped();

    @Query("SELECT a.outcome, COUNT(a) FROM AuditLog a GROUP BY a.outcome")
    List<Object[]> countByOutcomeGrouped();

    @Query("SELECT a FROM AuditLog a WHERE a.action IN ('DELETE', 'EXPORT', 'IMPORT') AND a.outcome = 'SUCCESS' ORDER BY a.timestamp DESC")
    List<AuditLog> findDestructiveActions();

    @Query("SELECT a FROM AuditLog a WHERE a.outcome = 'DENIED' ORDER BY a.timestamp DESC")
    List<AuditLog> findDeniedActions();

    @Query("SELECT a FROM AuditLog a WHERE a.action LIKE %:keyword% OR a.resourceName LIKE %:keyword% OR a.resourceType LIKE %:keyword%")
    List<AuditLog> searchLogs(@Param("keyword") String keyword);

    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.outcome = 'FAILURE'")
    long countFailures();

    @Query("SELECT AVG(a.durationMs) FROM AuditLog a WHERE a.durationMs IS NOT NULL")
    Double averageDurationMs();
}
