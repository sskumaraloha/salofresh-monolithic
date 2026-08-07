package com.salofresh.repository;

import com.salofresh.entity.PayrollRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PayrollRecordRepository extends JpaRepository<PayrollRecord, Long> {

    Page<PayrollRecord> findAllBySalonIdOrderByPeriodStartDesc(Long salonId, Pageable pageable);

    Page<PayrollRecord> findAllByEmployeeIdOrderByPeriodStartDesc(Long employeeId, Pageable pageable);

    Optional<PayrollRecord> findByIdAndSalonId(Long id, Long salonId);

    List<PayrollRecord> findAllByEmployeeIdAndPeriodStartAndPeriodEnd(Long employeeId, LocalDate periodStart, LocalDate periodEnd);
}
