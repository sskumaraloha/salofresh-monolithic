package com.salofresh.service.employee;

import com.salofresh.dto.employee.LeaveDecisionRequest;
import com.salofresh.dto.employee.LeaveRequest;
import com.salofresh.dto.employee.LeaveResponse;

import java.util.List;

public interface EmployeeLeaveService {

    LeaveResponse request(Long salonId, Long employeeId, LeaveRequest request);

    LeaveResponse decide(Long salonId, Long employeeId, Long leaveId, LeaveDecisionRequest request);

    List<LeaveResponse> listByEmployee(Long salonId, Long employeeId);

    List<LeaveResponse> listPendingForSalon(Long salonId);
}
