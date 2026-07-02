package com.salofresh.service.employee;

import com.salofresh.dto.employee.AttendanceRequest;
import com.salofresh.dto.employee.AttendanceResponse;

import java.time.LocalDate;
import java.util.List;

public interface EmployeeAttendanceService {

    AttendanceResponse record(Long salonId, Long employeeId, AttendanceRequest request);

    List<AttendanceResponse> listForEmployee(Long salonId, Long employeeId, LocalDate startDate, LocalDate endDate);

    AttendanceResponse dailySummary(Long salonId, Long employeeId, LocalDate date);
}
