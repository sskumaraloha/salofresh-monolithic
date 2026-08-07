package com.salofresh.controller.featureflag;

import com.salofresh.constant.AppConstants;
import com.salofresh.response.ApiResponse;
import com.salofresh.service.featureflag.FeatureFlagQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Client-facing feature flag endpoint. Requires authentication (any logged-in user/client), but
 * is intentionally not admin-restricted and not listed in
 * {@code SecurityConstants.PUBLIC_ENDPOINTS} - clients call this to know which flags are
 * currently active so they can drive client-side behavior.
 */
@RestController
@RequestMapping(AppConstants.API_BASE_PATH + "/feature-flags")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Feature Flags", description = "Fetch currently active feature flags for client-side use")
public class FeatureFlagController {

    private final FeatureFlagQueryService featureFlagQueryService;

    @GetMapping
    @Operation(summary = "List all active feature flags as a key-value map")
    public ResponseEntity<ApiResponse<Map<String, String>>> listActiveFlags() {
        return ResponseEntity.ok(ApiResponse.success("Active feature flags fetched successfully",
                featureFlagQueryService.getActiveFlagsMap()));
    }
}
