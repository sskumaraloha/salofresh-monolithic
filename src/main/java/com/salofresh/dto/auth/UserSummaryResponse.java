package com.salofresh.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryResponse {

    private Long id;
    private String email;
    private String phone;
    private String firstName;
    private String lastName;
    private Set<String> roles;
    private boolean emailVerified;
    private boolean phoneVerified;
}
