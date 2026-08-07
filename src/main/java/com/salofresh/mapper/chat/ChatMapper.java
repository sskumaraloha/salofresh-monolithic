package com.salofresh.mapper.chat;

import com.salofresh.dto.chat.ChatConversationResponse;
import com.salofresh.dto.chat.ChatCustomerSummary;
import com.salofresh.dto.chat.ChatMessageResponse;
import com.salofresh.dto.chat.ChatSalonSummary;
import com.salofresh.entity.ChatConversation;
import com.salofresh.entity.ChatMessage;
import com.salofresh.entity.Salon;
import com.salofresh.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps {@link ChatConversation}/{@link ChatMessage} entities to their response DTOs.
 *
 * <p>The {@code mine} flag on {@link ChatMessageResponse} and the {@code unreadCount} on
 * {@link ChatConversationResponse} require request-context (the requesting user) and an
 * aggregate repository query respectively, neither of which MapStruct can derive from the
 * entity graph alone. Both are left unmapped here and are populated by the service layer.
 */
@Mapper(componentModel = "spring")
public interface ChatMapper {

    @Mapping(target = "conversationId", source = "conversation.id")
    @Mapping(target = "senderId", source = "sender.id")
    @Mapping(target = "senderName", expression = "java(message.getSender().getFullName())")
    @Mapping(target = "mine", ignore = true)
    ChatMessageResponse toMessageResponse(ChatMessage message);

    @Mapping(target = "customer", source = "customer")
    @Mapping(target = "salon", source = "salon")
    @Mapping(target = "unreadCount", ignore = true)
    ChatConversationResponse toConversationResponse(ChatConversation conversation);

    @Mapping(target = "name", expression = "java(customer.getFullName())")
    @Mapping(target = "avatarUrl", source = "profileImageUrl")
    ChatCustomerSummary toCustomerSummary(User customer);

    ChatSalonSummary toSalonSummary(Salon salon);
}
