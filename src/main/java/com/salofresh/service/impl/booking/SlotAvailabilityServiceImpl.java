package com.salofresh.service.impl.booking;

import com.salofresh.common.enums.BookingStatus;
import com.salofresh.common.enums.EmploymentStatus;
import com.salofresh.common.enums.LeaveStatus;
import com.salofresh.common.enums.SalonStatus;
import com.salofresh.dto.booking.AvailableSlotResponse;
import com.salofresh.entity.Appointment;
import com.salofresh.entity.Employee;
import com.salofresh.entity.EmployeeLeave;
import com.salofresh.entity.EmployeeSchedule;
import com.salofresh.entity.Holiday;
import com.salofresh.entity.Salon;
import com.salofresh.entity.SalonService;
import com.salofresh.entity.WorkingHours;
import com.salofresh.exception.BadRequestException;
import com.salofresh.exception.ResourceNotFoundException;
import com.salofresh.exception.SlotUnavailableException;
import com.salofresh.repository.AppointmentRepository;
import com.salofresh.repository.EmployeeLeaveRepository;
import com.salofresh.repository.EmployeeRepository;
import com.salofresh.repository.EmployeeScheduleRepository;
import com.salofresh.repository.HolidayRepository;
import com.salofresh.repository.SalonRepository;
import com.salofresh.repository.SalonServiceRepository;
import com.salofresh.repository.WorkingHoursRepository;
import com.salofresh.service.booking.SlotAvailabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SlotAvailabilityServiceImpl implements SlotAvailabilityService {

    private static final List<BookingStatus> ACTIVE_STATUSES = List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED);

    private final SalonRepository salonRepository;
    private final EmployeeRepository employeeRepository;
    private final SalonServiceRepository salonServiceRepository;
    private final WorkingHoursRepository workingHoursRepository;
    private final HolidayRepository holidayRepository;
    private final EmployeeScheduleRepository employeeScheduleRepository;
    private final EmployeeLeaveRepository employeeLeaveRepository;
    private final AppointmentRepository appointmentRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AvailableSlotResponse> getAvailableSlots(Long salonId, LocalDate date, Long employeeId, List<Long> serviceIds) {
        Salon salon = salonRepository.findById(salonId)
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Salon", "id", salonId));

        if (salon.getStatus() != SalonStatus.ACTIVE || isHoliday(salonId, date)) {
            return List.of();
        }

        WorkingHours workingHours = workingHoursRepository.findBySalonIdAndDayOfWeek(salonId, date.getDayOfWeek()).orElse(null);
        if (workingHours == null || !workingHours.isOpen()
                || workingHours.getStartTime() == null || workingHours.getEndTime() == null) {
            return List.of();
        }

        LocalTime windowStart = workingHours.getStartTime();
        LocalTime windowEnd = workingHours.getEndTime();

        Employee employee = null;
        if (employeeId != null) {
            employee = employeeRepository.findByIdAndSalonIdAndDeletedFalse(employeeId, salonId)
                    .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId));
            if (employee.getEmploymentStatus() != EmploymentStatus.ACTIVE) {
                return List.of();
            }
            EmployeeSchedule schedule = employeeScheduleRepository
                    .findByEmployeeIdAndDayOfWeek(employeeId, date.getDayOfWeek()).orElse(null);
            if (schedule == null || !schedule.isWorkingDay()
                    || schedule.getStartTime() == null || schedule.getEndTime() == null) {
                return List.of();
            }
            if (isOnApprovedLeave(employeeId, date)) {
                return List.of();
            }
            windowStart = laterOf(windowStart, schedule.getStartTime());
            windowEnd = earlierOf(windowEnd, schedule.getEndTime());
        }

        if (!windowStart.isBefore(windowEnd)) {
            return List.of();
        }

        int totalDurationMinutes = resolveDurationMinutes(salon, serviceIds);
        int stepMinutes = salon.getSlotDurationMinutes() + salon.getBufferTimeMinutes();
        if (stepMinutes <= 0) {
            stepMinutes = Math.max(totalDurationMinutes, 1);
        }

        List<Appointment> sameDayAppointments = (employeeId != null
                ? appointmentRepository.findAllByEmployeeIdAndAppointmentDate(employeeId, date)
                : appointmentRepository.findAllBySalonIdAndAppointmentDate(salonId, date))
                .stream()
                .filter(a -> ACTIVE_STATUSES.contains(a.getStatus()))
                .toList();

        boolean isToday = date.equals(LocalDate.now());
        LocalTime now = LocalTime.now();
        int capacity = employeeId != null ? 1 : Math.max(salon.getMaxBookingsPerSlot(), 1);

        List<AvailableSlotResponse> slots = new ArrayList<>();
        LocalTime candidateStart = windowStart;
        while (!candidateStart.plusMinutes(totalDurationMinutes).isAfter(windowEnd)) {
            final LocalTime slotStart = candidateStart;
            final LocalTime slotEnd = candidateStart.plusMinutes(totalDurationMinutes);

            boolean overlapsBreak = overlapsBreak(workingHours, slotStart, slotEnd);
            boolean isPast = isToday && !slotStart.isAfter(now);

            if (!overlapsBreak && !isPast) {
                long bookedCount = sameDayAppointments.stream()
                        .filter(a -> a.getStartTime().isBefore(slotEnd) && a.getEndTime().isAfter(slotStart))
                        .count();
                int availableCount = (int) (capacity - bookedCount);
                if (availableCount > 0) {
                    slots.add(AvailableSlotResponse.builder()
                            .startTime(slotStart)
                            .endTime(slotEnd)
                            .availableCount(availableCount)
                            .build());
                }
            }
            candidateStart = candidateStart.plusMinutes(stepMinutes);
        }
        return slots;
    }

    @Override
    @Transactional(readOnly = true)
    public void ensureSlotAvailable(Salon salon, Employee employee, LocalDate date, LocalTime startTime, LocalTime endTime,
                                     Long excludeAppointmentId) {
        if (salon.getStatus() != SalonStatus.ACTIVE) {
            throw new SlotUnavailableException("This salon is not currently accepting bookings");
        }
        if (isHoliday(salon.getId(), date)) {
            throw new SlotUnavailableException("The salon is closed on the selected date");
        }

        WorkingHours workingHours = workingHoursRepository.findBySalonIdAndDayOfWeek(salon.getId(), date.getDayOfWeek()).orElse(null);
        if (workingHours == null || !workingHours.isOpen()
                || workingHours.getStartTime() == null || workingHours.getEndTime() == null) {
            throw new SlotUnavailableException("The salon is closed on the selected day");
        }
        if (startTime.isBefore(workingHours.getStartTime()) || endTime.isAfter(workingHours.getEndTime())) {
            throw new SlotUnavailableException("The selected time is outside the salon's working hours");
        }
        if (overlapsBreak(workingHours, startTime, endTime)) {
            throw new SlotUnavailableException("The selected time overlaps the salon's break hours");
        }

        if (employee != null) {
            if (employee.getSalon() == null || !employee.getSalon().getId().equals(salon.getId())) {
                throw new BadRequestException("Selected employee does not belong to this salon");
            }
            if (employee.getEmploymentStatus() != EmploymentStatus.ACTIVE) {
                throw new SlotUnavailableException("Selected employee is not currently active");
            }
            EmployeeSchedule schedule = employeeScheduleRepository
                    .findByEmployeeIdAndDayOfWeek(employee.getId(), date.getDayOfWeek()).orElse(null);
            if (schedule == null || !schedule.isWorkingDay()
                    || schedule.getStartTime() == null || schedule.getEndTime() == null) {
                throw new SlotUnavailableException("Selected employee does not work on the selected day");
            }
            if (startTime.isBefore(schedule.getStartTime()) || endTime.isAfter(schedule.getEndTime())) {
                throw new SlotUnavailableException("The selected time is outside the employee's working hours");
            }
            if (isOnApprovedLeave(employee.getId(), date)) {
                throw new SlotUnavailableException("Selected employee is on leave on the selected date");
            }

            List<Appointment> conflicts = appointmentRepository
                    .findAllByEmployeeIdAndAppointmentDateAndStatusInAndStartTimeLessThanAndEndTimeGreaterThan(
                            employee.getId(), date, ACTIVE_STATUSES, endTime, startTime);
            boolean hasConflict = conflicts.stream()
                    .anyMatch(a -> excludeAppointmentId == null || !a.getId().equals(excludeAppointmentId));
            if (hasConflict) {
                throw new SlotUnavailableException("Selected employee already has a booking that overlaps this time");
            }
        } else {
            List<Appointment> sameDay = appointmentRepository.findAllBySalonIdAndAppointmentDate(salon.getId(), date);
            long overlapping = sameDay.stream()
                    .filter(a -> excludeAppointmentId == null || !a.getId().equals(excludeAppointmentId))
                    .filter(a -> ACTIVE_STATUSES.contains(a.getStatus()))
                    .filter(a -> a.getStartTime().isBefore(endTime) && a.getEndTime().isAfter(startTime))
                    .count();
            if (overlapping >= Math.max(salon.getMaxBookingsPerSlot(), 1)) {
                throw new SlotUnavailableException("This time slot is fully booked, please choose another slot");
            }
        }
    }

    private int resolveDurationMinutes(Salon salon, List<Long> serviceIds) {
        if (serviceIds == null || serviceIds.isEmpty()) {
            return Math.max(salon.getSlotDurationMinutes(), 1);
        }
        int total = 0;
        for (Long serviceId : serviceIds) {
            SalonService service = salonServiceRepository.findByIdAndSalonIdAndDeletedFalse(serviceId, salon.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Service", "id", serviceId));
            total += service.getDurationMinutes();
        }
        return Math.max(total, 1);
    }

    private boolean isHoliday(Long salonId, LocalDate date) {
        List<Holiday> holidays = holidayRepository.findAllBySalonId(salonId);
        for (Holiday holiday : holidays) {
            LocalDate holidayDate = holiday.getHolidayDate();
            if (holidayDate.equals(date)) {
                return true;
            }
            if (holiday.isRecurringYearly()
                    && holidayDate.getMonthValue() == date.getMonthValue()
                    && holidayDate.getDayOfMonth() == date.getDayOfMonth()) {
                return true;
            }
        }
        return false;
    }

    private boolean isOnApprovedLeave(Long employeeId, LocalDate date) {
        List<EmployeeLeave> leaves = employeeLeaveRepository
                .findAllByEmployeeIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        employeeId, LeaveStatus.APPROVED, date, date);
        return !leaves.isEmpty();
    }

    private boolean overlapsBreak(WorkingHours workingHours, LocalTime start, LocalTime end) {
        return workingHours.getBreakStartTime() != null && workingHours.getBreakEndTime() != null
                && start.isBefore(workingHours.getBreakEndTime()) && end.isAfter(workingHours.getBreakStartTime());
    }

    private LocalTime laterOf(LocalTime a, LocalTime b) {
        return a.isAfter(b) ? a : b;
    }

    private LocalTime earlierOf(LocalTime a, LocalTime b) {
        return a.isBefore(b) ? a : b;
    }
}
