package com.salofresh.service.impl.salon;

import com.salofresh.common.enums.BookingStatus;
import com.salofresh.common.enums.EmploymentStatus;
import com.salofresh.dto.salon.OwnerDashboardResponse;
import com.salofresh.entity.Appointment;
import com.salofresh.entity.Salon;
import com.salofresh.entity.SalonOwner;
import com.salofresh.exception.ResourceNotFoundException;
import com.salofresh.repository.AppointmentRepository;
import com.salofresh.repository.EmployeeRepository;
import com.salofresh.repository.SalonOwnerRepository;
import com.salofresh.repository.SalonRepository;
import com.salofresh.security.SecurityUtils;
import com.salofresh.service.salon.OwnerDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;

/**
 * Aggregates counts/sums scoped to the salons owned by the currently authenticated
 * salon owner. Restricted to the repository methods already exposed by the appointment,
 * payment and employee modules, so figures are computed by fetching each owned salon's
 * appointments/employees in full and reducing them in-memory - acceptable at this scale.
 */
@Service
@RequiredArgsConstructor
public class OwnerDashboardServiceImpl implements OwnerDashboardService {

    private static final Set<BookingStatus> UPCOMING_STATUSES = Set.of(BookingStatus.PENDING, BookingStatus.CONFIRMED);

    private final SalonRepository salonRepository;
    private final SalonOwnerRepository salonOwnerRepository;
    private final AppointmentRepository appointmentRepository;
    private final EmployeeRepository employeeRepository;
    private final SecurityUtils securityUtils;

    @Override
    @Transactional(readOnly = true)
    public OwnerDashboardResponse getDashboard() {
        Long currentUserId = securityUtils.getCurrentUserId();
        SalonOwner owner = salonOwnerRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("SalonOwner", "userId", currentUserId));

        List<Salon> ownedSalons = salonRepository.findAllByOwnerIdAndDeletedFalse(owner.getId());

        long totalBookingsThisMonth = 0;
        long upcomingAppointmentsCount = 0;
        BigDecimal totalRevenueThisMonth = BigDecimal.ZERO;
        long activeEmployees = 0;

        LocalDate today = LocalDate.now();
        YearMonth currentMonth = YearMonth.from(today);

        for (Salon salon : ownedSalons) {
            List<Appointment> appointments = appointmentRepository.findAllBySalonId(salon.getId(), Pageable.unpaged()).getContent();
            for (Appointment appointment : appointments) {
                if (YearMonth.from(appointment.getAppointmentDate()).equals(currentMonth)) {
                    totalBookingsThisMonth++;
                    if (appointment.getStatus() == BookingStatus.COMPLETED && appointment.getFinalAmount() != null) {
                        totalRevenueThisMonth = totalRevenueThisMonth.add(appointment.getFinalAmount());
                    }
                }
                if (!appointment.getAppointmentDate().isBefore(today) && UPCOMING_STATUSES.contains(appointment.getStatus())) {
                    upcomingAppointmentsCount++;
                }
            }

            activeEmployees += employeeRepository.findAllBySalonIdAndDeletedFalse(salon.getId()).stream()
                    .filter(employee -> employee.getEmploymentStatus() == EmploymentStatus.ACTIVE)
                    .count();
        }

        return OwnerDashboardResponse.builder()
                .totalSalons(ownedSalons.size())
                .totalBookingsThisMonth(totalBookingsThisMonth)
                .totalRevenueThisMonth(totalRevenueThisMonth)
                .activeEmployees(activeEmployees)
                .upcomingAppointmentsCount(upcomingAppointmentsCount)
                .build();
    }
}
