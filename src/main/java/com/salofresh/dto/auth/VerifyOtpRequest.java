package com.salofresh.dto.auth;

import com.salofresh.common.enums.OtpType;
import com.salofresh.validation.ValidOtp;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class VerifyOtpRequest {

    @NotBlank(message = "Email or phone is required")
    private String identifier;

    @NotNull(message = "OTP type is required")
    private OtpType otpType;

    @NotBlank(message = "OTP is required")
    @ValidOtp
    private String otp;
}
