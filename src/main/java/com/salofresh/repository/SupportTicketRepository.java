package com.salofresh.repository;

import com.salofresh.common.enums.TicketStatus;
import com.salofresh.entity.SupportTicket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long>, JpaSpecificationExecutor<SupportTicket> {

    Page<SupportTicket> findAllByCreatedByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<SupportTicket> findAllByStatusOrderByCreatedAtDesc(TicketStatus status, Pageable pageable);

    Optional<SupportTicket> findByIdAndCreatedByUserId(Long id, Long userId);
}
