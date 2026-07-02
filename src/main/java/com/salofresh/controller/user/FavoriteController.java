package com.salofresh.controller.user;

import com.salofresh.constant.AppConstants;
import com.salofresh.dto.user.FavoriteSalonResponse;
import com.salofresh.response.ApiResponse;
import com.salofresh.response.PagedResponse;
import com.salofresh.security.SecurityUtils;
import com.salofresh.service.user.FavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(AppConstants.API_BASE_PATH + "/users/me/favorites")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "User Favorites", description = "Manage the authenticated user's favorite salons")
public class FavoriteController {

    private final FavoriteService favoriteService;
    private final SecurityUtils securityUtils;

    @GetMapping
    @Operation(summary = "List the current user's favorite salons")
    public ResponseEntity<ApiResponse<PagedResponse<FavoriteSalonResponse>>> listFavorites(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<FavoriteSalonResponse> response =
                favoriteService.listFavorites(securityUtils.getCurrentUserId(), pageable);
        return ResponseEntity.ok(ApiResponse.success("Favorites retrieved successfully", response));
    }

    @PostMapping("/{salonId}")
    @Operation(summary = "Add a salon to the current user's favorites")
    public ResponseEntity<ApiResponse<Void>> addFavorite(@PathVariable Long salonId) {
        favoriteService.addFavorite(securityUtils.getCurrentUserId(), salonId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Salon added to favorites"));
    }

    @DeleteMapping("/{salonId}")
    @Operation(summary = "Remove a salon from the current user's favorites")
    public ResponseEntity<ApiResponse<Void>> removeFavorite(@PathVariable Long salonId) {
        favoriteService.removeFavorite(securityUtils.getCurrentUserId(), salonId);
        return ResponseEntity.ok(ApiResponse.success("Salon removed from favorites"));
    }
}
