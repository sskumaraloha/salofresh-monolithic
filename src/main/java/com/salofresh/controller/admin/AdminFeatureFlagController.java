package com.salofresh.controller.admin;

import com.salofresh.constant.AppConstants;
import com.salofresh.dto.featureflag.CreateFeatureFlagRequest;
import com.salofresh.dto.featureflag.FeatureFlagResponse;
import com.salofresh.dto.featureflag.UpdateFeatureFlagRequest;
import com.salofresh.response.ApiResponse;
import com.salofresh.response.PagedResponse;
import com.salofresh.service.admin.AdminFeatureFlagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(AppConstants.API_BASE_PATH + "/admin/feature-flags")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
@Tag(name = "Admin - Feature Flags", description = "Admin CRUD over runtime-toggleable feature flags / dynamic config")
public class AdminFeatureFlagController {

    private final AdminFeatureFlagService adminFeatureFlagService;

    @GetMapping
    @Operation(summary = "List all feature flags")
    public ResponseEntity<ApiResponse<PagedResponse<FeatureFlagResponse>>> list(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Feature flags fetched successfully",
                PagedResponse.from(adminFeatureFlagService.list(pageable))));
    }

    @GetMapping("/{key}")
    @Operation(summary = "Get a feature flag by its key")
    public ResponseEntity<ApiResponse<FeatureFlagResponse>> getByKey(@PathVariable String key) {
        return ResponseEntity.ok(ApiResponse.success("Feature flag fetched successfully",
                adminFeatureFlagService.getByKey(key)));
    }

    @PostMapping
    @Operation(summary = "Create a new feature flag")
    public ResponseEntity<ApiResponse<FeatureFlagResponse>> create(
            @Valid @RequestBody CreateFeatureFlagRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "Feature flag created successfully", adminFeatureFlagService.create(request)));
    }

    @PutMapping("/{key}")
    @Operation(summary = "Update an existing feature flag (patch semantics - only provided fields are applied)")
    public ResponseEntity<ApiResponse<FeatureFlagResponse>> update(
            @PathVariable String key, @Valid @RequestBody UpdateFeatureFlagRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Feature flag updated successfully",
                adminFeatureFlagService.update(key, request)));
    }

    @DeleteMapping("/{key}")
    @Operation(summary = "Delete a feature flag")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String key) {
        adminFeatureFlagService.delete(key);
        return ResponseEntity.ok(ApiResponse.success("Feature flag deleted successfully"));
    }
}
