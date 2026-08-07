package com.salofresh.controller.admin;

import com.salofresh.constant.AppConstants;
import com.salofresh.dto.admin.CreatePlatformPlanRequest;
import com.salofresh.dto.admin.PlatformPlanAdminResponse;
import com.salofresh.dto.admin.UpdatePlatformPlanRequest;
import com.salofresh.response.ApiResponse;
import com.salofresh.response.PagedResponse;
import com.salofresh.security.SecurityUtils;
import com.salofresh.service.admin.AdminPlatformPlanService;
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
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin-only CRUD over {@link com.salofresh.entity.PlatformPlan}: platform monetization /
 * commission configuration (pricing, commission %, salon caps). Distinct from
 * {@code PlatformBillingController}, which is the salon-owner-facing subscribe-to-a-plan flow.
 */
@RestController
@RequestMapping(AppConstants.API_BASE_PATH + "/admin/platform-plans")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
@Tag(name = "Admin - Platform Plans", description = "Platform monetization / commission configuration (admin-only CRUD over platform plans)")
public class AdminPlatformPlanController {

    private final AdminPlatformPlanService adminPlatformPlanService;
    private final SecurityUtils securityUtils;

    @GetMapping
    @Operation(summary = "List all platform plans (paged)")
    public ResponseEntity<ApiResponse<PagedResponse<PlatformPlanAdminResponse>>> list(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Platform plans fetched successfully",
                PagedResponse.from(adminPlatformPlanService.list(pageable))));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a platform plan by id")
    public ResponseEntity<ApiResponse<PlatformPlanAdminResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Platform plan fetched successfully",
                adminPlatformPlanService.getById(id)));
    }

    @PostMapping
    @Operation(summary = "Create a new platform plan")
    public ResponseEntity<ApiResponse<PlatformPlanAdminResponse>> create(
            @Valid @RequestBody CreatePlatformPlanRequest request) {
        String createdBy = securityUtils.getCurrentUserId().toString();
        return ResponseEntity.ok(ApiResponse.success("Platform plan created successfully",
                adminPlatformPlanService.create(createdBy, request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a platform plan", description = "Patch semantics: only non-null fields are applied")
    public ResponseEntity<ApiResponse<PlatformPlanAdminResponse>> update(
            @PathVariable Long id, @Valid @RequestBody UpdatePlatformPlanRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Platform plan updated successfully",
                adminPlatformPlanService.update(securityUtils.getCurrentUserId(), id, request)));
    }

    @PutMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate a platform plan")
    public ResponseEntity<ApiResponse<PlatformPlanAdminResponse>> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Platform plan deactivated successfully",
                adminPlatformPlanService.deactivate(securityUtils.getCurrentUserId(), id)));
    }

    @PutMapping("/{id}/activate")
    @Operation(summary = "Activate a platform plan")
    public ResponseEntity<ApiResponse<PlatformPlanAdminResponse>> activate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Platform plan activated successfully",
                adminPlatformPlanService.activate(securityUtils.getCurrentUserId(), id)));
    }
}
