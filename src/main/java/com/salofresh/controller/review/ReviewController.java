package com.salofresh.controller.review;

import com.salofresh.constant.AppConstants;
import com.salofresh.dto.review.CreateReviewRequest;
import com.salofresh.dto.review.OwnerReplyRequest;
import com.salofresh.dto.review.ReportReviewRequest;
import com.salofresh.dto.review.ReviewResponse;
import com.salofresh.dto.review.UpdateReviewRequest;
import com.salofresh.response.ApiResponse;
import com.salofresh.response.PagedResponse;
import com.salofresh.security.SecurityUtils;
import com.salofresh.service.review.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(AppConstants.API_BASE_PATH + "/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Customer review authoring, salon-owner replies and moderation reporting")
public class ReviewController {

    private final ReviewService reviewService;
    private final SecurityUtils securityUtils;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create a review for a completed appointment")
    public ResponseEntity<ApiResponse<ReviewResponse>> create(@Valid @RequestBody CreateReviewRequest request) {
        Long customerId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Review created successfully",
                reviewService.create(customerId, request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update the rating/comment of the current user's own review")
    public ResponseEntity<ApiResponse<ReviewResponse>> update(@PathVariable Long id,
                                                               @Valid @RequestBody UpdateReviewRequest request) {
        Long customerId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Review updated successfully",
                reviewService.update(customerId, id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Soft-delete a review (own review, or admin)")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        Long userId = securityUtils.getCurrentUserId();
        boolean admin = securityUtils.getCurrentUser().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals("ROLE_ADMIN") || authority.equals("ROLE_SUPER_ADMIN"));
        reviewService.delete(userId, admin, id);
        return ResponseEntity.ok(ApiResponse.success("Review deleted successfully"));
    }

    @PostMapping("/{id}/reply")
    @PreAuthorize("hasRole('SALON_OWNER')")
    @Operation(summary = "Post or edit the salon owner's reply to a review")
    public ResponseEntity<ApiResponse<ReviewResponse>> reply(@PathVariable Long id,
                                                              @Valid @RequestBody OwnerReplyRequest request) {
        Long ownerUserId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Reply posted successfully",
                reviewService.reply(ownerUserId, id, request)));
    }

    @PostMapping("/{id}/report")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Report a review as inappropriate")
    public ResponseEntity<ApiResponse<Void>> report(@PathVariable Long id,
                                                     @Valid @RequestBody ReportReviewRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        reviewService.report(userId, id, request);
        return ResponseEntity.ok(ApiResponse.success("Review reported successfully"));
    }

    @PostMapping("/{id}/images")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Upload and attach an image to the current user's own review")
    public ResponseEntity<ApiResponse<ReviewResponse>> uploadImage(@PathVariable Long id,
                                                                    @RequestParam("file") MultipartFile file) {
        Long customerId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Image uploaded successfully",
                reviewService.uploadImage(customerId, id, file)));
    }

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List reviews written by the current user")
    public ResponseEntity<ApiResponse<PagedResponse<ReviewResponse>>> myReviews(
            @PageableDefault(size = 20) Pageable pageable) {
        Long customerId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Reviews fetched successfully",
                reviewService.listForCustomer(customerId, pageable)));
    }
}
