package com.salofresh.dto.auth;

import com.salofresh.validation.ValidOtp;
import com.salofresh.validation.ValidPassword;
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
public class ResetPasswordRequest {

    @NotBlank(message = "Email or phone is required")
    private String identifier;

    @NotBlank(message = "OTP is required")
    @ValidOtp
    private String otp;

    @NotBlank(message = "New password is required")
    @ValidPassword
    private String newPassword;
}
