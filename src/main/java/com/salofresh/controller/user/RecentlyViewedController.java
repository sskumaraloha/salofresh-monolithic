package com.salofresh.controller.user;

import com.salofresh.constant.AppConstants;
import com.salofresh.dto.user.RecentlyViewedResponse;
import com.salofresh.response.ApiResponse;
import com.salofresh.security.SecurityUtils;
import com.salofresh.service.user.FavoriteService;
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

import java.util.List;

@RestController
@RequestMapping(AppConstants.API_BASE_PATH + "/users/me/recently-viewed")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Recently Viewed Salons", description = "The authenticated user's recently viewed salons")
public class RecentlyViewedController {

    private final FavoriteService favoriteService;
    private final SecurityUtils securityUtils;

    @GetMapping
    @Operation(summary = "List the current user's recently viewed salons, most recent first")
    public ResponseEntity<ApiResponse<List<RecentlyViewedResponse>>> listRecentlyViewed(
            @PageableDefault(size = 20) Pageable pageable) {
        List<RecentlyViewedResponse> response =
                favoriteService.listRecentlyViewed(securityUtils.getCurrentUserId(), pageable);
        return ResponseEntity.ok(ApiResponse.success("Recently viewed salons retrieved successfully", response));
    }
}
