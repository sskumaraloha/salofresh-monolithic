package com.salofresh.dto.marketing;

import com.salofresh.common.enums.CampaignRecipientStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignRecipientResponse {

    private String customerName;
    private CampaignRecipientStatus status;
    private Instant sentAt;
    private String errorMessage;
}
