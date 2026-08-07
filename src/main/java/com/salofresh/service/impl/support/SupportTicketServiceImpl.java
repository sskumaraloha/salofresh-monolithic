package com.salofresh.service.impl.support;

import com.salofresh.common.enums.TicketPriority;
import com.salofresh.common.enums.TicketStatus;
import com.salofresh.dto.support.AssignTicketRequest;
import com.salofresh.dto.support.CreateTicketRequest;
import com.salofresh.dto.support.SupportTicketResponse;
import com.salofresh.dto.support.TicketMessageRequest;
import com.salofresh.dto.support.TicketMessageResponse;
import com.salofresh.dto.support.UpdateTicketStatusRequest;
import com.salofresh.entity.SupportTicket;
import com.salofresh.entity.TicketMessage;
import com.salofresh.entity.User;
import com.salofresh.exception.ForbiddenException;
import com.salofresh.exception.ResourceNotFoundException;
import com.salofresh.mapper.support.SupportTicketMapper;
import com.salofresh.repository.SupportTicketRepository;
import com.salofresh.repository.TicketMessageRepository;
import com.salofresh.repository.UserRepository;
import com.salofresh.response.PagedResponse;
import com.salofresh.service.support.SupportTicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SupportTicketServiceImpl implements SupportTicketService {

    private final SupportTicketRepository supportTicketRepository;
    private final TicketMessageRepository ticketMessageRepository;
    private final UserRepository userRepository;
    private final SupportTicketMapper supportTicketMapper;

    @Override
    @Transactional
    public SupportTicketResponse createTicket(Long userId, CreateTicketRequest request) {
        User creator = getUserOrThrow(userId);

        SupportTicket ticket = SupportTicket.builder()
                .createdByUser(creator)
                .subject(request.getSubject())
                .category(request.getCategory())
                .priority(request.getPriority() != null ? request.getPriority() : TicketPriority.MEDIUM)
                .status(TicketStatus.OPEN)
                .build();
        ticket = supportTicketRepository.save(ticket);

        return supportTicketMapper.toTicketResponse(ticket);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<SupportTicketResponse> listMyTickets(Long userId, Pageable pageable) {
        Page<SupportTicket> page = supportTicketRepository
                .findAllByCreatedByUserIdOrderByCreatedAtDesc(userId, pageable);
        List<SupportTicketResponse> content = page.getContent().stream()
                .map(supportTicketMapper::toTicketResponse)
                .toList();
        return PagedResponse.from(page, content);
    }

    @Override
    @Transactional(readOnly = true)
    public SupportTicketResponse getMyTicket(Long userId, boolean isAdmin, Long ticketId) {
        SupportTicket ticket = getTicketOrThrow(ticketId);
        assertOwnerOrAdmin(ticket, userId, isAdmin);
        return supportTicketMapper.toTicketResponse(ticket);
    }

    @Override
    @Transactional
    public TicketMessageResponse addMessage(Long senderId, boolean senderIsAdmin, Long ticketId,
                                              TicketMessageRequest request) {
        SupportTicket ticket = getTicketOrThrow(ticketId);
        assertOwnerOrAdmin(ticket, senderId, senderIsAdmin);

        User sender = getUserOrThrow(senderId);
        boolean internalNote = senderIsAdmin && request.isInternalNote();

        TicketMessage message = TicketMessage.builder()
                .ticket(ticket)
                .sender(sender)
                .message(request.getMessage())
                .internalNote(internalNote)
                .sentAt(Instant.now())
                .build();
        message = ticketMessageRepository.save(message);

        // Touch the ticket so its updatedAt reflects the latest activity.
        supportTicketRepository.save(ticket);

        return supportTicketMapper.toMessageResponse(message);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<TicketMessageResponse> listMessages(Long requesterId, boolean requesterIsAdmin,
                                                                Long ticketId, Pageable pageable) {
        SupportTicket ticket = getTicketOrThrow(ticketId);
        assertOwnerOrAdmin(ticket, requesterId, requesterIsAdmin);

        Page<TicketMessage> page = ticketMessageRepository.findAllByTicketIdOrderBySentAtAsc(ticketId, pageable);
        List<TicketMessageResponse> content = page.getContent().stream()
                .filter(message -> requesterIsAdmin || !message.isInternalNote())
                .map(supportTicketMapper::toMessageResponse)
                .toList();
        return PagedResponse.from(page, content);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<SupportTicketResponse> listAllTickets(TicketStatus status, Pageable pageable) {
        Page<SupportTicket> page = status != null
                ? supportTicketRepository.findAllByStatusOrderByCreatedAtDesc(status, pageable)
                : supportTicketRepository.findAll(pageable);
        List<SupportTicketResponse> content = page.getContent().stream()
                .map(supportTicketMapper::toTicketResponse)
                .toList();
        return PagedResponse.from(page, content);
    }

    @Override
    @Transactional
    public SupportTicketResponse assignTicket(Long ticketId, AssignTicketRequest request) {
        SupportTicket ticket = getTicketOrThrow(ticketId);
        User assignee = getUserOrThrow(request.getAssignedToId());

        ticket.setAssignedTo(assignee);
        if (ticket.getStatus() == TicketStatus.OPEN) {
            ticket.setStatus(TicketStatus.IN_PROGRESS);
        }
        ticket = supportTicketRepository.save(ticket);

        return supportTicketMapper.toTicketResponse(ticket);
    }

    @Override
    @Transactional
    public SupportTicketResponse updateStatus(Long ticketId, UpdateTicketStatusRequest request) {
        SupportTicket ticket = getTicketOrThrow(ticketId);

        ticket.setStatus(request.getStatus());
        if (request.getStatus() == TicketStatus.RESOLVED || request.getStatus() == TicketStatus.CLOSED) {
            ticket.setResolvedAt(Instant.now());
        }
        ticket = supportTicketRepository.save(ticket);

        return supportTicketMapper.toTicketResponse(ticket);
    }

    private SupportTicket getTicketOrThrow(Long ticketId) {
        return supportTicketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("SupportTicket", "id", ticketId));
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }

    private void assertOwnerOrAdmin(SupportTicket ticket, Long userId, boolean isAdmin) {
        if (isAdmin) {
            return;
        }
        if (!ticket.getCreatedByUser().getId().equals(userId)) {
            throw new ForbiddenException("You are not permitted to access this ticket");
        }
    }
}
