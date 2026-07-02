package com.salofresh.mapper.review;

import com.salofresh.dto.review.ReviewResponse;
import com.salofresh.entity.Review;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * Maps {@link Review} entities to {@link ReviewResponse} DTOs.
 *
 * <p>{@code ReviewImage}s are not a mapped relation on the {@code Review} entity (they only hold
 * a many-to-one back-reference), so the list of image URLs is not derivable from the entity graph
 * alone. Callers must resolve the image URLs separately (e.g. via {@code ReviewImageRepository})
 * and pass them into {@link #toResponse(Review, List)}.
 */
@Mapper(componentModel = "spring")
public interface ReviewMapper {

    @Mapping(target = "customerId", source = "review.customer.id")
    @Mapping(target = "customerName", expression = "java(review.getCustomer().getFullName())")
    @Mapping(target = "customerAvatarUrl", source = "review.customer.profileImageUrl")
    @Mapping(target = "salonId", source = "review.salon.id")
    @Mapping(target = "employeeId", source = "review.employee.id")
    @Mapping(target = "employeeName", expression = "java(review.getEmployee() != null ? review.getEmployee().getFullName() : null)")
    @Mapping(target = "images", source = "images")
    ReviewResponse toResponse(Review review, List<String> images);
}
