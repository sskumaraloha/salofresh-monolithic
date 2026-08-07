package com.salofresh.dto.support;

import com.salofresh.common.enums.TicketCategory;
import com.salofresh.common.enums.TicketPriority;
import com.salofresh.common.enums.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportTicketResponse {

    private Long id;
    private Long createdByUserId;
    private String subject;
    private TicketCategory category;
    private TicketPriority priority;
    private TicketStatus status;
    private Long assignedToId;
    private String assignedToName;
    private Instant resolvedAt;
    private Instant createdAt;
}
