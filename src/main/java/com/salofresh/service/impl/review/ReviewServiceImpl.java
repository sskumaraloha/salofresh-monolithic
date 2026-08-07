package com.salofresh.service.impl.review;

import com.salofresh.common.enums.BookingStatus;
import com.salofresh.common.enums.ReviewStatus;
import com.salofresh.dto.admin.ModerateReviewRequest;
import com.salofresh.dto.review.CreateReviewRequest;
import com.salofresh.dto.review.OwnerReplyRequest;
import com.salofresh.dto.review.ReportReviewRequest;
import com.salofresh.dto.review.ReviewResponse;
import com.salofresh.dto.review.SalonRatingBreakdownResponse;
import com.salofresh.dto.review.UpdateReviewRequest;
import com.salofresh.entity.Appointment;
import com.salofresh.entity.AuditLog;
import com.salofresh.entity.Employee;
import com.salofresh.entity.Review;
import com.salofresh.entity.ReviewImage;
import com.salofresh.entity.Salon;
import com.salofresh.event.NewReviewEvent;
import com.salofresh.event.ReviewReplyEvent;
import com.salofresh.exception.BadRequestException;
import com.salofresh.exception.ForbiddenException;
import com.salofresh.exception.ResourceNotFoundException;
import com.salofresh.file.FileStorageService;
import com.salofresh.mapper.review.ReviewMapper;
import com.salofresh.repository.AppointmentRepository;
import com.salofresh.repository.AuditLogRepository;
import com.salofresh.repository.EmployeeRepository;
import com.salofresh.repository.ReviewImageRepository;
import com.salofresh.repository.ReviewRepository;
import com.salofresh.repository.SalonRepository;
import com.salofresh.response.PagedResponse;
import com.salofresh.service.review.ReviewService;
import com.salofresh.specification.ReviewSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private static final String REVIEW_IMAGES_SUBDIRECTORY = "review-images";

    private final ReviewRepository reviewRepository;
    private final ReviewImageRepository reviewImageRepository;
    private final AppointmentRepository appointmentRepository;
    private final SalonRepository salonRepository;
    private final EmployeeRepository employeeRepository;
    private final ReviewMapper reviewMapper;
    private final FileStorageService fileStorageService;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional
    public ReviewResponse create(Long customerId, CreateReviewRequest request) {
        Appointment appointment = appointmentRepository.findById(request.getAppointmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", "id", request.getAppointmentId()));

        if (!appointment.getCustomer().getId().equals(customerId)) {
            throw new ForbiddenException("You may only review your own appointments");
        }
        if (appointment.getStatus() != BookingStatus.COMPLETED) {
            throw new BadRequestException("Only completed appointments can be reviewed");
        }
        if (reviewRepository.existsByAppointmentId(appointment.getId())) {
            throw new BadRequestException("This appointment has already been reviewed");
        }

        Employee employee = appointment.getEmployee();
        Integer employeeRating = request.getEmployeeRating();
        if (employeeRating != null && employee == null) {
            throw new BadRequestException("This appointment had no assigned employee to rate");
        }

        validateSubRating(request.getCleanlinessRating(), "Cleanliness rating");
        validateSubRating(request.getServiceQualityRating(), "Service quality rating");
        validateSubRating(request.getValueForMoneyRating(), "Value for money rating");

        Review review = Review.builder()
                .customer(appointment.getCustomer())
                .salon(appointment.getSalon())
                .employee(employee)
                .appointment(appointment)
                .salonRating(request.getSalonRating())
                .employeeRating(employee != null ? employeeRating : null)
                .cleanlinessRating(request.getCleanlinessRating())
                .serviceQualityRating(request.getServiceQualityRating())
                .valueForMoneyRating(request.getValueForMoneyRating())
                .comment(request.getComment())
                .status(ReviewStatus.VISIBLE)
                .build();
        review = reviewRepository.save(review);

        recomputeSalonRating(review.getSalon().getId());
        if (review.getEmployeeRating() != null) {
            recomputeEmployeeRating(review.getEmployee().getId());
        }

        eventPublisher.publishEvent(new NewReviewEvent(review));

        return toResponse(review);
    }

    @Override
    @Transactional
    public ReviewResponse update(Long customerId, Long reviewId, UpdateReviewRequest request) {
        Review review = getActiveReview(reviewId);
        if (!review.getCustomer().getId().equals(customerId)) {
            throw new ForbiddenException("You may only edit your own review");
        }

        Integer employeeRating = request.getEmployeeRating();
        if (employeeRating != null && review.getEmployee() == null) {
            throw new BadRequestException("This review has no associated employee to rate");
        }

        validateSubRating(request.getCleanlinessRating(), "Cleanliness rating");
        validateSubRating(request.getServiceQualityRating(), "Service quality rating");
        validateSubRating(request.getValueForMoneyRating(), "Value for money rating");

        review.setSalonRating(request.getSalonRating());
        review.setEmployeeRating(review.getEmployee() != null ? employeeRating : null);
        review.setCleanlinessRating(request.getCleanlinessRating());
        review.setServiceQualityRating(request.getServiceQualityRating());
        review.setValueForMoneyRating(request.getValueForMoneyRating());
        review.setComment(request.getComment());
        review = reviewRepository.save(review);

        recomputeSalonRating(review.getSalon().getId());
        if (review.getEmployee() != null) {
            recomputeEmployeeRating(review.getEmployee().getId());
        }

        return toResponse(review);
    }

    @Override
    @Transactional
    public void delete(Long userId, boolean admin, Long reviewId) {
        Review review = getActiveReview(reviewId);
        if (!admin && !review.getCustomer().getId().equals(userId)) {
            throw new ForbiddenException("You may only delete your own review");
        }

        review.setDeleted(true);
        reviewRepository.save(review);

        recomputeSalonRating(review.getSalon().getId());
        if (review.getEmployee() != null) {
            recomputeEmployeeRating(review.getEmployee().getId());
        }
    }

    @Override
    @Transactional
    public ReviewResponse reply(Long ownerUserId, Long reviewId, OwnerReplyRequest request) {
        Review review = getActiveReview(reviewId);
        Salon salon = review.getSalon();
        if (salon.getOwner() == null || salon.getOwner().getUser() == null
                || !salon.getOwner().getUser().getId().equals(ownerUserId)) {
            throw new ForbiddenException("Only the owner of this salon may reply to this review");
        }

        review.setOwnerReply(request.getReply());
        review.setOwnerReplyAt(Instant.now());
        review = reviewRepository.save(review);

        eventPublisher.publishEvent(new ReviewReplyEvent(review));

        return toResponse(review);
    }

    @Override
    @Transactional
    public void report(Long userId, Long reviewId, ReportReviewRequest request) {
        Review review = getActiveReview(reviewId);
        review.setStatus(ReviewStatus.REPORTED);
        review.setReportReason(request.getReason());
        reviewRepository.save(review);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ReviewResponse> listForSalon(Long salonId, Pageable pageable) {
        Specification<Review> spec = Specification.where(ReviewSpecification.hasSalon(salonId))
                .and(ReviewSpecification.isVisible())
                .and(ReviewSpecification.isNotDeleted());
        return toPagedResponse(reviewRepository.findAll(spec, pageable));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ReviewResponse> listForEmployee(Long employeeId, Pageable pageable) {
        Specification<Review> spec = Specification.where(ReviewSpecification.hasEmployee(employeeId))
                .and(ReviewSpecification.isVisible())
                .and(ReviewSpecification.isNotDeleted());
        return toPagedResponse(reviewRepository.findAll(spec, pageable));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ReviewResponse> listForCustomer(Long customerId, Pageable pageable) {
        Specification<Review> spec = Specification.where(ReviewSpecification.hasCustomer(customerId))
                .and(ReviewSpecification.isNotDeleted());
        return toPagedResponse(reviewRepository.findAll(spec, pageable));
    }

    @Override
    @Transactional
    public ReviewResponse uploadImage(Long customerId, Long reviewId, MultipartFile file) {
        Review review = getActiveReview(reviewId);
        if (!review.getCustomer().getId().equals(customerId)) {
            throw new ForbiddenException("You may only add images to your own review");
        }

        String imageUrl = fileStorageService.store(file, REVIEW_IMAGES_SUBDIRECTORY);
        reviewImageRepository.save(ReviewImage.builder()
                .review(review)
                .imageUrl(imageUrl)
                .build());

        return toResponse(review);
    }

    @Override
    @Transactional(readOnly = true)
    public SalonRatingBreakdownResponse getRatingBreakdown(Long salonId) {
        salonRepository.findById(salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Salon", "id", salonId));

        Double overallRating = reviewRepository.averageSalonRating(salonId, ReviewStatus.VISIBLE);
        long totalReviews = reviewRepository.countBySalonIdAndStatus(salonId, ReviewStatus.VISIBLE);
        Double cleanlinessRating = reviewRepository.averageCleanlinessRating(salonId, ReviewStatus.VISIBLE);
        Double serviceQualityRating = reviewRepository.averageServiceQualityRating(salonId, ReviewStatus.VISIBLE);
        Double valueForMoneyRating = reviewRepository.averageValueForMoneyRating(salonId, ReviewStatus.VISIBLE);

        return SalonRatingBreakdownResponse.builder()
                .overallRating(overallRating)
                .cleanlinessRating(cleanlinessRating)
                .serviceQualityRating(serviceQualityRating)
                .valueForMoneyRating(valueForMoneyRating)
                .totalReviews(totalReviews)
                .build();
    }

    @Override
    @Transactional
    public ReviewResponse moderate(Long adminUserId, Long reviewId, ModerateReviewRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));

        ReviewStatus oldStatus = review.getStatus();
        review.setStatus(request.getStatus());
        review = reviewRepository.save(review);

        auditLogRepository.save(AuditLog.builder()
                .entityName("Review")
                .entityId(reviewId.toString())
                .action("MODERATE")
                .performedBy(adminUserId.toString())
                .oldValue(oldStatus.name())
                .newValue(request.getStatus().name())
                .performedAt(Instant.now())
                .build());

        return toResponse(review);
    }

    private void validateSubRating(Integer rating, String label) {
        if (rating != null && (rating < 1 || rating > 5)) {
            throw new BadRequestException(label + " must be between 1 and 5");
        }
    }

    private Review getActiveReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));
        if (review.isDeleted()) {
            throw new ResourceNotFoundException("Review", "id", reviewId);
        }
        return review;
    }

    private void recomputeSalonRating(Long salonId) {
        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Salon", "id", salonId));
        Double average = reviewRepository.averageSalonRating(salonId, ReviewStatus.VISIBLE);
        long count = reviewRepository.countBySalonIdAndStatus(salonId, ReviewStatus.VISIBLE);
        salon.setRatingAverage(average != null ? average : 0.0);
        salon.setReviewCount((int) count);
        salonRepository.save(salon);
    }

    private void recomputeEmployeeRating(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId));
        Double average = reviewRepository.averageEmployeeRating(employeeId, ReviewStatus.VISIBLE);
        employee.setRatingAverage(average != null ? average : 0.0);
        employeeRepository.save(employee);
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

    private ReviewResponse toResponse(Review review) {
        List<String> images = reviewImageRepository.findAllByReviewId(review.getId()).stream()
                .map(ReviewImage::getImageUrl)
                .sorted(Comparator.naturalOrder())
                .toList();
        return reviewMapper.toResponse(review, images);
    }
}
