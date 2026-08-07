package com.salofresh.dto.platformbilling;

import com.salofresh.common.enums.MembershipStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * A salon owner's subscription to a {@link com.salofresh.entity.PlatformPlan}. {@code status}
 * reuses {@link MembershipStatus} (ACTIVE/EXPIRED/CANCELLED) purely as a shared status vocabulary;
 * this has nothing to do with a salon's customer-facing memberships.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformSubscriptionResponse {

    private Long id;
    private PlatformPlanResponse plan;
    private LocalDate startDate;
    private LocalDate endDate;
    private MembershipStatus status;
}
