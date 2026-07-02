package com.salofresh.dto.employee;

import com.salofresh.common.enums.AttendanceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceResponse {

    private Long id;
    private Long employeeId;
    private LocalDate attendanceDate;
    private AttendanceStatus status;
    private Instant checkIn;
    private Instant checkOut;
}
