package com.salofresh.dto.approval;

import com.salofresh.common.enums.ApprovalActionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Submitted by a "maker" admin to queue a high-risk action for checker approval. The action is
 * NOT executed until a different admin (SUPER_ADMIN) approves it via the approvals workflow.
 *
 * <p>{@code payloadJson} is the raw JSON of the parameters the target action needs, e.g.:
 * <ul>
 *   <li>SALON_SUSPENSION: {@code {"salonId": 5, "reason": "repeated fraud"}}</li>
 *   <li>USER_BAN: {@code {"userId": 10, "reason": "policy violation"}}</li>
 *   <li>LARGE_REFUND: {@code {"paymentId": 42, "amount": 500.00, "reason": "goodwill refund"}}
 *       ({@code amount} is optional — omit to refund the full remaining amount)</li>
 *   <li>PAYOUT_OVERRIDE: {@code {"payoutId": 7, "netPayoutAmount": 1200.00, "status": "PAID"}}
 *       (both fields optional, at least one should be supplied)</li>
 *   <li>OTHER: free-form JSON, recorded for manual follow-up only</li>
 * </ul>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateApprovalRequestRequest {

    @NotNull(message = "Action type is required")
    private ApprovalActionType actionType;

    @NotBlank(message = "Payload JSON is required")
    private String payloadJson;

    @Size(max = 30, message = "Related entity type must not exceed 30 characters")
    private String relatedEntityType;

    @Size(max = 50, message = "Related entity id must not exceed 50 characters")
    private String relatedEntityId;

    @Size(max = 500, message = "Reason must not exceed 500 characters")
    private String reason;
}
