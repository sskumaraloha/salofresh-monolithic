package com.salofresh.controller.admin;

import com.salofresh.common.enums.AccountStatus;
import com.salofresh.common.enums.RoleName;
import com.salofresh.constant.AppConstants;
import com.salofresh.dto.admin.SuspendUserRequest;
import com.salofresh.dto.admin.UserManagementResponse;
import com.salofresh.response.ApiResponse;
import com.salofresh.response.PagedResponse;
import com.salofresh.service.admin.AdminUserService;
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
@RequestMapping(AppConstants.API_BASE_PATH + "/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
@Tag(name = "Admin - Users", description = "Platform admin user management and moderation")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    @Operation(summary = "List platform users, optionally filtered by role and account status")
    public ResponseEntity<ApiResponse<PagedResponse<UserManagementResponse>>> list(
            @RequestParam(required = false) RoleName role,
            @RequestParam(required = false) AccountStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Users fetched successfully",
                PagedResponse.from(adminUserService.listUsers(role, status, pageable))));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single user's account details")
    public ResponseEntity<ApiResponse<UserManagementResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("User fetched successfully",
                adminUserService.getById(id)));
    }

    @PutMapping("/{id}/suspend")
    @Operation(summary = "Suspend (lock) a user's account")
    public ResponseEntity<ApiResponse<UserManagementResponse>> suspend(
            @PathVariable Long id, @Valid @RequestBody SuspendUserRequest request) {
        return ResponseEntity.ok(ApiResponse.success("User suspended successfully",
                adminUserService.suspendUser(id, request)));
    }

    @PutMapping("/{id}/reactivate")
    @Operation(summary = "Reactivate a previously suspended user's account")
    public ResponseEntity<ApiResponse<UserManagementResponse>> reactivate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("User reactivated successfully",
                adminUserService.reactivateUser(id)));
    }
}
