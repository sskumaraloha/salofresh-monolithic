package com.salofresh.service.auth;

import com.salofresh.common.enums.OtpType;
import com.salofresh.dto.auth.ChangePasswordRequest;
import com.salofresh.dto.auth.ForgotPasswordRequest;
import com.salofresh.dto.auth.LoginRequest;
import com.salofresh.dto.auth.LoginResponse;
import com.salofresh.dto.auth.RegisterCustomerRequest;
import com.salofresh.dto.auth.RegisterSalonOwnerRequest;
import com.salofresh.dto.auth.ResetPasswordRequest;
import com.salofresh.dto.auth.UserSummaryResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {

    UserSummaryResponse registerCustomer(RegisterCustomerRequest request);

    UserSummaryResponse registerSalonOwner(RegisterSalonOwnerRequest request);

    LoginResponse login(LoginRequest request, HttpServletRequest httpRequest);

    LoginResponse refreshToken(String refreshTokenValue, HttpServletRequest httpRequest);

    void logout(String refreshTokenValue, String accessToken);

    void logoutAllDevices(Long userId);

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);

    void changePassword(Long userId, ChangePasswordRequest request);

    void verifyOtp(String identifier, OtpType otpType, String otp);

    void resendOtp(String identifier, OtpType otpType);

    UserSummaryResponse getCurrentUser(Long userId);
}
