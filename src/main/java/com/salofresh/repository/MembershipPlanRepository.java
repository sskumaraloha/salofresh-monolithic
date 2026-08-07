package com.salofresh.repository;

import com.salofresh.entity.MembershipPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MembershipPlanRepository extends JpaRepository<MembershipPlan, Long> {

    List<MembershipPlan> findAllBySalonIdAndActiveTrue(Long salonId);

    Optional<MembershipPlan> findByIdAndSalonId(Long id, Long salonId);
}
