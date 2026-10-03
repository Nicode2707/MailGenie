package com.email.writer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for CampaignVariant entity.
 */
@Repository
public interface CampaignVariantRepository extends JpaRepository<CampaignVariant, Long> {

    List<CampaignVariant> findByCampaignIdOrderByLabelAsc(Long campaignId);

    List<CampaignVariant> findByCampaignIdAndIsWinnerTrue(Long campaignId);

    @Query("SELECT v FROM CampaignVariant v WHERE v.campaignId = :campaignId ORDER BY v.clickCount DESC")
    List<CampaignVariant> findBestPerformingByCampaignId(@Param("campaignId") Long campaignId);

    @Query("SELECT v, (v.clickCount + v.replyCount + v.conversionCount) AS score " +
           "FROM CampaignVariant v WHERE v.campaignId = :campaignId ORDER BY score DESC")
    List<CampaignVariant> findTopPerformingByCampaignId(@Param("campaignId") Long campaignId);

    long countByCampaignId(Long campaignId);

    long countByCampaignIdAndIsWinnerTrue(Long campaignId);
}
