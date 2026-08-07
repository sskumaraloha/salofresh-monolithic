package com.salofresh.dto.approval;

import com.salofresh.common.enums.ApprovalActionType;
import com.salofresh.common.enums.ApprovalStatus;
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
public class ApprovalRequestResponse {

    private Long id;
    private ApprovalActionType actionType;
    private Long requestedById;
    private String requestedByName;
    private String payload;
    private String relatedEntityType;
    private String relatedEntityId;
    private String reason;
    private ApprovalStatus status;
    private Long decidedById;
    private String decidedByName;
    private Instant decidedAt;
    private String rejectionReason;
    private Instant createdAt;
}
