package com.salofresh.service.impl.admin;

import com.salofresh.common.enums.AccountStatus;
import com.salofresh.common.enums.RoleName;
import com.salofresh.dto.admin.SuspendUserRequest;
import com.salofresh.dto.admin.UserManagementResponse;
import com.salofresh.entity.AuditLog;
import com.salofresh.entity.Role;
import com.salofresh.entity.User;
import com.salofresh.exception.ForbiddenException;
import com.salofresh.exception.ResourceNotFoundException;
import com.salofresh.repository.AuditLogRepository;
import com.salofresh.repository.UserRepository;
import com.salofresh.security.SecurityUtils;
import com.salofresh.service.admin.AdminUserService;
import jakarta.persistence.criteria.Join;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final SecurityUtils securityUtils;

    @Override
    @Transactional(readOnly = true)
    public Page<UserManagementResponse> listUsers(RoleName role, AccountStatus status, Pageable pageable) {
        Specification<User> spec = buildSpecification(role, status);
        return userRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public UserManagementResponse getById(Long userId) {
        return toResponse(findUser(userId));
    }

    @Override
    @Transactional
    public UserManagementResponse suspendUser(Long userId, SuspendUserRequest request) {
        User user = findUser(userId);
        guardAgainstPrivilegedTarget(user);

        user.setAccountStatus(AccountStatus.LOCKED);
        User saved = userRepository.save(user);

        logAction(userId, "SUSPEND", user.getAccountStatus().name(), request.getReason());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public UserManagementResponse reactivateUser(Long userId) {
        User user = findUser(userId);

        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setFailedLoginAttempts(0);
        user.setAccountLockedUntil(null);
        User saved = userRepository.save(user);

        logAction(userId, "REACTIVATE", AccountStatus.ACTIVE.name(), null);
        return toResponse(saved);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }

    private void guardAgainstPrivilegedTarget(User user) {
        boolean isSuperAdmin = user.getRoles().stream()
                .anyMatch(role -> role.getName() == RoleName.SUPER_ADMIN);
        if (isSuperAdmin) {
            throw new ForbiddenException("A super-admin account cannot be suspended");
        }
    }

    private void logAction(Long userId, String action, String newValue, String reason) {
        Long performedBy = securityUtils.isAuthenticated() ? securityUtils.getCurrentUserId() : null;
        auditLogRepository.save(AuditLog.builder()
                .entityName("User")
                .entityId(String.valueOf(userId))
                .action(action)
                .performedBy(performedBy == null ? "SYSTEM" : String.valueOf(performedBy))
                .oldValue(reason)
                .newValue(newValue)
                .build());
    }

    private Specification<User> buildSpecification(RoleName role, AccountStatus status) {
        List<Specification<User>> specs = new ArrayList<>();
        specs.add((root, query, cb) -> cb.isFalse(root.get("deleted")));

        if (status != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("accountStatus"), status));
        }
        if (role != null) {
            specs.add((root, query, cb) -> {
                query.distinct(true);
                Join<User, Role> roleJoin = root.join("roles");
                return cb.equal(roleJoin.get("name"), role);
            });
        }
        return specs.stream().reduce(Specification::and).orElse(null);
    }

    private UserManagementResponse toResponse(User user) {
        Set<String> roleNames = user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toSet());
        return UserManagementResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .phone(user.getPhone())
                .fullName(user.getFullName())
                .roles(roleNames)
                .accountStatus(user.getAccountStatus())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
