package com.salofresh.service.impl.employee;

import com.salofresh.dto.employee.ScheduleDayRequest;
import com.salofresh.dto.employee.ScheduleRequest;
import com.salofresh.dto.employee.ScheduleResponse;
import com.salofresh.entity.Employee;
import com.salofresh.entity.EmployeeSchedule;
import com.salofresh.entity.Salon;
import com.salofresh.exception.ForbiddenException;
import com.salofresh.exception.ResourceNotFoundException;
import com.salofresh.mapper.employee.EmployeeScheduleMapper;
import com.salofresh.repository.EmployeeRepository;
import com.salofresh.repository.EmployeeScheduleRepository;
import com.salofresh.security.SecurityUtils;
import com.salofresh.service.employee.EmployeeScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeScheduleServiceImpl implements EmployeeScheduleService {

    private final EmployeeScheduleRepository employeeScheduleRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeScheduleMapper employeeScheduleMapper;
    private final SecurityUtils securityUtils;

    @Override
    @Transactional
    public List<ScheduleResponse> upsertWeeklySchedule(Long salonId, Long employeeId, ScheduleRequest request) {
        Employee employee = getEmployeeOrThrow(salonId, employeeId);
        verifyOwnership(employee.getSalon());

        List<EmployeeSchedule> upserted = request.getDays().stream()
                .map(dayEntry -> upsertDay(employee, dayEntry))
                .collect(Collectors.toList());

        return upserted.stream()
                .map(employeeScheduleMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ScheduleResponse> getByEmployee(Long salonId, Long employeeId) {
        Employee employee = getEmployeeOrThrow(salonId, employeeId);
        return employeeScheduleRepository.findAllByEmployeeId(employee.getId()).stream()
                .map(employeeScheduleMapper::toResponse)
                .collect(Collectors.toList());
    }

    private EmployeeSchedule upsertDay(Employee employee, ScheduleDayRequest dayEntry) {
        DayOfWeek dayOfWeek = dayEntry.getDayOfWeek();
        EmployeeSchedule schedule = employeeScheduleRepository
                .findByEmployeeIdAndDayOfWeek(employee.getId(), dayOfWeek)
                .orElseGet(() -> EmployeeSchedule.builder()
                        .employee(employee)
                        .dayOfWeek(dayOfWeek)
                        .build());

        schedule.setWorkingDay(dayEntry.isWorkingDay());
        schedule.setStartTime(dayEntry.getStartTime());
        schedule.setEndTime(dayEntry.getEndTime());

        return employeeScheduleRepository.save(schedule);
    }

    private Employee getEmployeeOrThrow(Long salonId, Long employeeId) {
        return employeeRepository.findByIdAndSalonIdAndDeletedFalse(employeeId, salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId));
    }

    private void verifyOwnership(Salon salon) {
        Long currentUserId = securityUtils.getCurrentUserId();
        if (salon.getOwner() == null || salon.getOwner().getUser() == null
                || !salon.getOwner().getUser().getId().equals(currentUserId)) {
            throw new ForbiddenException("You do not have permission to manage this employee's schedule");
        }
    }
}
