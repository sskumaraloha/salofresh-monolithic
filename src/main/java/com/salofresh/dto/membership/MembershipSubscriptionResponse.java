package com.salofresh.dto.membership;

import com.salofresh.common.enums.MembershipStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MembershipSubscriptionResponse {

    private Long id;
    private MembershipPlanResponse plan;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer remainingSessions;
    private MembershipStatus status;
}
