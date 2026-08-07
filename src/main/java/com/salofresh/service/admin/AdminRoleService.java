package com.salofresh.service.admin;

import com.salofresh.dto.admin.CreatePermissionRequest;
import com.salofresh.dto.admin.PermissionResponse;
import com.salofresh.dto.admin.RoleResponse;
import com.salofresh.dto.admin.UpdateRolePermissionsRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AdminRoleService {

    List<RoleResponse> listRoles();

    RoleResponse getRole(Long roleId);

    RoleResponse updateRolePermissions(Long adminUserId, Long roleId, UpdateRolePermissionsRequest request);

    Page<PermissionResponse> listPermissions(Pageable pageable);

    PermissionResponse createPermission(String createdBy, CreatePermissionRequest request);

    void deletePermission(Long permissionId);
}
