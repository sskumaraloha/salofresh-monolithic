package com.salofresh.service.impl.admin;

import com.salofresh.dto.admin.CreatePermissionRequest;
import com.salofresh.dto.admin.PermissionResponse;
import com.salofresh.dto.admin.RoleResponse;
import com.salofresh.dto.admin.UpdateRolePermissionsRequest;
import com.salofresh.entity.AuditLog;
import com.salofresh.entity.Permission;
import com.salofresh.entity.Role;
import com.salofresh.exception.ConflictException;
import com.salofresh.exception.ResourceNotFoundException;
import com.salofresh.repository.AuditLogRepository;
import com.salofresh.repository.PermissionRepository;
import com.salofresh.repository.RoleRepository;
import com.salofresh.service.admin.AdminRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminRoleServiceImpl implements AdminRoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional(readOnly = true)
    public List<RoleResponse> listRoles() {
        return roleRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RoleResponse getRole(Long roleId) {
        return toResponse(findRole(roleId));
    }

    @Override
    @Transactional
    public RoleResponse updateRolePermissions(Long adminUserId, Long roleId, UpdateRolePermissionsRequest request) {
        Role role = findRole(roleId);

        String oldValue = role.getPermissions().stream()
                .map(Permission::getName)
                .sorted()
                .collect(Collectors.joining(", "));

        List<Permission> newPermissions = permissionRepository.findAllById(request.getPermissionIds());
        if (newPermissions.size() != request.getPermissionIds().size()) {
            Set<Long> foundIds = newPermissions.stream().map(Permission::getId).collect(Collectors.toSet());
            Long missingId = request.getPermissionIds().stream()
                    .filter(id -> !foundIds.contains(id))
                    .findFirst()
                    .orElse(null);
            throw new ResourceNotFoundException("Permission", "id", missingId);
        }

        role.setPermissions(new HashSet<>(newPermissions));
        Role saved = roleRepository.save(role);

        String newValue = newPermissions.stream()
                .map(Permission::getName)
                .sorted()
                .collect(Collectors.joining(", "));

        auditLogRepository.save(AuditLog.builder()
                .entityName("Role")
                .entityId(roleId.toString())
                .action("UPDATE_PERMISSIONS")
                .performedBy(adminUserId.toString())
                .oldValue(oldValue)
                .newValue(newValue)
                .build());

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PermissionResponse> listPermissions(Pageable pageable) {
        return permissionRepository.findAllByOrderByNameAsc(pageable).map(this::toResponse);
    }

    @Override
    @Transactional
    public PermissionResponse createPermission(String createdBy, CreatePermissionRequest request) {
        if (permissionRepository.existsByNameIgnoreCase(request.getName())) {
            throw new ConflictException("Permission already exists with name: '%s'".formatted(request.getName()));
        }

        Permission permission = Permission.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
        permission.setCreatedBy(createdBy);

        return toResponse(permissionRepository.save(permission));
    }

    @Override
    @Transactional
    public void deletePermission(Long permissionId) {
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Permission", "id", permissionId));
        permission.setDeleted(true);
        permissionRepository.save(permission);
    }

    private Role findRole(Long roleId) {
        return roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", roleId));
    }

    private RoleResponse toResponse(Role role) {
        List<PermissionResponse> permissions = role.getPermissions().stream()
                .map(this::toResponse)
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                .toList();
        return RoleResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .permissions(permissions)
                .build();
    }

    private PermissionResponse toResponse(Permission permission) {
        return PermissionResponse.builder()
                .id(permission.getId())
                .name(permission.getName())
                .description(permission.getDescription())
                .build();
    }
}
