package com.salofresh.repository;

import com.salofresh.common.enums.WaitlistStatus;
import com.salofresh.entity.Waitlist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WaitlistRepository extends JpaRepository<Waitlist, Long> {

    Page<Waitlist> findAllByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Optional<Waitlist> findByIdAndUserId(Long id, Long userId);

    List<Waitlist> findAllBySalonIdAndPreferredDateAndStatus(Long salonId, LocalDate preferredDate, WaitlistStatus status);

    List<Waitlist> findAllByEmployeeIdAndPreferredDateAndStatus(Long employeeId, LocalDate preferredDate, WaitlistStatus status);
}
