package com.salofresh.repository;

import com.salofresh.entity.SalonPayout;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SalonPayoutRepository extends JpaRepository<SalonPayout, Long> {

    Page<SalonPayout> findAllBySalonIdOrderByPeriodStartDesc(Long salonId, Pageable pageable);

    Page<SalonPayout> findAllByOrderByPeriodStartDesc(Pageable pageable);

    Optional<SalonPayout> findByIdAndSalonId(Long id, Long salonId);
}
