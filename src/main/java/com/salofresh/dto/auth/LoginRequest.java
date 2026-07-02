package com.salofresh.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Email or phone is required")
    private String identifier;

    @NotBlank(message = "Password is required")
    private String password;

    @Builder.Default
    private boolean rememberMe = false;
}
