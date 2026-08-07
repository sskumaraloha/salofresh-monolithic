package com.salofresh.dto.waitlist;

import com.salofresh.common.enums.WaitlistStatus;
import com.salofresh.dto.salon.SalonSummaryResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaitlistResponse {

    private Long id;
    private SalonSummaryResponse salon;
    private String employeeName;
    private String serviceName;
    private LocalDate preferredDate;
    private LocalTime preferredStartTime;
    private LocalTime preferredEndTime;
    private WaitlistStatus status;
    private Instant notifiedAt;
    private Instant createdAt;
}
