package com.salofresh.controller.auth;

import com.salofresh.constant.AppConstants;
import com.salofresh.constant.SecurityConstants;
import com.salofresh.dto.auth.ChangePasswordRequest;
import com.salofresh.dto.auth.ForgotPasswordRequest;
import com.salofresh.dto.auth.LoginRequest;
import com.salofresh.dto.auth.LoginResponse;
import com.salofresh.dto.auth.RefreshTokenRequest;
import com.salofresh.dto.auth.RegisterCustomerRequest;
import com.salofresh.dto.auth.RegisterSalonOwnerRequest;
import com.salofresh.dto.auth.ResendOtpRequest;
import com.salofresh.dto.auth.ResetPasswordRequest;
import com.salofresh.dto.auth.UserSummaryResponse;
import com.salofresh.dto.auth.VerifyOtpRequest;
import com.salofresh.response.ApiResponse;
import com.salofresh.security.SecurityUtils;
import com.salofresh.service.auth.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(AppConstants.API_BASE_PATH + "/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Registration, login, token and password management APIs")
public class AuthController {

    private final AuthService authService;
    private final SecurityUtils securityUtils;

    @PostMapping("/register/customer")
    @Operation(summary = "Register a new customer account")
    public ResponseEntity<ApiResponse<UserSummaryResponse>> registerCustomer(
            @Valid @RequestBody RegisterCustomerRequest request) {
        UserSummaryResponse response = authService.registerCustomer(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Customer registered successfully. Please verify your email.", response));
    }

    @PostMapping("/register/salon-owner")
    @Operation(summary = "Register a new salon owner account")
    public ResponseEntity<ApiResponse<UserSummaryResponse>> registerSalonOwner(
            @Valid @RequestBody RegisterSalonOwnerRequest request) {
        UserSummaryResponse response = authService.registerSalonOwner(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Salon owner registered successfully. Please verify your email.", response));
    }

    @PostMapping("/login")
    @Operation(summary = "Login with email or phone and password")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request,
                                                              HttpServletRequest httpRequest) {
        LoginResponse response = authService.login(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Rotate refresh token and issue a new access/refresh token pair")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request,
                                                                     HttpServletRequest httpRequest) {
        LoginResponse response = authService.refreshToken(request.getRefreshToken(), httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", response));
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Logout current device by revoking the refresh token and blacklisting the access token")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody RefreshTokenRequest request,
                                                      HttpServletRequest httpRequest) {
        authService.logout(request.getRefreshToken(), resolveAccessToken(httpRequest));
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
    }

    @PostMapping("/logout-all")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Logout from all devices by revoking every refresh token for the current user")
    public ResponseEntity<ApiResponse<Void>> logoutAllDevices() {
        authService.logoutAllDevices(securityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success("Logged out from all devices successfully"));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request an OTP to reset a forgotten password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success("OTP sent successfully"));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password using a verified OTP")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset successfully"));
    }

    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Change password for the currently authenticated user")
    public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(securityUtils.getCurrentUserId(), request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully"));
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Verify an OTP for email/phone verification or password reset")
    public ResponseEntity<ApiResponse<Void>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        authService.verifyOtp(request.getIdentifier(), request.getOtpType(), request.getOtp());
        return ResponseEntity.ok(ApiResponse.success("OTP verified successfully"));
    }

    @PostMapping("/resend-otp")
    @Operation(summary = "Resend an OTP")
    public ResponseEntity<ApiResponse<Void>> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        authService.resendOtp(request.getIdentifier(), request.getOtpType());
        return ResponseEntity.ok(ApiResponse.success("OTP resent successfully"));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get the currently authenticated user's profile summary")
    public ResponseEntity<ApiResponse<UserSummaryResponse>> me() {
        UserSummaryResponse response = authService.getCurrentUser(securityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success("User fetched successfully", response));
    }

    private String resolveAccessToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(SecurityConstants.AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(SecurityConstants.TOKEN_PREFIX)) {
            return bearerToken.substring(SecurityConstants.TOKEN_PREFIX.length());
        }
        return null;
    }
}
