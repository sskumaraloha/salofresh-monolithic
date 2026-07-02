package com.salofresh.dto.booking;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RescheduleRequest {

    @NotNull(message = "New appointment date is required")
    @FutureOrPresent(message = "New appointment date cannot be in the past")
    private LocalDate newAppointmentDate;

    @NotNull(message = "New start time is required")
    private LocalTime newStartTime;

    /**
     * Optional. When omitted, the appointment keeps its currently assigned employee (if any).
     */
    private Long newEmployeeId;
}
