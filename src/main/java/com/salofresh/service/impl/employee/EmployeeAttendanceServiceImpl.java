package com.salofresh.service.impl.employee;

import com.salofresh.dto.employee.AttendanceRequest;
import com.salofresh.dto.employee.AttendanceResponse;
import com.salofresh.entity.Employee;
import com.salofresh.entity.EmployeeAttendance;
import com.salofresh.entity.Salon;
import com.salofresh.exception.ForbiddenException;
import com.salofresh.exception.ResourceNotFoundException;
import com.salofresh.mapper.employee.EmployeeAttendanceMapper;
import com.salofresh.repository.EmployeeAttendanceRepository;
import com.salofresh.repository.EmployeeRepository;
import com.salofresh.security.SecurityUtils;
import com.salofresh.service.employee.EmployeeAttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeAttendanceServiceImpl implements EmployeeAttendanceService {

    private final EmployeeAttendanceRepository employeeAttendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeAttendanceMapper employeeAttendanceMapper;
    private final SecurityUtils securityUtils;

    @Override
    @Transactional
    public AttendanceResponse record(Long salonId, Long employeeId, AttendanceRequest request) {
        Employee employee = getEmployeeOrThrow(salonId, employeeId);
        verifyOwnership(employee.getSalon());

        EmployeeAttendance attendance = employeeAttendanceRepository
                .findByEmployeeIdAndAttendanceDate(employeeId, request.getAttendanceDate())
                .orElseGet(() -> EmployeeAttendance.builder()
                        .employee(employee)
                        .attendanceDate(request.getAttendanceDate())
                        .build());

        attendance.setStatus(request.getStatus());
        if (request.getCheckIn() != null) {
            attendance.setCheckIn(request.getCheckIn());
        }
        if (request.getCheckOut() != null) {
            attendance.setCheckOut(request.getCheckOut());
        }

        EmployeeAttendance saved = employeeAttendanceRepository.save(attendance);
        return employeeAttendanceMapper.toResponse(saved);
    }

    @Override
    public List<AttendanceResponse> listForEmployee(Long salonId, Long employeeId, LocalDate startDate, LocalDate endDate) {
        Employee employee = getEmployeeOrThrow(salonId, employeeId);
        verifyOwnership(employee.getSalon());

        return employeeAttendanceRepository
                .findAllByEmployeeIdAndAttendanceDateBetween(employeeId, startDate, endDate).stream()
                .map(employeeAttendanceMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public AttendanceResponse dailySummary(Long salonId, Long employeeId, LocalDate date) {
        Employee employee = getEmployeeOrThrow(salonId, employeeId);
        verifyOwnership(employee.getSalon());

        return employeeAttendanceRepository.findByEmployeeIdAndAttendanceDate(employeeId, date)
                .map(employeeAttendanceMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance record not found for date: " + date));
    }

    private Employee getEmployeeOrThrow(Long salonId, Long employeeId) {
        return employeeRepository.findByIdAndSalonIdAndDeletedFalse(employeeId, salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId));
    }

    private void verifyOwnership(Salon salon) {
        Long currentUserId = securityUtils.getCurrentUserId();
        if (salon.getOwner() == null || salon.getOwner().getUser() == null
                || !salon.getOwner().getUser().getId().equals(currentUserId)) {
            throw new ForbiddenException("You do not have permission to manage attendance for this salon");
        }
    }
}
