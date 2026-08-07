package com.salofresh.controller.admin;

import com.salofresh.common.enums.DataRequestStatus;
import com.salofresh.constant.AppConstants;
import com.salofresh.dto.gdpr.DataSubjectRequestResponse;
import com.salofresh.dto.gdpr.RejectDataRequestRequest;
import com.salofresh.response.ApiResponse;
import com.salofresh.response.PagedResponse;
import com.salofresh.security.SecurityUtils;
import com.salofresh.service.gdpr.DataSubjectRequestService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(AppConstants.API_BASE_PATH + "/admin/privacy/requests")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
@Tag(name = "Admin - Privacy Requests", description = "Platform admin processing of GDPR/DPDP data export & account deletion requests")
public class AdminDataSubjectRequestController {

    private final DataSubjectRequestService dataSubjectRequestService;
    private final SecurityUtils securityUtils;

    @GetMapping
    @Operation(summary = "List all data subject requests, optionally filtered by status")
    public ResponseEntity<ApiResponse<PagedResponse<DataSubjectRequestResponse>>> listAll(
            @RequestParam(required = false) DataRequestStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Data subject requests fetched successfully",
                dataSubjectRequestService.listAll(status, pageable)));
    }

    @PutMapping("/{id}/process-export")
    @Operation(summary = "Process a pending EXPORT request and generate the data export file")
    public ResponseEntity<ApiResponse<DataSubjectRequestResponse>> processExport(@PathVariable Long id) {
        Long adminUserId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Data export request processed successfully",
                dataSubjectRequestService.processExport(adminUserId, id)));
    }

    @PutMapping("/{id}/process-deletion")
    @Operation(summary = "Process a pending DELETE request and soft-delete the account")
    public ResponseEntity<ApiResponse<DataSubjectRequestResponse>> processDeletion(@PathVariable Long id) {
        Long adminUserId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Account deletion request processed successfully",
                dataSubjectRequestService.processDeletion(adminUserId, id)));
    }

    @PutMapping("/{id}/reject")
    @Operation(summary = "Reject a pending data subject request")
    public ResponseEntity<ApiResponse<DataSubjectRequestResponse>> reject(
            @PathVariable Long id, @Valid @RequestBody RejectDataRequestRequest request) {
        Long adminUserId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Data subject request rejected successfully",
                dataSubjectRequestService.reject(adminUserId, id, request)));
    }
}
