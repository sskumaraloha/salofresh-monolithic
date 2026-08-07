package com.salofresh.controller.gdpr;

import com.salofresh.constant.AppConstants;
import com.salofresh.dto.gdpr.CreateDataRequestRequest;
import com.salofresh.dto.gdpr.DataSubjectRequestResponse;
import com.salofresh.response.ApiResponse;
import com.salofresh.response.PagedResponse;
import com.salofresh.security.SecurityUtils;
import com.salofresh.service.gdpr.DataSubjectRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(AppConstants.API_BASE_PATH + "/privacy/requests")
@RequiredArgsConstructor
@Tag(name = "Privacy Requests", description = "GDPR/DPDP data subject export & deletion requests raised by the current user")
public class DataSubjectRequestController {

    private final DataSubjectRequestService dataSubjectRequestService;
    private final SecurityUtils securityUtils;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Raise a new data export or account deletion request")
    public ResponseEntity<ApiResponse<DataSubjectRequestResponse>> create(
            @Valid @RequestBody CreateDataRequestRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Data subject request submitted successfully",
                dataSubjectRequestService.create(userId, request)));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List the current user's own data subject requests")
    public ResponseEntity<ApiResponse<PagedResponse<DataSubjectRequestResponse>>> listMine(
            @PageableDefault(size = 20) Pageable pageable) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Data subject requests fetched successfully",
                dataSubjectRequestService.listMine(userId, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get one of the current user's own data subject requests")
    public ResponseEntity<ApiResponse<DataSubjectRequestResponse>> getMine(@PathVariable Long id) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Data subject request fetched successfully",
                dataSubjectRequestService.getMine(userId, id)));
    }
}
