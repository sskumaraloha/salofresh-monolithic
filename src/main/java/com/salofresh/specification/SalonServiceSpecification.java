package com.salofresh.specification;

import com.salofresh.common.enums.EntityStatus;
import com.salofresh.entity.SalonService;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

public final class SalonServiceSpecification {

    private SalonServiceSpecification() {
    }

    public static Specification<SalonService> notDeleted() {
        return (root, query, cb) -> cb.isFalse(root.get("deleted"));
    }

    public static Specification<SalonService> hasSalon(Long salonId) {
        return (root, query, cb) -> salonId == null ? null : cb.equal(root.get("salon").get("id"), salonId);
    }

    public static Specification<SalonService> hasCategory(Long categoryId) {
        return (root, query, cb) -> categoryId == null ? null : cb.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<SalonService> priceBetween(BigDecimal minPrice, BigDecimal maxPrice) {
        return (root, query, cb) -> {
            if (minPrice == null && maxPrice == null) {
                return null;
            }
            if (minPrice != null && maxPrice != null) {
                return cb.between(root.get("price"), minPrice, maxPrice);
            }
            if (minPrice != null) {
                return cb.greaterThanOrEqualTo(root.get("price"), minPrice);
            }
            return cb.lessThanOrEqualTo(root.get("price"), maxPrice);
        };
    }

    public static Specification<SalonService> isActive() {
        return (root, query, cb) -> cb.equal(root.get("status"), EntityStatus.ACTIVE);
    }
}
