package com.salofresh.controller.chat;

import com.salofresh.constant.AppConstants;
import com.salofresh.dto.chat.ChatConversationResponse;
import com.salofresh.dto.chat.ChatMessageResponse;
import com.salofresh.response.ApiResponse;
import com.salofresh.response.PagedResponse;
import com.salofresh.security.SecurityUtils;
import com.salofresh.service.chat.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for chat history and conversation management. Live message delivery happens
 * over the STOMP WebSocket channel (see {@link ChatWebSocketController}) - this controller only
 * covers get-or-create, listing, marking read and closing.
 */
@RestController
@RequestMapping(AppConstants.API_BASE_PATH + "/chat")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "In-app chat/support conversations and message history")
public class ChatController {

    private final ChatService chatService;
    private final SecurityUtils securityUtils;

    @GetMapping("/conversations")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List the current user's conversations (as customer)")
    public ResponseEntity<ApiResponse<PagedResponse<ChatConversationResponse>>> myConversations(
            @PageableDefault(size = 20) Pageable pageable) {
        Long customerId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Conversations fetched successfully",
                chatService.listConversationsForCustomer(customerId, pageable)));
    }

    @GetMapping("/salons/{salonId}/conversations")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List a salon's support conversations (salon owner only)")
    public ResponseEntity<ApiResponse<PagedResponse<ChatConversationResponse>>> salonConversations(
            @PathVariable Long salonId, @PageableDefault(size = 20) Pageable pageable) {
        Long ownerUserId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Conversations fetched successfully",
                chatService.listConversationsForSalon(salonId, pageable, ownerUserId)));
    }

    @PostMapping("/conversations/salon/{salonId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get or create the current user's SALON_SUPPORT conversation with a salon")
    public ResponseEntity<ApiResponse<ChatConversationResponse>> getOrCreateSalonConversation(
            @PathVariable Long salonId) {
        Long customerId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Conversation fetched successfully",
                chatService.getOrCreateSalonSupportConversation(customerId, salonId)));
    }

    @PostMapping("/conversations/support")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get or create the current user's PLATFORM_SUPPORT conversation")
    public ResponseEntity<ApiResponse<ChatConversationResponse>> getOrCreatePlatformSupportConversation() {
        Long customerId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Conversation fetched successfully",
                chatService.getOrCreatePlatformSupportConversation(customerId)));
    }

    @GetMapping("/conversations/{id}/messages")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Paginated message history for a conversation (participants only)")
    public ResponseEntity<ApiResponse<PagedResponse<ChatMessageResponse>>> listMessages(
            @PathVariable Long id, @PageableDefault(size = 30) Pageable pageable) {
        Long requesterId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Messages fetched successfully",
                chatService.listMessages(id, requesterId, pageable)));
    }

    @PutMapping("/conversations/{id}/read")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Mark all unread messages in a conversation as read by the current user")
    public ResponseEntity<ApiResponse<Void>> markRead(@PathVariable Long id) {
        Long readerId = securityUtils.getCurrentUserId();
        chatService.markRead(id, readerId);
        return ResponseEntity.ok(ApiResponse.success("Conversation marked as read"));
    }

    @PutMapping("/conversations/{id}/close")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Close a conversation (participants only)")
    public ResponseEntity<ApiResponse<ChatConversationResponse>> closeConversation(@PathVariable Long id) {
        Long requesterId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Conversation closed successfully",
                chatService.closeConversation(id, requesterId)));
    }
}
