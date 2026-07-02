package com.salofresh.controller.user;

import com.salofresh.constant.AppConstants;
import com.salofresh.dto.user.UpdateProfileRequest;
import com.salofresh.dto.user.UserProfileResponse;
import com.salofresh.response.ApiResponse;
import com.salofresh.security.SecurityUtils;
import com.salofresh.service.user.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(AppConstants.API_BASE_PATH + "/users/me")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "User Profile", description = "Manage the authenticated user's profile")
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final SecurityUtils securityUtils;

    @GetMapping
    @Operation(summary = "Get the current user's profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile() {
        UserProfileResponse response = userProfileService.getProfile(securityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success("Profile retrieved successfully", response));
    }

    @PutMapping
    @Operation(summary = "Update the current user's profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request) {
        UserProfileResponse response = userProfileService.updateProfile(securityUtils.getCurrentUserId(), request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", response));
    }

    @PostMapping("/profile-image")
    @Operation(summary = "Upload or replace the current user's profile picture")
    public ResponseEntity<ApiResponse<UserProfileResponse>> uploadProfileImage(
            @RequestParam("file") MultipartFile file) {
        UserProfileResponse response = userProfileService.uploadProfileImage(securityUtils.getCurrentUserId(), file);
        return ResponseEntity.ok(ApiResponse.success("Profile image uploaded successfully", response));
    }

    @DeleteMapping("/account")
    @Operation(summary = "Soft-delete the current user's account")
    public ResponseEntity<ApiResponse<Void>> deleteAccount() {
        userProfileService.deleteAccount(securityUtils.getCurrentUserId());
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success("Account deleted successfully"));
    }

    @PostMapping("/deactivate")
    @Operation(summary = "Deactivate the current user's account")
    public ResponseEntity<ApiResponse<Void>> deactivateAccount() {
        userProfileService.deactivateAccount(securityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success("Account deactivated successfully"));
    }
}
