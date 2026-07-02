package com.salofresh.controller.review;

import com.salofresh.constant.AppConstants;
import com.salofresh.dto.review.ReviewResponse;
import com.salofresh.response.ApiResponse;
import com.salofresh.response.PagedResponse;
import com.salofresh.service.review.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(AppConstants.API_BASE_PATH + "/salons/{salonId}/reviews")
@RequiredArgsConstructor
@Tag(name = "Salon Reviews", description = "Public browsing of a salon's visible reviews")
public class SalonReviewController {

    private final ReviewService reviewService;

    @GetMapping
    @PreAuthorize("permitAll()")
    @Operation(summary = "List a salon's visible reviews")
    public ResponseEntity<ApiResponse<PagedResponse<ReviewResponse>>> list(
            @PathVariable Long salonId, @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Reviews fetched successfully",
                reviewService.listForSalon(salonId, pageable)));
    }
}
