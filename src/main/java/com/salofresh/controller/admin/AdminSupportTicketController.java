package com.salofresh.controller.admin;

import com.salofresh.common.enums.TicketStatus;
import com.salofresh.constant.AppConstants;
import com.salofresh.dto.support.AssignTicketRequest;
import com.salofresh.dto.support.SupportTicketResponse;
import com.salofresh.dto.support.TicketMessageRequest;
import com.salofresh.dto.support.TicketMessageResponse;
import com.salofresh.dto.support.UpdateTicketStatusRequest;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Platform-admin endpoints for triaging support tickets raised by any user.
 */
@RestController
@RequestMapping(AppConstants.API_BASE_PATH + "/admin/support/tickets")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
@Tag(name = "Admin - Support Tickets", description = "Platform admin support ticket triage: list/assign/resolve")
public class AdminSupportTicketController {

    private final SupportTicketService supportTicketService;
    private final SecurityUtils securityUtils;

    @GetMapping
    @Operation(summary = "List all support tickets, optionally filtered by status")
    public ResponseEntity<ApiResponse<PagedResponse<SupportTicketResponse>>> listAll(
            @RequestParam(required = false) TicketStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Tickets fetched successfully",
                supportTicketService.listAllTickets(status, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get any support ticket")
    public ResponseEntity<ApiResponse<SupportTicketResponse>> getTicket(@PathVariable Long id) {
        Long adminId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Ticket fetched successfully",
                supportTicketService.getMyTicket(adminId, true, id)));
    }

    @GetMapping("/{id}/messages")
    @Operation(summary = "List all messages on a ticket, including internal notes")
    public ResponseEntity<ApiResponse<PagedResponse<TicketMessageResponse>>> listMessages(
            @PathVariable Long id, @PageableDefault(size = 30) Pageable pageable) {
        Long adminId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Messages fetched successfully",
                supportTicketService.listMessages(adminId, true, id, pageable)));
    }

    @PostMapping("/{id}/messages")
    @Operation(summary = "Reply to a ticket as admin (may flag the message as an internal note)")
    public ResponseEntity<ApiResponse<TicketMessageResponse>> addMessage(
            @PathVariable Long id, @Valid @RequestBody TicketMessageRequest request) {
        Long adminId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Message added successfully",
                supportTicketService.addMessage(adminId, true, id, request)));
    }

    @PutMapping("/{id}/assign")
    @Operation(summary = "Assign a ticket to a staff user")
    public ResponseEntity<ApiResponse<SupportTicketResponse>> assign(
            @PathVariable Long id, @Valid @RequestBody AssignTicketRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Ticket assigned successfully",
                supportTicketService.assignTicket(id, request)));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update a ticket's status")
    public ResponseEntity<ApiResponse<SupportTicketResponse>> updateStatus(
            @PathVariable Long id, @Valid @RequestBody UpdateTicketStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Ticket status updated successfully",
                supportTicketService.updateStatus(id, request)));
    }
}
