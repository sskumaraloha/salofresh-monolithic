package com.salofresh.service.impl.chat;

import com.salofresh.common.enums.ChatStatus;
import com.salofresh.common.enums.ChatType;
import com.salofresh.common.enums.NotificationChannel;
import com.salofresh.common.enums.NotificationType;
import com.salofresh.common.enums.RoleName;
import com.salofresh.dto.chat.ChatConversationResponse;
import com.salofresh.dto.chat.ChatMessageResponse;
import com.salofresh.entity.ChatConversation;
import com.salofresh.entity.ChatMessage;
import com.salofresh.entity.Salon;
import com.salofresh.entity.User;
import com.salofresh.exception.ForbiddenException;
import com.salofresh.exception.ResourceNotFoundException;
import com.salofresh.mapper.chat.ChatMapper;
import com.salofresh.notification.NotificationService;
import com.salofresh.repository.ChatConversationRepository;
import com.salofresh.repository.ChatMessageRepository;
import com.salofresh.repository.SalonRepository;
import com.salofresh.repository.UserRepository;
import com.salofresh.response.PagedResponse;
import com.salofresh.service.chat.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatConversationRepository chatConversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final SalonRepository salonRepository;
    private final ChatMapper chatMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public ChatConversationResponse getOrCreateSalonSupportConversation(Long customerId, Long salonId) {
        ChatConversation conversation = chatConversationRepository
                .findByCustomerIdAndSalonIdAndType(customerId, salonId, ChatType.SALON_SUPPORT)
                .orElseGet(() -> createSalonSupportConversation(customerId, salonId));
        return toConversationResponseWithUnread(conversation, customerId);
    }

    @Override
    @Transactional
    public ChatConversationResponse getOrCreatePlatformSupportConversation(Long customerId) {
        ChatConversation conversation = chatConversationRepository
                .findByCustomerIdAndTypeAndSalonIsNull(customerId, ChatType.PLATFORM_SUPPORT)
                .orElseGet(() -> createPlatformSupportConversation(customerId));
        return toConversationResponseWithUnread(conversation, customerId);
    }

    @Override
    @Transactional
    public ChatMessageResponse sendMessage(Long conversationId, Long senderId, String text) {
        ChatConversation conversation = getConversationOrThrow(conversationId);
        assertParticipant(conversation, senderId);

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", senderId));

        Instant now = Instant.now();
        ChatMessage message = ChatMessage.builder()
                .conversation(conversation)
                .sender(sender)
                .message(text)
                .sentAt(now)
                .build();
        message = chatMessageRepository.save(message);

        conversation.setLastMessageAt(now);
        chatConversationRepository.save(conversation);

        ChatMessageResponse response = chatMapper.toMessageResponse(message);
        // The broadcast payload is shared by every subscriber of this topic, so "mine" here is
        // necessarily relative to the sender; REST history recomputes it per-requester.
        response.setMine(true);

        messagingTemplate.convertAndSend("/topic/conversations/" + conversationId, response);
        notifyOtherParticipant(conversation, sender);

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ChatMessageResponse> listMessages(Long conversationId, Long requesterId, Pageable pageable) {
        ChatConversation conversation = getConversationOrThrow(conversationId);
        assertParticipant(conversation, requesterId);

        Page<ChatMessage> page = chatMessageRepository.findAllByConversationIdOrderBySentAtDesc(conversationId, pageable);
        List<ChatMessageResponse> content = page.getContent().stream()
                .map(chatMessage -> {
                    ChatMessageResponse response = chatMapper.toMessageResponse(chatMessage);
                    response.setMine(chatMessage.getSender().getId().equals(requesterId));
                    return response;
                })
                .toList();
        return PagedResponse.from(page, content);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ChatConversationResponse> listConversationsForCustomer(Long customerId, Pageable pageable) {
        Page<ChatConversation> page = chatConversationRepository
                .findAllByCustomerIdOrderByLastMessageAtDesc(customerId, pageable);
        return toConversationPagedResponse(page, customerId);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ChatConversationResponse> listConversationsForSalon(Long salonId, Pageable pageable,
                                                                              Long ownerUserId) {
        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Salon", "id", salonId));
        if (!isSalonOwner(salon, ownerUserId)) {
            throw new ForbiddenException("Only the owner of this salon may view its conversations");
        }

        Page<ChatConversation> page = chatConversationRepository
                .findAllBySalonIdOrderByLastMessageAtDesc(salonId, pageable);
        return toConversationPagedResponse(page, ownerUserId);
    }

    @Override
    @Transactional
    public void markRead(Long conversationId, Long readerId) {
        ChatConversation conversation = getConversationOrThrow(conversationId);
        assertParticipant(conversation, readerId);
        chatMessageRepository.markConversationAsRead(conversationId, readerId, Instant.now());
    }

    @Override
    @Transactional
    public ChatConversationResponse closeConversation(Long conversationId, Long requesterId) {
        ChatConversation conversation = getConversationOrThrow(conversationId);
        assertParticipant(conversation, requesterId);

        conversation.setStatus(ChatStatus.CLOSED);
        conversation = chatConversationRepository.save(conversation);

        return toConversationResponseWithUnread(conversation, requesterId);
    }

    private ChatConversation createSalonSupportConversation(Long customerId, Long salonId) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", customerId));
        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Salon", "id", salonId));

        ChatConversation conversation = ChatConversation.builder()
                .customer(customer)
                .salon(salon)
                .type(ChatType.SALON_SUPPORT)
                .status(ChatStatus.OPEN)
                .lastMessageAt(Instant.now())
                .build();
        return chatConversationRepository.save(conversation);
    }

    private ChatConversation createPlatformSupportConversation(Long customerId) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", customerId));

        ChatConversation conversation = ChatConversation.builder()
                .customer(customer)
                .salon(null)
                .type(ChatType.PLATFORM_SUPPORT)
                .status(ChatStatus.OPEN)
                .lastMessageAt(Instant.now())
                .build();
        return chatConversationRepository.save(conversation);
    }

    private ChatConversation getConversationOrThrow(Long conversationId) {
        return chatConversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "id", conversationId));
    }

    /**
     * Verifies that {@code userId} may act on this conversation: the conversation's own customer,
     * OR - for SALON_SUPPORT - the salon's owning user, OR - for PLATFORM_SUPPORT - a user holding
     * ROLE_ADMIN/ROLE_SUPER_ADMIN.
     */
    private void assertParticipant(ChatConversation conversation, Long userId) {
        if (conversation.getCustomer().getId().equals(userId)) {
            return;
        }
        if (conversation.getType() == ChatType.SALON_SUPPORT
                && conversation.getSalon() != null
                && isSalonOwner(conversation.getSalon(), userId)) {
            return;
        }
        if (conversation.getType() == ChatType.PLATFORM_SUPPORT && isPlatformAdmin(userId)) {
            return;
        }
        throw new ForbiddenException("You are not a participant in this conversation");
    }

    private boolean isSalonOwner(Salon salon, Long userId) {
        return salon.getOwner() != null && salon.getOwner().getUser() != null
                && salon.getOwner().getUser().getId().equals(userId);
    }

    private boolean isPlatformAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        return user.getRoles().stream()
                .map(role -> role.getName())
                .anyMatch(name -> name == RoleName.ADMIN || name == RoleName.SUPER_ADMIN);
    }

    private void notifyOtherParticipant(ChatConversation conversation, User sender) {
        try {
            User recipient = resolveNotificationRecipient(conversation, sender.getId());
            if (recipient == null) {
                return;
            }
            notificationService.createAndDispatch(recipient, NotificationType.GENERIC, NotificationChannel.IN_APP,
                    "New message", "%s sent you a new message".formatted(sender.getFullName()),
                    conversation.getId().toString(), "CHAT_CONVERSATION");
        } catch (Exception ex) {
            log.warn("Failed to dispatch chat notification for conversation {}: {}", conversation.getId(),
                    ex.getMessage());
        }
    }

    private User resolveNotificationRecipient(ChatConversation conversation, Long senderId) {
        if (conversation.getCustomer().getId().equals(senderId)) {
            if (conversation.getType() == ChatType.SALON_SUPPORT
                    && conversation.getSalon() != null
                    && conversation.getSalon().getOwner() != null) {
                return conversation.getSalon().getOwner().getUser();
            }
            // PLATFORM_SUPPORT has no single fixed admin recipient - skip best-effort notification.
            return null;
        }
        return conversation.getCustomer();
    }

    private PagedResponse<ChatConversationResponse> toConversationPagedResponse(Page<ChatConversation> page,
                                                                                 Long viewerId) {
        List<ChatConversationResponse> content = page.getContent().stream()
                .map(conversation -> toConversationResponseWithUnread(conversation, viewerId))
                .toList();
        return PagedResponse.from(page, content);
    }

    private ChatConversationResponse toConversationResponseWithUnread(ChatConversation conversation, Long viewerId) {
        ChatConversationResponse response = chatMapper.toConversationResponse(conversation);
        long unread = chatMessageRepository.countByConversationIdAndSenderIdNotAndReadAtIsNull(
                conversation.getId(), viewerId);
        response.setUnreadCount(unread);
        return response;
    }
}
