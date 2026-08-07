package com.salofresh.controller.admin;

import com.salofresh.common.enums.ApprovalStatus;
import com.salofresh.constant.AppConstants;
import com.salofresh.dto.approval.ApprovalRequestResponse;
import com.salofresh.dto.approval.CreateApprovalRequestRequest;
import com.salofresh.dto.approval.RejectApprovalRequestRequest;
import com.salofresh.response.ApiResponse;
import com.salofresh.response.PagedResponse;
import com.salofresh.security.SecurityUtils;
import com.salofresh.service.approval.ApprovalRequestService;
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
 * Maker-checker approval workflow for high-risk admin actions. Any admin ("maker") may queue a
 * request; only a SUPER_ADMIN ("checker") may review, approve/execute, or reject the queue, and
 * a checker may never decide on their own request.
 */
@RestController
@RequestMapping(AppConstants.API_BASE_PATH + "/admin/approvals")
@RequiredArgsConstructor
@Tag(name = "Admin - Approvals", description = "Maker-checker approval workflow for high-risk admin actions")
public class AdminApprovalRequestController {

    private final ApprovalRequestService approvalRequestService;
    private final SecurityUtils securityUtils;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Queue a high-risk admin action for checker approval (maker step)")
    public ResponseEntity<ApiResponse<ApprovalRequestResponse>> create(@Valid @RequestBody CreateApprovalRequestRequest request) {
        Long makerId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Approval request submitted successfully",
                approvalRequestService.create(makerId, request)));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @Operation(summary = "List approval requests submitted by the current admin")
    public ResponseEntity<ApiResponse<PagedResponse<ApprovalRequestResponse>>> listMine(
            @PageableDefault(size = 20) Pageable pageable) {
        Long makerId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Your approval requests fetched successfully",
                approvalRequestService.listMine(makerId, pageable)));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "List approval requests awaiting a checker decision")
    public ResponseEntity<ApiResponse<PagedResponse<ApprovalRequestResponse>>> listPending(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Pending approval requests fetched successfully",
                approvalRequestService.listPending(pageable)));
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "List all approval requests, optionally filtered by status")
    public ResponseEntity<ApiResponse<PagedResponse<ApprovalRequestResponse>>> listAll(
            @RequestParam(required = false) ApprovalStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Approval requests fetched successfully",
                approvalRequestService.listAll(status, pageable)));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Approve a pending request and execute the underlying action (checker step)")
    public ResponseEntity<ApiResponse<ApprovalRequestResponse>> approve(@PathVariable Long id) {
        Long checkerId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Approval request approved and executed successfully",
                approvalRequestService.approveAndExecute(checkerId, id)));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Reject a pending approval request (checker step)")
    public ResponseEntity<ApiResponse<ApprovalRequestResponse>> reject(
            @PathVariable Long id, @Valid @RequestBody RejectApprovalRequestRequest request) {
        Long checkerId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Approval request rejected successfully",
                approvalRequestService.reject(checkerId, id, request)));
    }
}
