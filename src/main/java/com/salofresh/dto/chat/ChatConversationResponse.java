package com.salofresh.dto.chat;

import com.salofresh.common.enums.ChatStatus;
import com.salofresh.common.enums.ChatType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatConversationResponse {

    private Long id;
    private ChatCustomerSummary customer;
    private ChatSalonSummary salon;
    private ChatType type;
    private ChatStatus status;
    private Instant lastMessageAt;

    /**
     * Count of unread messages (from the other participant's perspective) for this conversation.
     * Computed in the service layer via an aggregate repository query.
     */
    private long unreadCount;
}
