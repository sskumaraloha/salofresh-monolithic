package com.salofresh.dto.marketing;

import com.salofresh.common.enums.CampaignAudience;
import com.salofresh.common.enums.NotificationChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCampaignRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    @NotBlank(message = "Message is required")
    @Size(max = 2000, message = "Message must not exceed 2000 characters")
    private String message;

    @NotNull(message = "Channel is required")
    private NotificationChannel channel;

    @NotNull(message = "Target audience is required")
    private CampaignAudience targetAudience;

    /**
     * Explicit list of customer ids to target. Required (and only used) when
     * {@link #targetAudience} is {@link CampaignAudience#CUSTOM}.
     */
    private List<Long> customCustomerIds;

    /**
     * When to send the campaign. Leave null to send immediately on submit; set a future
     * instant to schedule it for later (picked up by {@code CampaignDispatchScheduler}).
     */
    private Instant scheduledAt;
}
