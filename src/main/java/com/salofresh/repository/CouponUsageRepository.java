package com.salofresh.repository;

import com.salofresh.entity.CouponUsage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CouponUsageRepository extends JpaRepository<CouponUsage, Long> {

    List<CouponUsage> findAllByCouponIdAndUserId(Long couponId, Long userId);

    long countByCouponIdAndUserId(Long couponId, Long userId);
}
