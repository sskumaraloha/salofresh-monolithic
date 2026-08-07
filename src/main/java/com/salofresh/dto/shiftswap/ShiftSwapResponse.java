package com.salofresh.dto.shiftswap;

import com.salofresh.common.enums.ShiftSwapStatus;
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
public class ShiftSwapResponse {

    private Long id;
    private Long salonId;
    private EmployeeSummary requestingEmployee;
    private EmployeeSummary targetEmployee;
    private LocalDate shiftDate;
    private LocalTime originalStartTime;
    private LocalTime originalEndTime;
    private String reason;
    private ShiftSwapStatus status;
    private String respondedByName;
    private Instant respondedAt;
    private Instant createdAt;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmployeeSummary {
        private Long id;
        private String fullName;
        private String designation;
    }
}
