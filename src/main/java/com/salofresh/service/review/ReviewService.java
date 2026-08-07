package com.salofresh.service.review;

import com.salofresh.dto.admin.ModerateReviewRequest;
import com.salofresh.dto.review.CreateReviewRequest;
import com.salofresh.dto.review.OwnerReplyRequest;
import com.salofresh.dto.review.ReportReviewRequest;
import com.salofresh.dto.review.ReviewResponse;
import com.salofresh.dto.review.SalonRatingBreakdownResponse;
import com.salofresh.dto.review.UpdateReviewRequest;
import com.salofresh.response.PagedResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface ReviewService {

    /**
     * Creates a review for a completed appointment belonging to the given customer.
     */
    ReviewResponse create(Long customerId, CreateReviewRequest request);

    /**
     * Updates the rating/comment of a review owned by the given customer.
     */
    ReviewResponse update(Long customerId, Long reviewId, UpdateReviewRequest request);

    /**
     * Soft-deletes a review. Allowed for the review's own customer, or an admin.
     */
    void delete(Long userId, boolean admin, Long reviewId);

    /**
     * Posts/edits the owning salon owner's reply to a review.
     */
    ReviewResponse reply(Long ownerUserId, Long reviewId, OwnerReplyRequest request);

    /**
     * Flags a review as reported by any authenticated user.
     */
    void report(Long userId, Long reviewId, ReportReviewRequest request);

    /**
     * Public, paginated listing of VISIBLE reviews for a salon.
     */
    PagedResponse<ReviewResponse> listForSalon(Long salonId, Pageable pageable);

    /**
     * Public, paginated listing of VISIBLE reviews for an employee.
     */
    PagedResponse<ReviewResponse> listForEmployee(Long employeeId, Pageable pageable);

    /**
     * Paginated listing of all (non-deleted) reviews written by a customer.
     */
    PagedResponse<ReviewResponse> listForCustomer(Long customerId, Pageable pageable);

    /**
     * Uploads and attaches an image to a review owned by the given customer.
     */
    ReviewResponse uploadImage(Long customerId, Long reviewId, MultipartFile file);

    /**
     * Public aggregate rating breakdown for a salon: overall rating plus the per-criteria
     * (cleanliness, service quality, value-for-money) averages across its VISIBLE reviews.
     */
    SalonRatingBreakdownResponse getRatingBreakdown(Long salonId);

    /**
     * Admin moderation action: sets a review's status (e.g. hides or restores it) and records
     * the change in the audit trail.
     */
    ReviewResponse moderate(Long adminUserId, Long reviewId, ModerateReviewRequest request);
}
