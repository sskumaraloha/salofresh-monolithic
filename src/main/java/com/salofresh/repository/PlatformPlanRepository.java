package com.salofresh.repository;

import com.salofresh.entity.PlatformPlan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlatformPlanRepository extends JpaRepository<PlatformPlan, Long> {

    List<PlatformPlan> findAllByActiveTrue();

    Page<PlatformPlan> findAllByOrderByIdAsc(Pageable pageable);
}
