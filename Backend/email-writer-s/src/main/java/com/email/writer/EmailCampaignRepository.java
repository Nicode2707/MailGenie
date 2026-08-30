package com.email.writer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for EmailCampaign entity.
 */
@Repository
public interface EmailCampaignRepository extends JpaRepository<EmailCampaign, Long> {

    List<EmailCampaign> findByStatusOrderByCreatedAtDesc(String status);

    List<EmailCampaign> findByCampaignTypeOrderByCreatedAtDesc(String type);

    List<EmailCampaign> findByCreatedByOrderByCreatedAtDesc(String createdBy);

    @Query("SELECT c FROM EmailCampaign c WHERE c.startDate <= :now AND c.endDate >= :now AND c.status = 'RUNNING'")
    List<EmailCampaign> findActiveCampaigns(@Param("now") LocalDateTime now);

    @Query("SELECT c FROM EmailCampaign c WHERE c.endDate < :now AND c.status = 'RUNNING'")
    List<EmailCampaign> findExpiredRunningCampaigns(@Param("now") LocalDateTime now);

    List<EmailCampaign> findByTagsLikeOrderByCreatedAtDesc(String tag);

    long countByStatus(String status);

    @Query("SELECT c.campaignType, COUNT(c) FROM EmailCampaign c GROUP BY c.campaignType")
    List<Object[]> countByCampaignType();
}
