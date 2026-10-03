package com.email.writer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AudienceSegmentRepository extends JpaRepository<AudienceSegment, Long> {

    List<AudienceSegment> findByUserId(String userId);

    List<AudienceSegment> findBySegmentType(String segmentType);

    List<AudienceSegment> findByIsActive(boolean isActive);

    List<AudienceSegment> findByUserIdAndIsActive(String userId, boolean isActive);

    AudienceSegment findByUserIdAndName(String userId, String name);

    @Query("SELECT a FROM AudienceSegment a WHERE a.userId = :userId AND a.memberCount > 0 ORDER BY a.memberCount DESC")
    List<AudienceSegment> findNonEmptyByUser(@Param("userId") String userId);

    @Query("SELECT a FROM AudienceSegment a WHERE a.needsRefresh = true")
    List<AudienceSegment> findNeedingRefresh();

    @Query("SELECT a.segmentType, COUNT(a) FROM AudienceSegment a WHERE a.userId = :userId GROUP BY a.segmentType")
    List<Object[]> countByTypeForUser(@Param("userId") String userId);

    @Query("SELECT SUM(a.memberCount) FROM AudienceSegment a WHERE a.userId = :userId")
    Long totalMembersForUser(@Param("userId") String userId);
}
