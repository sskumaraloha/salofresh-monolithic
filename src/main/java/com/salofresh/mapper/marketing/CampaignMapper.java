package com.salofresh.mapper.marketing;

import com.salofresh.dto.marketing.CampaignRecipientResponse;
import com.salofresh.dto.marketing.CampaignResponse;
import com.salofresh.entity.CampaignRecipient;
import com.salofresh.entity.MarketingCampaign;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CampaignMapper {

    /**
     * Maps the straightforward campaign fields. recipientCount/sentCount/failedCount are
     * intentionally left at their default (0) here — the service fills them in afterwards
     * using CampaignRecipientRepository's count methods.
     */
    @Mapping(target = "recipientCount", ignore = true)
    @Mapping(target = "sentCount", ignore = true)
    @Mapping(target = "failedCount", ignore = true)
    CampaignResponse toResponse(MarketingCampaign campaign);

    @Mapping(target = "customerName", expression = "java(recipient.getCustomer() != null ? recipient.getCustomer().getFullName() : null)")
    CampaignRecipientResponse toRecipientResponse(CampaignRecipient recipient);
}
