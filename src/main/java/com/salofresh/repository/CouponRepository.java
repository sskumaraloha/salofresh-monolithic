package com.salofresh.repository;

import com.salofresh.entity.Coupon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    Optional<Coupon> findByCodeIgnoreCase(String code);

    Page<Coupon> findAllByActiveTrue(Pageable pageable);

    boolean existsByCodeIgnoreCase(String code);
}
