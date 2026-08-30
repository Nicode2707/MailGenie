package com.email.writer;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CampaignAggregationJob {

    private final EmailCampaignRepository campaignRepository;
    private final CampaignVariantRepository variantRepository;

    // Run every day at midnight (or frequently for testing)
    // Here we'll configure it to run every hour for aggregation
    @Scheduled(cron = "0 0 * * * *")
    public void aggregateAndDeclareWinners() {
        // Fetch running campaigns
        List<EmailCampaign> activeCampaigns = campaignRepository.findByStatus("RUNNING");
        
        for (EmailCampaign campaign : activeCampaigns) {
            if (campaign.isTestPeriodComplete() || campaign.hasMinimumSampleSize()) {
                declareWinner(campaign);
            }
        }
    }

    private void declareWinner(EmailCampaign campaign) {
        List<CampaignVariant> variants = variantRepository.findByCampaignId(campaign.getId());
        
        CampaignVariant winner = null;
        double maxRate = -1;

        for (CampaignVariant variant : variants) {
            double rate = 0;
            // Simplified calculation
            if ("CLICK_RATE".equals(campaign.getPrimaryMetric())) {
                rate = variant.getSentCount() > 0 ? (double) variant.getClickCount() / variant.getSentCount() : 0;
            } else {
                rate = variant.getSentCount() > 0 ? (double) variant.getOpenCount() / variant.getSentCount() : 0;
            }
            
            if (rate > maxRate) {
                maxRate = rate;
                winner = variant;
            }
        }

        if (winner != null) {
            campaign.setWinningVariantId(winner.getId());
            campaign.setStatus("COMPLETED");
            campaignRepository.save(campaign);
        }
    }
}
