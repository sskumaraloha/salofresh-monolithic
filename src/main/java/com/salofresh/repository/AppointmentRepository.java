package com.salofresh.repository;

import com.salofresh.common.enums.BookingStatus;
import com.salofresh.entity.Appointment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long>, JpaSpecificationExecutor<Appointment> {

    Optional<Appointment> findByBookingNumber(String bookingNumber);

    Page<Appointment> findAllByCustomerId(Long customerId, Pageable pageable);

    Page<Appointment> findAllBySalonId(Long salonId, Pageable pageable);

    List<Appointment> findAllByEmployeeIdAndAppointmentDate(Long employeeId, LocalDate appointmentDate);

    List<Appointment> findAllBySalonIdAndAppointmentDate(Long salonId, LocalDate appointmentDate);

    List<Appointment> findAllByEmployeeIdAndAppointmentDateAndStatusInAndStartTimeLessThanAndEndTimeGreaterThan(
            Long employeeId, LocalDate appointmentDate, List<BookingStatus> statuses, LocalTime endTime, LocalTime startTime);

    long countBySalonIdAndAppointmentDateAndStatusIn(Long salonId, LocalDate appointmentDate, List<BookingStatus> statuses);
}
