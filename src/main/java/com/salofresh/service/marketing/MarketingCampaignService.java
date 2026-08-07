package com.salofresh.service.marketing;

import com.salofresh.dto.marketing.CampaignRecipientResponse;
import com.salofresh.dto.marketing.CampaignResponse;
import com.salofresh.dto.marketing.CreateCampaignRequest;
import com.salofresh.response.PagedResponse;
import org.springframework.data.domain.Pageable;

public interface MarketingCampaignService {

    /**
     * Resolves the requested target audience into an actual customer list, creates the
     * campaign row plus one CampaignRecipient row per resolved customer, and either leaves it
     * as DRAFT/dispatches it immediately (scheduledAt is null or in the past) or marks it
     * SCHEDULED (scheduledAt is a future instant, to be picked up by CampaignDispatchScheduler).
     */
    CampaignResponse create(Long ownerUserId, Long salonId, CreateCampaignRequest request);

    /**
     * Transitions a DRAFT/SCHEDULED campaign to SENDING and dispatches it to every PENDING
     * recipient via the channel appropriate service, one recipient failure at a time so it
     * never aborts the whole batch. Ends as SENT, unless literally every recipient failed, in
     * which case the campaign is marked FAILED.
     */
    CampaignResponse sendNow(Long ownerUserId, Long campaignId);

    /**
     * Cancels a campaign that is still DRAFT or SCHEDULED.
     */
    CampaignResponse cancel(Long ownerUserId, Long campaignId);

    PagedResponse<CampaignResponse> listForSalon(Long ownerUserId, Long salonId, Pageable pageable);

    CampaignResponse getById(Long ownerUserId, Long campaignId);

    PagedResponse<CampaignRecipientResponse> listRecipients(Long ownerUserId, Long campaignId, Pageable pageable);
}
