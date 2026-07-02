package com.salofresh.specification;

import com.salofresh.common.enums.BookingStatus;
import com.salofresh.entity.Appointment;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public final class AppointmentSpecification {

    private AppointmentSpecification() {
    }

    public static Specification<Appointment> hasCustomer(Long customerId) {
        return (root, query, cb) -> customerId == null
                ? null
                : cb.equal(root.get("customer").get("id"), customerId);
    }

    public static Specification<Appointment> hasSalon(Long salonId) {
        return (root, query, cb) -> salonId == null
                ? null
                : cb.equal(root.get("salon").get("id"), salonId);
    }

    public static Specification<Appointment> hasStatus(BookingStatus status) {
        return (root, query, cb) -> status == null
                ? null
                : cb.equal(root.get("status"), status);
    }

    public static Specification<Appointment> dateBetween(LocalDate from, LocalDate to) {
        return (root, query, cb) -> {
            if (from == null && to == null) {
                return null;
            }
            if (from != null && to != null) {
                return cb.between(root.get("appointmentDate"), from, to);
            }
            if (from != null) {
                return cb.greaterThanOrEqualTo(root.get("appointmentDate"), from);
            }
            return cb.lessThanOrEqualTo(root.get("appointmentDate"), to);
        };
    }
}
