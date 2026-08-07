package com.salofresh.controller.admin;

import com.salofresh.constant.AppConstants;
import com.salofresh.dto.admin.CreatePermissionRequest;
import com.salofresh.dto.admin.PermissionResponse;
import com.salofresh.response.ApiResponse;
import com.salofresh.response.PagedResponse;
import com.salofresh.security.SecurityUtils;
import com.salofresh.service.admin.AdminRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(AppConstants.API_BASE_PATH + "/admin/permissions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
@Tag(name = "Admin - Permissions", description = "Platform-wide permission catalog management (SUPER_ADMIN only)")
public class AdminPermissionController {

    private final AdminRoleService adminRoleService;
    private final SecurityUtils securityUtils;

    @GetMapping
    @Operation(summary = "List all permissions in the platform catalog")
    public ResponseEntity<ApiResponse<PagedResponse<PermissionResponse>>> listPermissions(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Permissions fetched successfully",
                PagedResponse.from(adminRoleService.listPermissions(pageable))));
    }

    @PostMapping
    @Operation(summary = "Create a new permission in the platform catalog")
    public ResponseEntity<ApiResponse<PermissionResponse>> createPermission(
            @Valid @RequestBody CreatePermissionRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Permission created successfully",
                adminRoleService.createPermission(securityUtils.getCurrentUser().getEmail(), request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a permission from the platform catalog")
    public ResponseEntity<ApiResponse<Void>> deletePermission(@PathVariable Long id) {
        adminRoleService.deletePermission(id);
        return ResponseEntity.ok(ApiResponse.success("Permission deleted successfully"));
    }
}
