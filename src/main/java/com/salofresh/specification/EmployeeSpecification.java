package com.salofresh.specification;

import com.salofresh.common.enums.EmploymentStatus;
import com.salofresh.entity.Employee;
import com.salofresh.entity.SalonService;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

public final class EmployeeSpecification {

    private EmployeeSpecification() {
    }

    public static Specification<Employee> hasSalon(Long salonId) {
        return (root, query, cb) -> salonId == null ? cb.conjunction() : cb.equal(root.get("salon").get("id"), salonId);
    }

    public static Specification<Employee> hasEmploymentStatus(EmploymentStatus status) {
        return (root, query, cb) -> status == null ? cb.conjunction() : cb.equal(root.get("employmentStatus"), status);
    }

    public static Specification<Employee> offersService(Long serviceId) {
        return (root, query, cb) -> {
            if (serviceId == null) {
                return cb.conjunction();
            }
            if (query != null) {
                query.distinct(true);
            }
            Join<Employee, SalonService> join = root.join("services", JoinType.INNER);
            return cb.equal(join.get("id"), serviceId);
        };
    }

    public static Specification<Employee> isNotDeleted() {
        return (root, query, cb) -> cb.isFalse(root.get("deleted"));
    }
}
