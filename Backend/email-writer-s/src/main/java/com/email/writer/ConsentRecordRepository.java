package com.email.writer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ConsentRecordRepository extends JpaRepository<ConsentRecord, Long> {

    List<ConsentRecord> findByUserId(String userId);

    List<ConsentRecord> findByEmail(String email);

    List<ConsentRecord> findByConsentType(String consentType);

    List<ConsentRecord> findByStatus(String status);

    List<ConsentRecord> findBySource(String source);

    List<ConsentRecord> findByUserIdAndConsentType(String userId, String consentType);

    List<ConsentRecord> findByUserIdAndStatus(String userId, String status);

    List<ConsentRecord> findByIsActive(Boolean isActive);

    List<ConsentRecord> findByExpiryDateBefore(LocalDateTime date);

    List<ConsentRecord> findByDoubleOptInConfirmed(Boolean confirmed);

    @Query("SELECT c FROM ConsentRecord c WHERE c.status = 'GRANTED' AND c.isActive = true AND c.expiryDate < :now")
    List<ConsentRecord> findExpiredActiveConsents(@Param("now") LocalDateTime now);

    @Query("SELECT c FROM ConsentRecord c WHERE c.status = 'PENDING' AND c.consentDate < :deadline")
    List<ConsentRecord> findPendingOlderThan(@Param("deadline") LocalDateTime deadline);

    @Query("SELECT c.consentType, COUNT(c) FROM ConsentRecord c WHERE c.status = 'GRANTED' GROUP BY c.consentType")
    List<Object[]> countGrantedByTypeGrouped();

    @Query("SELECT c.status, COUNT(c) FROM ConsentRecord c GROUP BY c.status")
    List<Object[]> countByStatusGrouped();

    @Query("SELECT c.source, COUNT(c) FROM ConsentRecord c GROUP BY c.source")
    List<Object[]> countBySourceGrouped();

    @Query("SELECT c FROM ConsentRecord c WHERE c.email = :email AND c.consentType = :type AND c.status = 'GRANTED'")
    ConsentRecord findValidConsentByEmailAndType(@Param("email") String email, @Param("type") String type);

    @Query("SELECT COUNT(c) FROM ConsentRecord c WHERE c.userId = :userId AND c.status = 'GRANTED'")
    long countGrantedByUserId(@Param("userId") String userId);

    @Query("SELECT c FROM ConsentRecord c WHERE c.consentType LIKE %:keyword% OR c.purpose LIKE %:keyword%")
    List<ConsentRecord> searchConsents(@Param("keyword") String keyword);
}
