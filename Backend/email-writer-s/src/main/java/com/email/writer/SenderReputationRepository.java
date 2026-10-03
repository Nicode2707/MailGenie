package com.email.writer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for SenderReputation entity.
 */
@Repository
public interface SenderReputationRepository extends JpaRepository<SenderReputation, Long> {

    Optional<SenderReputation> findByDomain(String domain);

    List<SenderReputation> findAllByOrderByReputationScoreDesc();

    List<SenderReputation> findByHealthGradeOrderByReputationScoreDesc(String grade);

    List<SenderReputation> findByRiskLevelOrderByReputationScoreAsc(String riskLevel);

    @Query("SELECT s FROM SenderReputation s WHERE s.reputationScore < :threshold ORDER BY s.reputationScore ASC")
    List<SenderReputation> findBelowThreshold(double threshold);

    @Query("SELECT s FROM SenderReputation s WHERE s.spamComplaintRate > :rate ORDER BY s.spamComplaintRate DESC")
    List<SenderReputation> findAboveComplaintRate(double rate);
}
