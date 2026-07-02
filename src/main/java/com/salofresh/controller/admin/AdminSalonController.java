package com.salofresh.controller.admin;

import com.salofresh.common.enums.SalonStatus;
import com.salofresh.common.enums.VerificationStatus;
import com.salofresh.constant.AppConstants;
import com.salofresh.dto.admin.RejectSalonRequest;
import com.salofresh.dto.admin.SalonModerationResponse;
import com.salofresh.dto.admin.SuspendSalonRequest;
import com.salofresh.response.ApiResponse;
import com.salofresh.response.PagedResponse;
import com.salofresh.service.admin.AdminSalonService;
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
@RequestMapping(AppConstants.API_BASE_PATH + "/admin/salons")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
@Tag(name = "Admin - Salons", description = "Platform admin salon moderation (approval/rejection/suspension)")
public class AdminSalonController {

    private final AdminSalonService adminSalonService;

    @GetMapping
    @Operation(summary = "List all salons, optionally filtered by verification status and status")
    public ResponseEntity<ApiResponse<PagedResponse<SalonModerationResponse>>> list(
            @RequestParam(required = false) VerificationStatus verificationStatus,
            @RequestParam(required = false) SalonStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Salons fetched successfully",
                PagedResponse.from(adminSalonService.listAll(verificationStatus, status, pageable))));
    }

    @GetMapping("/pending")
    @Operation(summary = "List salons pending verification approval")
    public ResponseEntity<ApiResponse<PagedResponse<SalonModerationResponse>>> pending(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Pending salons fetched successfully",
                PagedResponse.from(adminSalonService.listPendingApprovals(pageable))));
    }

    @PutMapping("/{id}/approve")
    @Operation(summary = "Approve a salon's verification")
    public ResponseEntity<ApiResponse<SalonModerationResponse>> approve(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Salon approved successfully",
                adminSalonService.approve(id)));
    }

    @PutMapping("/{id}/reject")
    @Operation(summary = "Reject a salon's verification")
    public ResponseEntity<ApiResponse<SalonModerationResponse>> reject(
            @PathVariable Long id, @Valid @RequestBody RejectSalonRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Salon rejected successfully",
                adminSalonService.reject(id, request)));
    }

    @PutMapping("/{id}/suspend")
    @Operation(summary = "Suspend an active salon")
    public ResponseEntity<ApiResponse<SalonModerationResponse>> suspend(
            @PathVariable Long id, @Valid @RequestBody SuspendSalonRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Salon suspended successfully",
                adminSalonService.suspend(id, request)));
    }
}
