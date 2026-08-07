package com.salofresh.repository;

import com.salofresh.common.enums.FraudAlertSeverity;
import com.salofresh.common.enums.FraudAlertStatus;
import com.salofresh.entity.FraudAlert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FraudAlertRepository extends JpaRepository<FraudAlert, Long> {

    Page<FraudAlert> findAllByStatusOrderByDetectedAtDesc(FraudAlertStatus status, Pageable pageable);

    Page<FraudAlert> findAllByOrderByDetectedAtDesc(Pageable pageable);

    boolean existsByRelatedUserIdAndTypeAndStatus(Long relatedUserId, com.salofresh.common.enums.FraudAlertType type,
                                                   FraudAlertStatus status);

    Page<FraudAlert> findAllBySeverityOrderByDetectedAtDesc(FraudAlertSeverity severity, Pageable pageable);
}
