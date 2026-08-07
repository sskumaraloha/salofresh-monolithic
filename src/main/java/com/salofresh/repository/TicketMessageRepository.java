package com.salofresh.repository;

import com.salofresh.entity.TicketMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketMessageRepository extends JpaRepository<TicketMessage, Long> {

    Page<TicketMessage> findAllByTicketIdOrderBySentAtAsc(Long ticketId, Pageable pageable);
}
