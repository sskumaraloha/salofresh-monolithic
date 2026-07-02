package com.salofresh.specification;

import com.salofresh.common.enums.EntityStatus;
import com.salofresh.common.enums.SalonGenderType;
import com.salofresh.common.enums.SalonStatus;
import com.salofresh.common.enums.VerificationStatus;
import com.salofresh.entity.Salon;
import com.salofresh.entity.SalonService;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * Reusable {@link Specification} builders for filtering the public salon catalogue.
 * Every method returns {@code null} when the filter criterion is absent so callers can
 * freely chain them via {@code Specification.where(...).and(...)} - Spring Data JPA treats
 * a {@code null} specification as a no-op when composing.
 */
public final class SalonSpecification {

    private SalonSpecification() {
    }

    /**
     * A salon is only publicly visible/bookable once an admin has approved it, the owner
     * has it toggled to ACTIVE, and it has not been soft-deleted.
     */
    public static Specification<Salon> isApprovedAndActive() {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("verificationStatus"), VerificationStatus.APPROVED),
                cb.equal(root.get("status"), SalonStatus.ACTIVE),
                cb.isFalse(root.get("deleted")));
    }

    public static Specification<Salon> hasCity(String cityName) {
        if (cityName == null || cityName.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.equal(cb.lower(root.get("city").get("name")), cityName.toLowerCase(Locale.ROOT));
    }

    public static Specification<Salon> hasMinRating(Double minRating) {
        if (minRating == null) {
            return null;
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("ratingAverage"), minRating);
    }

    public static Specification<Salon> hasGenderType(SalonGenderType genderType) {
        if (genderType == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("genderType"), genderType);
    }

    /**
     * Matches salons that offer at least one active service priced at or below {@code maxPrice}
     * (using the discounted price when present).
     */
    public static Specification<Salon> hasMaxPrice(BigDecimal maxPrice) {
        if (maxPrice == null) {
            return null;
        }
        return (root, query, cb) -> {
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<SalonService> serviceRoot = subquery.from(SalonService.class);
            subquery.select(serviceRoot.get("id"))
                    .where(cb.and(
                            cb.equal(serviceRoot.get("salon"), root),
                            cb.isFalse(serviceRoot.get("deleted")),
                            cb.equal(serviceRoot.get("status"), EntityStatus.ACTIVE),
                            cb.lessThanOrEqualTo(
                                    cb.coalesce(serviceRoot.get("discountPrice"), serviceRoot.get("price")),
                                    maxPrice)));
            return cb.exists(subquery);
        };
    }
}
