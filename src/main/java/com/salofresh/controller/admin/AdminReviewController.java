package com.salofresh.controller.admin;

import com.salofresh.common.enums.ReviewStatus;
import com.salofresh.constant.AppConstants;
import com.salofresh.dto.admin.ModerateReviewRequest;
import com.salofresh.dto.review.ReviewResponse;
import com.salofresh.entity.Review;
import com.salofresh.entity.ReviewImage;
import com.salofresh.mapper.review.ReviewMapper;
import com.salofresh.repository.ReviewImageRepository;
import com.salofresh.repository.ReviewRepository;
import com.salofresh.response.ApiResponse;
import com.salofresh.response.PagedResponse;
import com.salofresh.security.SecurityUtils;
import com.salofresh.service.review.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping(AppConstants.API_BASE_PATH + "/admin/reviews")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
@Tag(name = "Admin - Reviews", description = "Platform admin review moderation")
public class AdminReviewController {

    private final ReviewService reviewService;
    private final ReviewRepository reviewRepository;
    private final ReviewImageRepository reviewImageRepository;
    private final ReviewMapper reviewMapper;
    private final SecurityUtils securityUtils;

    @GetMapping
    @Operation(summary = "List reviews, optionally filtered by status")
    public ResponseEntity<ApiResponse<PagedResponse<ReviewResponse>>> list(
            @RequestParam(required = false) ReviewStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<Review> page = status != null
                ? reviewRepository.findAllByStatusOrderByCreatedAtDesc(status, pageable)
                : reviewRepository.findAllByOrderByCreatedAtDesc(pageable);
        return ResponseEntity.ok(ApiResponse.success("Reviews fetched successfully", toPagedResponse(page)));
    }

    @PutMapping("/{id}/moderate")
    @Operation(summary = "Moderate a review by setting its status (e.g. hide or restore)")
    public ResponseEntity<ApiResponse<ReviewResponse>> moderate(
            @PathVariable Long id, @Valid @RequestBody ModerateReviewRequest request) {
        Long adminUserId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Review moderated successfully",
                reviewService.moderate(adminUserId, id, request)));
    }

    private PagedResponse<ReviewResponse> toPagedResponse(Page<Review> page) {
        List<Long> reviewIds = page.getContent().stream().map(Review::getId).toList();
        Map<Long, List<String>> imagesByReviewId = reviewImageRepository.findAllByReviewIdIn(reviewIds).stream()
                .collect(Collectors.groupingBy(image -> image.getReview().getId(),
                        Collectors.mapping(ReviewImage::getImageUrl, Collectors.toList())));

        List<ReviewResponse> content = page.getContent().stream()
                .map(review -> reviewMapper.toResponse(review, imagesByReviewId.getOrDefault(review.getId(), List.of())))
                .toList();
        return PagedResponse.from(page, content);
    }
}
