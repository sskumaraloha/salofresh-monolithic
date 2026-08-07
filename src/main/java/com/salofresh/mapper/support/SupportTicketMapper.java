package com.salofresh.mapper.support;

import com.salofresh.dto.support.SupportTicketResponse;
import com.salofresh.dto.support.TicketMessageResponse;
import com.salofresh.entity.SupportTicket;
import com.salofresh.entity.TicketMessage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps {@link SupportTicket}/{@link TicketMessage} entities to their response DTOs.
 */
@Mapper(componentModel = "spring")
public interface SupportTicketMapper {

    @Mapping(target = "createdByUserId", source = "createdByUser.id")
    @Mapping(target = "assignedToId", source = "assignedTo.id")
    @Mapping(target = "assignedToName", expression = "java(ticket.getAssignedTo() != null ? ticket.getAssignedTo().getFullName() : null)")
    SupportTicketResponse toTicketResponse(SupportTicket ticket);

    @Mapping(target = "senderId", source = "sender.id")
    @Mapping(target = "senderName", expression = "java(message.getSender().getFullName())")
    TicketMessageResponse toMessageResponse(TicketMessage message);
}
