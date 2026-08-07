package com.salofresh.dto.marketing;

import com.salofresh.common.enums.CampaignAudience;
import com.salofresh.common.enums.CampaignStatus;
import com.salofresh.common.enums.NotificationChannel;
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
public class CampaignResponse {

    private Long id;
    private String title;
    private String message;
    private NotificationChannel channel;
    private CampaignAudience targetAudience;
    private CampaignStatus status;
    private Instant scheduledAt;
    private Instant sentAt;

    /**
     * Computed by the service via CampaignRecipientRepository count methods, not mapped
     * directly from the entity.
     */
    private long recipientCount;
    private long sentCount;
    private long failedCount;
}
