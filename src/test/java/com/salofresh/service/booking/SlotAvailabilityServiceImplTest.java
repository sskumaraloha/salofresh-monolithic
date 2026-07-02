package com.salofresh.service.booking;

import com.salofresh.common.enums.BookingStatus;
import com.salofresh.common.enums.EmploymentStatus;
import com.salofresh.common.enums.SalonGenderType;
import com.salofresh.common.enums.SalonStatus;
import com.salofresh.entity.Appointment;
import com.salofresh.entity.Employee;
import com.salofresh.entity.EmployeeSchedule;
import com.salofresh.entity.Salon;
import com.salofresh.entity.WorkingHours;
import com.salofresh.exception.SlotUnavailableException;
import com.salofresh.repository.AppointmentRepository;
import com.salofresh.repository.EmployeeLeaveRepository;
import com.salofresh.repository.EmployeeRepository;
import com.salofresh.repository.EmployeeScheduleRepository;
import com.salofresh.repository.HolidayRepository;
import com.salofresh.repository.SalonRepository;
import com.salofresh.repository.SalonServiceRepository;
import com.salofresh.repository.WorkingHoursRepository;
import com.salofresh.service.impl.booking.SlotAvailabilityServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SlotAvailabilityServiceImplTest {

    @Mock
    private SalonRepository salonRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private SalonServiceRepository salonServiceRepository;
    @Mock
    private WorkingHoursRepository workingHoursRepository;
    @Mock
    private HolidayRepository holidayRepository;
    @Mock
    private EmployeeScheduleRepository employeeScheduleRepository;
    @Mock
    private EmployeeLeaveRepository employeeLeaveRepository;
    @Mock
    private AppointmentRepository appointmentRepository;

    private SlotAvailabilityServiceImpl slotAvailabilityService;

    private Salon salon;
    private Employee employee;
    private final LocalDate bookingDate = LocalDate.of(2026, 8, 3); // a Monday

    @BeforeEach
    void setUp() {
        slotAvailabilityService = new SlotAvailabilityServiceImpl(salonRepository, employeeRepository,
                salonServiceRepository, workingHoursRepository, holidayRepository, employeeScheduleRepository,
                employeeLeaveRepository, appointmentRepository);

        salon = Salon.builder()
                .id(10L)
                .status(SalonStatus.ACTIVE)
                .genderType(SalonGenderType.UNISEX)
                .maxBookingsPerSlot(1)
                .build();

        employee = Employee.builder()
                .id(20L)
                .salon(salon)
                .employmentStatus(EmploymentStatus.ACTIVE)
                .build();

        lenient().when(holidayRepository.findAllBySalonId(salon.getId())).thenReturn(Collections.emptyList());
        lenient().when(workingHoursRepository.findBySalonIdAndDayOfWeek(salon.getId(), bookingDate.getDayOfWeek()))
                .thenReturn(Optional.of(WorkingHours.builder()
                        .salon(salon)
                        .dayOfWeek(bookingDate.getDayOfWeek())
                        .open(true)
                        .startTime(LocalTime.of(9, 0))
                        .endTime(LocalTime.of(20, 0))
                        .build()));
        lenient().when(employeeScheduleRepository.findByEmployeeIdAndDayOfWeek(employee.getId(), bookingDate.getDayOfWeek()))
                .thenReturn(Optional.of(EmployeeSchedule.builder()
                        .employee(employee)
                        .dayOfWeek(bookingDate.getDayOfWeek())
                        .workingDay(true)
                        .startTime(LocalTime.of(9, 0))
                        .endTime(LocalTime.of(20, 0))
                        .build()));
        lenient().when(employeeLeaveRepository.findAllByEmployeeIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        anyLong(), any(), any(), any()))
                .thenReturn(Collections.emptyList());
    }

    @Test
    void ensureSlotAvailable_withNoConflicts_doesNotThrow() {
        when(appointmentRepository.findAllByEmployeeIdAndAppointmentDateAndStatusInAndStartTimeLessThanAndEndTimeGreaterThan(
                        any(), any(), anyList(), any(), any()))
                .thenReturn(Collections.emptyList());

        assertThatCode(() -> slotAvailabilityService.ensureSlotAvailable(
                salon, employee, bookingDate, LocalTime.of(10, 0), LocalTime.of(10, 30), null))
                .doesNotThrowAnyException();
    }

    @Test
    void ensureSlotAvailable_whenEmployeeAlreadyBookedInOverlappingWindow_throwsSlotUnavailable() {
        Appointment conflicting = Appointment.builder()
                .id(99L)
                .status(BookingStatus.CONFIRMED)
                .appointmentDate(bookingDate)
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(10, 30))
                .build();

        when(appointmentRepository.findAllByEmployeeIdAndAppointmentDateAndStatusInAndStartTimeLessThanAndEndTimeGreaterThan(
                        any(), any(), anyList(), any(), any()))
                .thenReturn(List.of(conflicting));

        assertThatThrownBy(() -> slotAvailabilityService.ensureSlotAvailable(
                salon, employee, bookingDate, LocalTime.of(10, 15), LocalTime.of(10, 45), null))
                .isInstanceOf(SlotUnavailableException.class)
                .hasMessageContaining("overlaps");
    }

    @Test
    void ensureSlotAvailable_whenExcludingTheConflictingAppointmentItself_doesNotThrow() {
        Appointment conflicting = Appointment.builder()
                .id(99L)
                .status(BookingStatus.CONFIRMED)
                .appointmentDate(bookingDate)
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(10, 30))
                .build();

        when(appointmentRepository.findAllByEmployeeIdAndAppointmentDateAndStatusInAndStartTimeLessThanAndEndTimeGreaterThan(
                        any(), any(), anyList(), any(), any()))
                .thenReturn(List.of(conflicting));

        assertThatCode(() -> slotAvailabilityService.ensureSlotAvailable(
                salon, employee, bookingDate, LocalTime.of(10, 0), LocalTime.of(10, 30), 99L))
                .doesNotThrowAnyException();
    }

    @Test
    void ensureSlotAvailable_whenSalonNotActive_throwsSlotUnavailable() {
        salon.setStatus(SalonStatus.PAUSED);

        assertThatThrownBy(() -> slotAvailabilityService.ensureSlotAvailable(
                salon, employee, bookingDate, LocalTime.of(10, 0), LocalTime.of(10, 30), null))
                .isInstanceOf(SlotUnavailableException.class)
                .hasMessageContaining("not currently accepting bookings");
    }

    @Test
    void ensureSlotAvailable_whenOutsideWorkingHours_throwsSlotUnavailable() {
        assertThatThrownBy(() -> slotAvailabilityService.ensureSlotAvailable(
                salon, employee, bookingDate, LocalTime.of(21, 0), LocalTime.of(21, 30), null))
                .isInstanceOf(SlotUnavailableException.class);
    }
}
