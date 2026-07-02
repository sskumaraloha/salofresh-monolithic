package com.salofresh.dto.salon;

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
public class HolidayResponse {

    private Long id;
    private LocalDate holidayDate;
    private String reason;
    private boolean recurringYearly;
}
