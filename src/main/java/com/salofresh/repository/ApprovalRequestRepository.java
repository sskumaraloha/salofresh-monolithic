package com.salofresh.repository;

import com.salofresh.common.enums.ApprovalStatus;
import com.salofresh.entity.ApprovalRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, Long> {

    Page<ApprovalRequest> findAllByStatusOrderByCreatedAtDesc(ApprovalStatus status, Pageable pageable);

    Page<ApprovalRequest> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<ApprovalRequest> findAllByRequestedByIdOrderByCreatedAtDesc(Long requestedById, Pageable pageable);
}
