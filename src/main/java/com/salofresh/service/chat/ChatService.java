package com.salofresh.service.chat;

import com.salofresh.dto.chat.ChatConversationResponse;
import com.salofresh.dto.chat.ChatMessageResponse;
import com.salofresh.response.PagedResponse;
import org.springframework.data.domain.Pageable;

public interface ChatService {

    /**
     * Finds the existing SALON_SUPPORT conversation between the customer and salon, or creates one.
     */
    ChatConversationResponse getOrCreateSalonSupportConversation(Long customerId, Long salonId);

    /**
     * Finds the existing PLATFORM_SUPPORT conversation for the customer (salon is always null), or
     * creates one.
     */
    ChatConversationResponse getOrCreatePlatformSupportConversation(Long customerId);

    /**
     * Persists a new message on behalf of {@code senderId}, who must be a participant in the
     * conversation (the customer, the owning salon's user for SALON_SUPPORT, or an admin for
     * PLATFORM_SUPPORT). Broadcasts the message live over the WebSocket broker and best-effort
     * notifies the other participant.
     */
    ChatMessageResponse sendMessage(Long conversationId, Long senderId, String text);

    /**
     * Paginated message history for a conversation, newest first. Ownership-checked the same way
     * as {@link #sendMessage(Long, Long, String)}.
     */
    PagedResponse<ChatMessageResponse> listMessages(Long conversationId, Long requesterId, Pageable pageable);

    /**
     * Paginated conversations for a customer, most recently active first.
     */
    PagedResponse<ChatConversationResponse> listConversationsForCustomer(Long customerId, Pageable pageable);

    /**
     * Paginated conversations for a salon. Only the salon's owning user may call this.
     */
    PagedResponse<ChatConversationResponse> listConversationsForSalon(Long salonId, Pageable pageable, Long ownerUserId);

    /**
     * Marks all of the other participant(s)' messages in the conversation as read by {@code readerId}.
     */
    void markRead(Long conversationId, Long readerId);

    /**
     * Closes a conversation. Participant-checked the same way as {@link #sendMessage(Long, Long, String)}.
     */
    ChatConversationResponse closeConversation(Long conversationId, Long requesterId);
}
