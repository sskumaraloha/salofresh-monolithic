package com.salofresh.repository;

import com.salofresh.common.enums.ChatType;
import com.salofresh.entity.ChatConversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatConversationRepository extends JpaRepository<ChatConversation, Long> {

    Page<ChatConversation> findAllByCustomerIdOrderByLastMessageAtDesc(Long customerId, Pageable pageable);

    Page<ChatConversation> findAllBySalonIdOrderByLastMessageAtDesc(Long salonId, Pageable pageable);

    Optional<ChatConversation> findByCustomerIdAndSalonIdAndType(Long customerId, Long salonId, ChatType type);

    Optional<ChatConversation> findByCustomerIdAndTypeAndSalonIsNull(Long customerId, ChatType type);
}
