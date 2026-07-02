package com.salofresh.repository;

import com.salofresh.entity.RewardPoint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RewardPointRepository extends JpaRepository<RewardPoint, Long> {

    Page<RewardPoint> findAllByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
