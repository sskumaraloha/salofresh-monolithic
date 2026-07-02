package com.salofresh.specification;

import com.salofresh.common.enums.ReviewStatus;
import com.salofresh.entity.Review;
import org.springframework.data.jpa.domain.Specification;

/**
 * Reusable {@link Specification} builders for querying {@link Review} entities.
 */
public final class ReviewSpecification {

    private ReviewSpecification() {
    }

    public static Specification<Review> hasSalon(Long salonId) {
        return (root, query, cb) -> salonId == null ? null : cb.equal(root.get("salon").get("id"), salonId);
    }

    public static Specification<Review> hasEmployee(Long employeeId) {
        return (root, query, cb) -> employeeId == null ? null : cb.equal(root.get("employee").get("id"), employeeId);
    }

    public static Specification<Review> hasCustomer(Long customerId) {
        return (root, query, cb) -> customerId == null ? null : cb.equal(root.get("customer").get("id"), customerId);
    }

    public static Specification<Review> isVisible() {
        return (root, query, cb) -> cb.equal(root.get("status"), ReviewStatus.VISIBLE);
    }

    public static Specification<Review> isNotDeleted() {
        return (root, query, cb) -> cb.isFalse(root.get("deleted"));
    }
}
