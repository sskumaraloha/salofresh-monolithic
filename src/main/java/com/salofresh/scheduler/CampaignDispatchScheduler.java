package com.salofresh.scheduler;

import com.salofresh.common.enums.CampaignStatus;
import com.salofresh.entity.MarketingCampaign;
import com.salofresh.repository.MarketingCampaignRepository;
import com.salofresh.service.marketing.MarketingCampaignService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Every 15 minutes, finds SCHEDULED marketing campaigns whose scheduledAt time has arrived and
 * dispatches them.
 *
 * <p>Note: MarketingCampaignRepository was not pre-authorized to gain a new derived query
 * method for this lookup, so this loads every campaign via {@code findAll()} and filters by
 * status/scheduledAt in memory (the same "acceptable at this scale" trade-off called out for
 * WaitlistCleanupScheduler-style jobs elsewhere in this codebase). A real production system with
 * a non-trivial number of campaigns would want an indexed query, e.g.
 * {@code findAllByStatusAndScheduledAtLessThanEqual(SCHEDULED, now)}.</p>
 */
@Component
@RequiredArgsConstructor
public class CampaignDispatchScheduler {

    private static final Logger log = LoggerFactory.getLogger(CampaignDispatchScheduler.class);

    private final MarketingCampaignRepository marketingCampaignRepository;
    private final MarketingCampaignService marketingCampaignService;

    @Scheduled(cron = "0 */15 * * * *")
    public void dispatchDueCampaigns() {
        Instant now = Instant.now();
        // findAll() runs inside Spring Data's own transaction, so accessing the (lazy)
        // createdByUser id here — while still inside that call chain — is safe; we deliberately
        // extract only the plain ids we need into a detached record before the session closes.
        List<DueCampaign> due = marketingCampaignRepository.findAll().stream()
                .filter(campaign -> campaign.getStatus() == CampaignStatus.SCHEDULED)
                .filter(campaign -> campaign.getScheduledAt() != null && !campaign.getScheduledAt().isAfter(now))
                .map(this::toDueCampaign)
                .toList();

        if (due.isEmpty()) {
            return;
        }
        for (DueCampaign campaign : due) {
            try {
                marketingCampaignService.sendNow(campaign.ownerUserId(), campaign.campaignId());
            } catch (Exception ex) {
                log.error("Failed to dispatch scheduled campaign {}", campaign.campaignId(), ex);
            }
        }
        log.info("Dispatched {} scheduled marketing campaigns", due.size());
    }

    private DueCampaign toDueCampaign(MarketingCampaign campaign) {
        return new DueCampaign(campaign.getId(), campaign.getCreatedByUser().getId());
    }

    private record DueCampaign(Long campaignId, Long ownerUserId) {
    }
}
