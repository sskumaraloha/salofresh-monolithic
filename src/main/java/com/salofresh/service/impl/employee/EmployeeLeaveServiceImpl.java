package com.salofresh.service.impl.employee;

import com.salofresh.common.enums.LeaveStatus;
import com.salofresh.dto.employee.LeaveDecisionRequest;
import com.salofresh.dto.employee.LeaveRequest;
import com.salofresh.dto.employee.LeaveResponse;
import com.salofresh.entity.Employee;
import com.salofresh.entity.EmployeeLeave;
import com.salofresh.entity.Salon;
import com.salofresh.exception.ForbiddenException;
import com.salofresh.exception.ResourceNotFoundException;
import com.salofresh.mapper.employee.EmployeeLeaveMapper;
import com.salofresh.repository.EmployeeLeaveRepository;
import com.salofresh.repository.EmployeeRepository;
import com.salofresh.repository.SalonRepository;
import com.salofresh.security.SecurityUtils;
import com.salofresh.service.employee.EmployeeLeaveService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeLeaveServiceImpl implements EmployeeLeaveService {

    private final EmployeeLeaveRepository employeeLeaveRepository;
    private final EmployeeRepository employeeRepository;
    private final SalonRepository salonRepository;
    private final EmployeeLeaveMapper employeeLeaveMapper;
    private final SecurityUtils securityUtils;

    @Override
    @Transactional
    public LeaveResponse request(Long salonId, Long employeeId, LeaveRequest request) {
        Employee employee = getEmployeeOrThrow(salonId, employeeId);
        verifyRequester(employee);

        EmployeeLeave leave = EmployeeLeave.builder()
                .employee(employee)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .reason(request.getReason())
                .build();

        EmployeeLeave saved = employeeLeaveRepository.save(leave);
        return employeeLeaveMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public LeaveResponse decide(Long salonId, Long employeeId, Long leaveId, LeaveDecisionRequest request) {
        Employee employee = getEmployeeOrThrow(salonId, employeeId);
        verifyOwnership(employee.getSalon());

        EmployeeLeave leave = employeeLeaveRepository.findByIdAndEmployeeId(leaveId, employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave", "id", leaveId));

        leave.setStatus(Boolean.TRUE.equals(request.getApprove()) ? LeaveStatus.APPROVED : LeaveStatus.REJECTED);
        if (request.getReason() != null && !request.getReason().isBlank()) {
            String existingReason = leave.getReason() == null ? "" : leave.getReason();
            leave.setReason(existingReason + " [Decision note: " + request.getReason() + "]");
        }

        EmployeeLeave saved = employeeLeaveRepository.save(leave);
        return employeeLeaveMapper.toResponse(saved);
    }

    @Override
    public List<LeaveResponse> listByEmployee(Long salonId, Long employeeId) {
        Employee employee = getEmployeeOrThrow(salonId, employeeId);
        verifyRequester(employee);

        return employeeLeaveRepository.findAllByEmployeeId(employeeId).stream()
                .map(employeeLeaveMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<LeaveResponse> listPendingForSalon(Long salonId) {
        Salon salon = getSalonOrThrow(salonId);
        verifyOwnership(salon);

        return employeeLeaveRepository.findAllByEmployee_Salon_IdAndStatus(salonId, LeaveStatus.PENDING).stream()
                .map(employeeLeaveMapper::toResponse)
                .collect(Collectors.toList());
    }

    private Salon getSalonOrThrow(Long salonId) {
        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Salon", "id", salonId));
        if (salon.isDeleted()) {
            throw new ResourceNotFoundException("Salon", "id", salonId);
        }
        return salon;
    }

    private Employee getEmployeeOrThrow(Long salonId, Long employeeId) {
        return employeeRepository.findByIdAndSalonIdAndDeletedFalse(employeeId, salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId));
    }

    private void verifyOwnership(Salon salon) {
        Long currentUserId = securityUtils.getCurrentUserId();
        if (salon.getOwner() == null || salon.getOwner().getUser() == null
                || !salon.getOwner().getUser().getId().equals(currentUserId)) {
            throw new ForbiddenException("You do not have permission to manage leave for this salon");
        }
    }

    private void verifyRequester(Employee employee) {
        Long currentUserId = securityUtils.getCurrentUserId();
        boolean isOwner = employee.getSalon().getOwner() != null
                && employee.getSalon().getOwner().getUser() != null
                && employee.getSalon().getOwner().getUser().getId().equals(currentUserId);
        boolean isSelf = employee.getUser() != null && employee.getUser().getId().equals(currentUserId);
        if (!isOwner && !isSelf) {
            throw new ForbiddenException("You do not have permission to access leave records for this employee");
        }
    }
}
