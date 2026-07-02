package com.salofresh.repository;

import com.salofresh.entity.EmployeeLeave;
import com.salofresh.common.enums.LeaveStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EmployeeLeaveRepository extends JpaRepository<EmployeeLeave, Long> {

    List<EmployeeLeave> findAllByEmployeeId(Long employeeId);

    List<EmployeeLeave> findAllByEmployeeIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long employeeId, LeaveStatus status, LocalDate date1, LocalDate date2);

    Optional<EmployeeLeave> findByIdAndEmployeeId(Long id, Long employeeId);

    List<EmployeeLeave> findAllByEmployee_Salon_IdAndStatus(Long salonId, LeaveStatus status);
}
