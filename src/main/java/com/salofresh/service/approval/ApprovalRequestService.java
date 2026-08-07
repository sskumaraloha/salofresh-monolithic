package com.salofresh.service.approval;

import com.salofresh.common.enums.ApprovalStatus;
import com.salofresh.dto.approval.ApprovalRequestResponse;
import com.salofresh.dto.approval.CreateApprovalRequestRequest;
import com.salofresh.dto.approval.RejectApprovalRequestRequest;
import com.salofresh.response.PagedResponse;
import org.springframework.data.domain.Pageable;

/**
 * Maker-checker approval workflow for high-risk admin actions. A "maker" admin queues an action
 * as a {@link com.salofresh.entity.ApprovalRequest}; a different "checker" (SUPER_ADMIN) must
 * approve it before the underlying action is actually executed, or reject it outright.
 */
public interface ApprovalRequestService {

    /**
     * Queues a new approval request on behalf of the requesting (maker) admin. Status starts as
     * PENDING; nothing is executed yet.
     */
    ApprovalRequestResponse create(Long requestedByUserId, CreateApprovalRequestRequest request);

    /**
     * Lists all PENDING approval requests awaiting a checker's decision.
     */
    PagedResponse<ApprovalRequestResponse> listPending(Pageable pageable);

    /**
     * Lists all approval requests, optionally filtered by status.
     */
    PagedResponse<ApprovalRequestResponse> listAll(ApprovalStatus status, Pageable pageable);

    /**
     * Lists approval requests submitted by a given (maker) admin.
     */
    PagedResponse<ApprovalRequestResponse> listMine(Long requestedByUserId, Pageable pageable);

    /**
     * Approves a PENDING request and executes the underlying action. The checker must be a
     * different user than the original maker. On successful execution the request is marked
     * EXECUTED; if execution fails the exception propagates, the transaction rolls back and the
     * request remains PENDING so it can be retried.
     */
    ApprovalRequestResponse approveAndExecute(Long checkerUserId, Long approvalId);

    /**
     * Rejects a PENDING request without executing anything. The checker must be a different user
     * than the original maker.
     */
    ApprovalRequestResponse reject(Long checkerUserId, Long approvalId, RejectApprovalRequestRequest request);
}
