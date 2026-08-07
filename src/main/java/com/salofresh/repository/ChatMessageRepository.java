package com.salofresh.repository;

import com.salofresh.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    Page<ChatMessage> findAllByConversationIdOrderBySentAtDesc(Long conversationId, Pageable pageable);

    @Modifying
    @Query("update ChatMessage m set m.readAt = :readAt where m.conversation.id = :conversationId "
            + "and m.sender.id <> :readerId and m.readAt is null")
    void markConversationAsRead(@Param("conversationId") Long conversationId, @Param("readerId") Long readerId,
                                 @Param("readAt") Instant readAt);

    long countByConversationIdAndSenderIdNotAndReadAtIsNull(Long conversationId, Long readerId);
}
