package com.salofresh.dto.employee;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleDayRequest {

    @NotNull(message = "Day of week is required")
    private DayOfWeek dayOfWeek;

    private boolean workingDay;

    private LocalTime startTime;

    private LocalTime endTime;
}
