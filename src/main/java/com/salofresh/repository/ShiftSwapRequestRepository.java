package com.salofresh.repository;

import com.salofresh.common.enums.ShiftSwapStatus;
import com.salofresh.entity.ShiftSwapRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShiftSwapRequestRepository extends JpaRepository<ShiftSwapRequest, Long> {

    Page<ShiftSwapRequest> findAllBySalonIdOrderByCreatedAtDesc(Long salonId, Pageable pageable);

    Page<ShiftSwapRequest> findAllByRequestingEmployeeIdOrderByCreatedAtDesc(Long employeeId, Pageable pageable);

    Page<ShiftSwapRequest> findAllBySalonIdAndStatusOrderByCreatedAtDesc(Long salonId, ShiftSwapStatus status, Pageable pageable);

    Optional<ShiftSwapRequest> findByIdAndSalonId(Long id, Long salonId);
}
