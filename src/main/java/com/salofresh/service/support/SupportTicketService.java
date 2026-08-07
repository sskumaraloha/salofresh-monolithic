package com.salofresh.service.support;

import com.salofresh.common.enums.TicketStatus;
import com.salofresh.dto.support.AssignTicketRequest;
import com.salofresh.dto.support.CreateTicketRequest;
import com.salofresh.dto.support.SupportTicketResponse;
import com.salofresh.dto.support.TicketMessageRequest;
import com.salofresh.dto.support.TicketMessageResponse;
import com.salofresh.dto.support.UpdateTicketStatusRequest;
import com.salofresh.response.PagedResponse;
import org.springframework.data.domain.Pageable;

public interface SupportTicketService {

    /**
     * Raises a new support ticket on behalf of {@code userId}.
     */
    SupportTicketResponse createTicket(Long userId, CreateTicketRequest request);

    /**
     * Paginated tickets raised by {@code userId}, most recently created first.
     */
    PagedResponse<SupportTicketResponse> listMyTickets(Long userId, Pageable pageable);

    /**
     * Fetches a single ticket. Non-admin callers may only fetch tickets they created.
     */
    SupportTicketResponse getMyTicket(Long userId, boolean isAdmin, Long ticketId);

    /**
     * Adds a message to a ticket. Non-admin senders must be the ticket's creator, and
     * {@code internalNote} is forced to {@code false} for them regardless of the request.
     */
    TicketMessageResponse addMessage(Long senderId, boolean senderIsAdmin, Long ticketId, TicketMessageRequest request);

    /**
     * Paginated messages for a ticket, oldest first. Non-admin requesters never see messages
     * flagged as internal notes.
     */
    PagedResponse<TicketMessageResponse> listMessages(Long requesterId, boolean requesterIsAdmin, Long ticketId,
                                                        Pageable pageable);

    /**
     * Admin-only: paginated tickets across the platform, optionally filtered by status.
     */
    PagedResponse<SupportTicketResponse> listAllTickets(TicketStatus status, Pageable pageable);

    /**
     * Admin-only: assigns a ticket to a staff user.
     */
    SupportTicketResponse assignTicket(Long ticketId, AssignTicketRequest request);

    /**
     * Admin-only: updates a ticket's status. Transitioning to RESOLVED or CLOSED stamps
     * {@code resolvedAt}.
     */
    SupportTicketResponse updateStatus(Long ticketId, UpdateTicketStatusRequest request);
}
