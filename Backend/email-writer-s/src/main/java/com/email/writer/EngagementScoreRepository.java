package com.email.writer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EngagementScoreRepository extends JpaRepository<EngagementScore, Long> {

    EngagementScore findByEmail(String email);

    List<EngagementScore> findByUserId(String userId);

    List<EngagementScore> findByEngagementTier(String tier);

    List<EngagementScore> findByUserIdAndEngagementTier(String userId, String tier);

    List<EngagementScore> findByIsVip(boolean isVip);

    List<EngagementScore> findByIsAtRisk(boolean isAtRisk);

    List<EngagementScore> findBySegmentName(String segmentName);

    @Query("SELECT e FROM EngagementScore e WHERE e.userId = :userId ORDER BY e.engagementScore DESC")
    List<EngagementScore> findTopEngaged(@Param("userId") String userId);

    @Query("SELECT e FROM EngagementScore e WHERE e.userId = :userId AND e.totalSent >= :minSent ORDER BY e.engagementScore DESC")
    List<EngagementScore> findTopEngagedWithMinSends(@Param("userId") String userId, @Param("minSent") long minSent);

    @Query("SELECT e.engagementTier, COUNT(e) FROM EngagementScore e WHERE e.userId = :userId GROUP BY e.engagementTier")
    List<Object[]> countByTierForUser(@Param("userId") String userId);

    @Query("SELECT AVG(e.engagementScore) FROM EngagementScore e WHERE e.userId = :userId")
    Double avgScoreForUser(@Param("userId") String userId);

    @Query("SELECT e FROM EngagementScore e WHERE e.userId = :userId AND e.daysSinceLastEngagement > :days")
    List<EngagementScore> findInactiveForDays(@Param("userId") String userId, @Param("days") int days);

    @Query("SELECT e FROM EngagementScore e WHERE e.userId = :userId AND e.engagementScore < :threshold")
    List<EngagementScore> findBelowThreshold(@Param("userId") String userId, @Param("threshold") double threshold);
}
