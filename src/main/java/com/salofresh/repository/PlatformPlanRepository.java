package com.salofresh.repository;

import com.salofresh.entity.PlatformPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlatformPlanRepository extends JpaRepository<PlatformPlan, Long> {

    List<PlatformPlan> findAllByActiveTrue();
}
