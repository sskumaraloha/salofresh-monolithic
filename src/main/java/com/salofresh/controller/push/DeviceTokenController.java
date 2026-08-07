package com.salofresh.controller.push;

import com.salofresh.constant.AppConstants;
import com.salofresh.dto.push.DeviceTokenRegisterRequest;
import com.salofresh.entity.User;
import com.salofresh.exception.ResourceNotFoundException;
import com.salofresh.push.DeviceTokenService;
import com.salofresh.repository.UserRepository;
import com.salofresh.response.ApiResponse;
import com.salofresh.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(AppConstants.API_BASE_PATH + "/users/me/device-tokens")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Device Tokens", description = "Register/unregister the current user's push notification device tokens")
public class DeviceTokenController {

    private final DeviceTokenService deviceTokenService;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;

    @PostMapping
    @Operation(summary = "Register (or re-register) a push notification device token for the current user")
    public ResponseEntity<ApiResponse<Void>> registerToken(@Valid @RequestBody DeviceTokenRegisterRequest request) {
        User user = loadCurrentUser();
        deviceTokenService.registerToken(user, request.getToken(), request.getPlatform());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Device token registered successfully"));
    }

    @DeleteMapping("/{token}")
    @Operation(summary = "Unregister a push notification device token")
    public ResponseEntity<ApiResponse<Void>> unregisterToken(@PathVariable String token) {
        deviceTokenService.unregisterToken(token);
        return ResponseEntity.ok(ApiResponse.success("Device token unregistered successfully"));
    }

    private User loadCurrentUser() {
        Long userId = securityUtils.getCurrentUserId();
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }
}
