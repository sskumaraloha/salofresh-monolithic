package com.salofresh.repository;

import com.salofresh.common.enums.EmploymentStatus;
import com.salofresh.common.enums.PaymentStatus;
import com.salofresh.common.enums.RoleName;
import com.salofresh.common.enums.VerificationStatus;
import com.salofresh.dto.admin.ReportGroupBy;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Read-only aggregate query repository for the admin analytics/dashboard module.
 * Deliberately kept separate from the domain repositories (UserRepository, SalonRepository, etc.)
 * so this module never touches files owned by other parallel workstreams.
 * Uses a plain injected {@link EntityManager} for JPQL counts/sums and native SQL for
 * time-bucketed (DAY/WEEK/MONTH) aggregations.
 */
@Repository
public class AnalyticsQueryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public long countTotalUsers() {
        return entityManager.createQuery(
                        "SELECT COUNT(u) FROM User u WHERE u.deleted = false", Long.class)
                .getSingleResult();
    }

    public long countUsersByRole(RoleName roleName) {
        return entityManager.createQuery(
                        "SELECT COUNT(DISTINCT u) FROM User u JOIN u.roles r " +
                                "WHERE r.name = :roleName AND u.deleted = false", Long.class)
                .setParameter("roleName", roleName)
                .getSingleResult();
    }

    public long countTotalCustomers() {
        return entityManager.createQuery(
                        "SELECT COUNT(c) FROM Customer c WHERE c.deleted = false", Long.class)
                .getSingleResult();
    }

    public long countTotalSalonOwners() {
        return entityManager.createQuery(
                        "SELECT COUNT(o) FROM SalonOwner o WHERE o.deleted = false", Long.class)
                .getSingleResult();
    }

    public long countTotalSalons() {
        return entityManager.createQuery(
                        "SELECT COUNT(s) FROM Salon s WHERE s.deleted = false", Long.class)
                .getSingleResult();
    }

    public long countSalonsByVerificationStatus(VerificationStatus status) {
        return entityManager.createQuery(
                        "SELECT COUNT(s) FROM Salon s WHERE s.verificationStatus = :status AND s.deleted = false",
                        Long.class)
                .setParameter("status", status)
                .getSingleResult();
    }

    public long countActiveEmployeesAcrossPlatform() {
        return entityManager.createQuery(
                        "SELECT COUNT(e) FROM Employee e WHERE e.employmentStatus = :status AND e.deleted = false",
                        Long.class)
                .setParameter("status", EmploymentStatus.ACTIVE)
                .getSingleResult();
    }

    public long countBookingsBetween(LocalDate from, LocalDate to) {
        return entityManager.createQuery(
                        "SELECT COUNT(a) FROM Appointment a " +
                                "WHERE a.appointmentDate BETWEEN :from AND :to AND a.deleted = false", Long.class)
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();
    }

    public BigDecimal totalRevenueBetween(LocalDate from, LocalDate to) {
        BigDecimal result = entityManager.createQuery(
                        "SELECT COALESCE(SUM(p.amount - p.refundedAmount), 0) FROM Payment p JOIN p.appointment a " +
                                "WHERE p.paymentStatus = :status AND a.appointmentDate BETWEEN :from AND :to " +
                                "AND p.deleted = false", BigDecimal.class)
                .setParameter("status", PaymentStatus.SUCCESS)
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();
        return result == null ? BigDecimal.ZERO : result;
    }

    /**
     * Revenue and booking-count grouped by period (DAY/WEEK/MONTH bucket of the appointment date).
     * Each row: [period(String), revenue(BigDecimal), bookingCount(Long)]
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> revenueByPeriod(LocalDate from, LocalDate to, ReportGroupBy groupBy) {
        String sql = "SELECT DATE_FORMAT(a.appointment_date, :fmt) AS period, " +
                "COALESCE(SUM(p.amount - p.refunded_amount), 0) AS revenue, " +
                "COUNT(DISTINCT a.id) AS booking_count " +
                "FROM appointments a " +
                "JOIN payments p ON p.appointment_id = a.id " +
                "WHERE p.payment_status = 'SUCCESS' AND p.is_deleted = 0 AND a.is_deleted = 0 " +
                "AND a.appointment_date BETWEEN :fromDate AND :toDate " +
                "GROUP BY period ORDER BY MIN(a.appointment_date)";
        Query query = entityManager.createNativeQuery(sql)
                .setParameter("fmt", mysqlDateFormat(groupBy))
                .setParameter("fromDate", from)
                .setParameter("toDate", to);
        return query.getResultList();
    }

    /**
     * Booking counts grouped by period with status breakdown.
     * Each row: [period(String), total(Long), completed(Long), cancelled(Long), noShow(Long)]
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> bookingCountsByPeriod(LocalDate from, LocalDate to, ReportGroupBy groupBy) {
        String sql = "SELECT DATE_FORMAT(a.appointment_date, :fmt) AS period, " +
                "COUNT(*) AS total, " +
                "SUM(CASE WHEN a.status = 'COMPLETED' THEN 1 ELSE 0 END) AS completed, " +
                "SUM(CASE WHEN a.status = 'CANCELLED' THEN 1 ELSE 0 END) AS cancelled, " +
                "SUM(CASE WHEN a.status = 'NO_SHOW' THEN 1 ELSE 0 END) AS no_show " +
                "FROM appointments a " +
                "WHERE a.is_deleted = 0 AND a.appointment_date BETWEEN :fromDate AND :toDate " +
                "GROUP BY period ORDER BY MIN(a.appointment_date)";
        Query query = entityManager.createNativeQuery(sql)
                .setParameter("fmt", mysqlDateFormat(groupBy))
                .setParameter("fromDate", from)
                .setParameter("toDate", to);
        return query.getResultList();
    }

    /**
     * Top salons within the given date range ranked by revenue.
     * Each row: [salonId(Number), salonName(String), bookings(Long), revenue(BigDecimal), ratingAverage(Double)]
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> topSalonsByRevenue(LocalDate from, LocalDate to, int limit) {
        String sql = "SELECT s.id AS salon_id, s.name AS salon_name, " +
                "COUNT(DISTINCT a.id) AS bookings, " +
                "COALESCE(SUM(CASE WHEN p.payment_status = 'SUCCESS' THEN p.amount - p.refunded_amount ELSE 0 END), 0) AS revenue, " +
                "s.rating_average AS rating_average " +
                "FROM salons s " +
                "LEFT JOIN appointments a ON a.salon_id = s.id AND a.is_deleted = 0 " +
                "   AND a.appointment_date BETWEEN :fromDate AND :toDate " +
                "LEFT JOIN payments p ON p.appointment_id = a.id AND p.is_deleted = 0 " +
                "WHERE s.is_deleted = 0 " +
                "GROUP BY s.id, s.name, s.rating_average " +
                "ORDER BY revenue DESC " +
                "LIMIT :limit";
        Query query = entityManager.createNativeQuery(sql)
                .setParameter("fromDate", from)
                .setParameter("toDate", to)
                .setParameter("limit", limit);
        return query.getResultList();
    }

    /**
     * New customer signups grouped by period (based on customer profile creation date).
     * Each row: [period(String), newCustomers(Long)]
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> newCustomersByPeriod(LocalDate from, LocalDate to, ReportGroupBy groupBy) {
        String sql = "SELECT DATE_FORMAT(c.created_at, :fmt) AS period, COUNT(*) AS new_customers " +
                "FROM customers c " +
                "WHERE c.is_deleted = 0 AND c.created_at BETWEEN :fromDate AND :toDatePlusOne " +
                "GROUP BY period ORDER BY MIN(c.created_at)";
        Query query = entityManager.createNativeQuery(sql)
                .setParameter("fmt", mysqlDateFormat(groupBy))
                .setParameter("fromDate", from)
                .setParameter("toDatePlusOne", to.plusDays(1));
        return query.getResultList();
    }

    /**
     * Distinct customers who made a booking within each period.
     * Each row: [period(String), activeCustomers(Long)]
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> activeCustomersByPeriod(LocalDate from, LocalDate to, ReportGroupBy groupBy) {
        String sql = "SELECT DATE_FORMAT(a.appointment_date, :fmt) AS period, " +
                "COUNT(DISTINCT a.customer_id) AS active_customers " +
                "FROM appointments a " +
                "WHERE a.is_deleted = 0 AND a.appointment_date BETWEEN :fromDate AND :toDate " +
                "GROUP BY period ORDER BY MIN(a.appointment_date)";
        Query query = entityManager.createNativeQuery(sql)
                .setParameter("fmt", mysqlDateFormat(groupBy))
                .setParameter("fromDate", from)
                .setParameter("toDate", to);
        return query.getResultList();
    }

    private String mysqlDateFormat(ReportGroupBy groupBy) {
        return switch (groupBy) {
            case DAY -> "%Y-%m-%d";
            case WEEK -> "%x-W%v";
            case MONTH -> "%Y-%m";
        };
    }
}
