package com.salofresh.repository;

import com.salofresh.common.enums.DataRequestStatus;
import com.salofresh.common.enums.DataRequestType;
import com.salofresh.entity.DataSubjectRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DataSubjectRequestRepository extends JpaRepository<DataSubjectRequest, Long> {

    Page<DataSubjectRequest> findAllByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<DataSubjectRequest> findAllByStatusOrderByCreatedAtDesc(DataRequestStatus status, Pageable pageable);

    Page<DataSubjectRequest> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Optional<DataSubjectRequest> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndRequestTypeAndStatusIn(Long userId, DataRequestType requestType, List<DataRequestStatus> statuses);
}
