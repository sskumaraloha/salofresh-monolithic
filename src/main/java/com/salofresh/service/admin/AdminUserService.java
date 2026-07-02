package com.salofresh.service.admin;

import com.salofresh.common.enums.AccountStatus;
import com.salofresh.common.enums.RoleName;
import com.salofresh.dto.admin.SuspendUserRequest;
import com.salofresh.dto.admin.UserManagementResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminUserService {

    Page<UserManagementResponse> listUsers(RoleName role, AccountStatus status, Pageable pageable);

    UserManagementResponse getById(Long userId);

    UserManagementResponse suspendUser(Long userId, SuspendUserRequest request);

    UserManagementResponse reactivateUser(Long userId);
}
