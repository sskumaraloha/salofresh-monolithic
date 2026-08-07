package com.salofresh.dto.shiftswap;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class CreateShiftSwapRequest {

    @NotNull(message = "Shift date is required")
    private LocalDate shiftDate;

    @NotNull(message = "Original start time is required")
    private LocalTime originalStartTime;

    @NotNull(message = "Original end time is required")
    private LocalTime originalEndTime;

    /**
     * Optional employee id being asked to cover the shift. Null means "anyone available".
     */
    private Long targetEmployeeId;

    @Size(max = 500, message = "Reason must not exceed 500 characters")
    private String reason;
}
