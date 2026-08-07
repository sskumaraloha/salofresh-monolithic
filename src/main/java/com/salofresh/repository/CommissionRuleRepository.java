package com.salofresh.repository;

import com.salofresh.entity.CommissionRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CommissionRuleRepository extends JpaRepository<CommissionRule, Long> {

    List<CommissionRule> findAllBySalonIdAndActiveTrue(Long salonId);

    List<CommissionRule> findAllByEmployeeIdAndActiveTrue(Long employeeId);

    Optional<CommissionRule> findByIdAndSalonId(Long id, Long salonId);
}
