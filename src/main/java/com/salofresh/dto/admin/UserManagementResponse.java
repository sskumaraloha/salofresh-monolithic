package com.salofresh.dto.admin;

import com.salofresh.common.enums.AccountStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserManagementResponse {

    private Long id;
    private String email;
    private String phone;
    private String fullName;
    private Set<String> roles;
    private AccountStatus accountStatus;
    private Instant createdAt;
}
