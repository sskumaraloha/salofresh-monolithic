package com.salofresh.controller.admin;

import com.salofresh.constant.AppConstants;
import com.salofresh.dto.admin.RoleResponse;
import com.salofresh.dto.admin.UpdateRolePermissionsRequest;
import com.salofresh.response.ApiResponse;
import com.salofresh.security.SecurityUtils;
import com.salofresh.service.admin.AdminRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(AppConstants.API_BASE_PATH + "/admin/roles")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
@Tag(name = "Admin - Roles", description = "Platform-wide role and permission-assignment management (SUPER_ADMIN only)")
public class AdminRoleController {

    private final AdminRoleService adminRoleService;
    private final SecurityUtils securityUtils;

    @GetMapping
    @Operation(summary = "List all system roles with their assigned permissions")
    public ResponseEntity<ApiResponse<List<RoleResponse>>> listRoles() {
        return ResponseEntity.ok(ApiResponse.success("Roles fetched successfully",
                adminRoleService.listRoles()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single role with its assigned permissions")
    public ResponseEntity<ApiResponse<RoleResponse>> getRole(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Role fetched successfully",
                adminRoleService.getRole(id)));
    }

    @PutMapping("/{id}/permissions")
    @Operation(summary = "Replace the full set of permissions assigned to a role")
    public ResponseEntity<ApiResponse<RoleResponse>> updateRolePermissions(
            @PathVariable Long id, @Valid @RequestBody UpdateRolePermissionsRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Role permissions updated successfully",
                adminRoleService.updateRolePermissions(securityUtils.getCurrentUserId(), id, request)));
    }
}
