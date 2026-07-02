package com.salofresh.service.employee;

import com.salofresh.dto.employee.ScheduleRequest;
import com.salofresh.dto.employee.ScheduleResponse;

import java.util.List;

public interface EmployeeScheduleService {

    List<ScheduleResponse> upsertWeeklySchedule(Long salonId, Long employeeId, ScheduleRequest request);

    List<ScheduleResponse> getByEmployee(Long salonId, Long employeeId);
}
