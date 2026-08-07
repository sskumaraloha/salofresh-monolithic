package com.salofresh.dto.membership;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MembershipPlanResponse {

    private Long id;
    private Long salonId;
    private String name;
    private String description;
    private BigDecimal price;
    private int validityDays;
    private Integer totalSessions;
    private boolean unlimited;
    private boolean active;
}
