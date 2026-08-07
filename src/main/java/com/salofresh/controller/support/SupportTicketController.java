package com.salofresh.controller.support;

import com.salofresh.constant.AppConstants;
import com.salofresh.dto.support.CreateTicketRequest;
import com.salofresh.dto.support.SupportTicketResponse;
import com.salofresh.dto.support.TicketMessageRequest;
import com.salofresh.dto.support.TicketMessageResponse;
import com.salofresh.response.ApiResponse;
import com.salofresh.response.PagedResponse;
import com.salofresh.security.SecurityUtils;
import com.salofresh.service.support.SupportTicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for raising and following up on support tickets. Any authenticated user may
 * create a ticket and exchange messages on their own tickets; platform-wide administration lives
 * in {@link com.salofresh.controller.admin.AdminSupportTicketController}.
 */
@RestController
@RequestMapping(AppConstants.API_BASE_PATH + "/support/tickets")
@RequiredArgsConstructor
@Tag(name = "Support Tickets", description = "Raise and follow up on support tickets")
public class SupportTicketController {

    private final SupportTicketService supportTicketService;
    private final SecurityUtils securityUtils;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Raise a new support ticket")
    public ResponseEntity<ApiResponse<SupportTicketResponse>> createTicket(
            @Valid @RequestBody CreateTicketRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Ticket created successfully",
                supportTicketService.createTicket(userId, request)));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List the current user's support tickets")
    public ResponseEntity<ApiResponse<PagedResponse<SupportTicketResponse>>> myTickets(
            @PageableDefault(size = 20) Pageable pageable) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Tickets fetched successfully",
                supportTicketService.listMyTickets(userId, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get a support ticket (own ticket, or admin)")
    public ResponseEntity<ApiResponse<SupportTicketResponse>> getTicket(@PathVariable Long id) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Ticket fetched successfully",
                supportTicketService.getMyTicket(userId, isAdmin(), id)));
    }

    @PostMapping("/{id}/messages")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Add a message to a support ticket (own ticket, or admin)")
    public ResponseEntity<ApiResponse<TicketMessageResponse>> addMessage(
            @PathVariable Long id, @Valid @RequestBody TicketMessageRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Message added successfully",
                supportTicketService.addMessage(userId, isAdmin(), id, request)));
    }

    @GetMapping("/{id}/messages")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List messages on a support ticket (own ticket, or admin); internal notes are hidden from non-admins")
    public ResponseEntity<ApiResponse<PagedResponse<TicketMessageResponse>>> listMessages(
            @PathVariable Long id, @PageableDefault(size = 30) Pageable pageable) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Messages fetched successfully",
                supportTicketService.listMessages(userId, isAdmin(), id, pageable)));
    }

    private boolean isAdmin() {
        return securityUtils.getCurrentUser().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals("ROLE_ADMIN") || authority.equals("ROLE_SUPER_ADMIN"));
    }
}
