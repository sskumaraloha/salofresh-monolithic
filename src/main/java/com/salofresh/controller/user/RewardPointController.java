package com.salofresh.controller.user;

import com.salofresh.constant.AppConstants;
import com.salofresh.dto.user.RewardPointResponse;
import com.salofresh.response.ApiResponse;
import com.salofresh.response.PagedResponse;
import com.salofresh.security.SecurityUtils;
import com.salofresh.service.user.RewardPointService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(AppConstants.API_BASE_PATH + "/users/me/reward-points")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Reward Points", description = "The authenticated user's reward point history and balance")
public class RewardPointController {

    private final RewardPointService rewardPointService;
    private final SecurityUtils securityUtils;

    @GetMapping
    @Operation(summary = "Get the current user's reward point history, paginated")
    public ResponseEntity<ApiResponse<PagedResponse<RewardPointResponse>>> getHistory(
            @PageableDefault(size = 20) Pageable pageable) {
        PagedResponse<RewardPointResponse> response =
                rewardPointService.getHistory(securityUtils.getCurrentUserId(), pageable);
        return ResponseEntity.ok(ApiResponse.success("Reward point history retrieved successfully", response));
    }

    @GetMapping("/balance")
    @Operation(summary = "Get the current user's reward point balance")
    public ResponseEntity<ApiResponse<Integer>> getBalance() {
        int balance = rewardPointService.getCurrentBalance(securityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success("Reward point balance retrieved successfully", balance));
    }
}
