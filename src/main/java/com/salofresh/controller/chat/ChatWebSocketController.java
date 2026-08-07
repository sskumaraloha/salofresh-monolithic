package com.salofresh.controller.chat;

import com.salofresh.dto.chat.SendMessageRequest;
import com.salofresh.exception.UnauthorizedException;
import com.salofresh.service.chat.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * STOMP endpoint for live message sending. The actual broadcast to subscribers of
 * {@code /topic/conversations/{conversationId}} happens inside {@link ChatService#sendMessage},
 * so no {@code @SendTo} is used here.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;

    @MessageMapping("/conversations/{conversationId}/send")
    public void send(@DestinationVariable Long conversationId, @Valid @Payload SendMessageRequest request,
                      Principal principal) {
        Long senderId = resolveSenderId(principal);
        chatService.sendMessage(conversationId, senderId, request.getMessage());
    }

    private Long resolveSenderId(Principal principal) {
        if (principal == null || principal.getName() == null) {
            throw new UnauthorizedException("No authenticated user found on the STOMP session");
        }
        return Long.valueOf(principal.getName());
    }
}
