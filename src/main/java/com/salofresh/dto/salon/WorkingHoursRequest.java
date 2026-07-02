package com.salofresh.dto.salon;

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
public class WorkingHoursRequest {

    @NotNull
    private DayOfWeek dayOfWeek;

    private boolean open;

    private LocalTime startTime;

    private LocalTime endTime;

    private LocalTime breakStartTime;

    private LocalTime breakEndTime;
}
