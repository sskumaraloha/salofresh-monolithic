package com.salofresh.service.impl.booking;

import com.salofresh.common.enums.BookingStatus;
import com.salofresh.common.enums.EntityStatus;
import com.salofresh.constant.AppConstants;
import com.salofresh.coupon.CouponEngine;
import com.salofresh.coupon.CouponValidationResult;
import com.salofresh.dto.booking.AppointmentResponse;
import com.salofresh.dto.booking.AppointmentSummaryResponse;
import com.salofresh.dto.booking.CancelRequest;
import com.salofresh.dto.booking.CreateBookingRequest;
import com.salofresh.dto.booking.RescheduleRequest;
import com.salofresh.entity.Appointment;
import com.salofresh.entity.AppointmentService;
import com.salofresh.entity.Coupon;
import com.salofresh.entity.CouponUsage;
import com.salofresh.entity.Employee;
import com.salofresh.entity.Payment;
import com.salofresh.entity.Salon;
import com.salofresh.entity.SalonService;
import com.salofresh.entity.User;
import com.salofresh.event.BookingCancelledEvent;
import com.salofresh.event.BookingCompletedEvent;
import com.salofresh.event.BookingCreatedEvent;
import com.salofresh.event.BookingModifiedEvent;
import com.salofresh.event.BookingRejectedEvent;
import com.salofresh.exception.BadRequestException;
import com.salofresh.exception.ForbiddenException;
import com.salofresh.exception.ResourceNotFoundException;
import com.salofresh.mapper.booking.AppointmentMapper;
import com.salofresh.mapper.booking.AppointmentServiceMapper;
import com.salofresh.repository.AppointmentRepository;
import com.salofresh.repository.AppointmentServiceRepository;
import com.salofresh.repository.CouponRepository;
import com.salofresh.repository.CouponUsageRepository;
import com.salofresh.repository.EmployeeRepository;
import com.salofresh.repository.PaymentRepository;
import com.salofresh.repository.SalonRepository;
import com.salofresh.repository.SalonServiceRepository;
import com.salofresh.repository.UserRepository;
import com.salofresh.response.PagedResponse;
import com.salofresh.security.SecurityUtils;
import com.salofresh.service.booking.BookingService;
import com.salofresh.service.booking.SlotAvailabilityService;
import com.salofresh.specification.AppointmentSpecification;
import com.salofresh.util.RandomCodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private static final List<BookingStatus> ACTIVE_STATUSES = List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED);

    private final AppointmentRepository appointmentRepository;
    private final AppointmentServiceRepository appointmentServiceRepository;
    private final SalonRepository salonRepository;
    private final EmployeeRepository employeeRepository;
    private final SalonServiceRepository salonServiceRepository;
    private final PaymentRepository paymentRepository;
    private final CouponRepository couponRepository;
    private final CouponUsageRepository couponUsageRepository;
    private final UserRepository userRepository;
    private final CouponEngine couponEngine;
    private final SlotAvailabilityService slotAvailabilityService;
    private final SecurityUtils securityUtils;
    private final AppointmentMapper appointmentMapper;
    private final AppointmentServiceMapper appointmentServiceMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public AppointmentResponse createBooking(CreateBookingRequest request) {
        Long currentUserId = securityUtils.getCurrentUserId();
        User customer = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId));

        Salon salon = salonRepository.findById(request.getSalonId())
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Salon", "id", request.getSalonId()));

        Employee employee = null;
        if (request.getEmployeeId() != null) {
            employee = employeeRepository.findByIdAndSalonIdAndDeletedFalse(request.getEmployeeId(), salon.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", request.getEmployeeId()));
        }

        if (request.getServiceIds() == null || request.getServiceIds().isEmpty()) {
            throw new BadRequestException("At least one service must be selected");
        }

        List<SalonService> services = new ArrayList<>();
        for (Long serviceId : request.getServiceIds()) {
            SalonService service = salonServiceRepository.findByIdAndSalonIdAndDeletedFalse(serviceId, salon.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Service", "id", serviceId));
            if (service.getStatus() != EntityStatus.ACTIVE) {
                throw new BadRequestException("Service '%s' is not currently available".formatted(service.getName()));
            }
            services.add(service);
        }

        if (employee != null && !employee.getServices().isEmpty()) {
            for (SalonService service : services) {
                boolean offered = employee.getServices().stream().anyMatch(s -> s.getId().equals(service.getId()));
                if (!offered) {
                    throw new BadRequestException(
                            "Selected employee does not offer service '%s'".formatted(service.getName()));
                }
            }
        }

        if (request.getAppointmentDate().equals(LocalDate.now()) && request.getStartTime().isBefore(LocalTime.now())) {
            throw new BadRequestException("Cannot book a slot in the past");
        }

        int totalDurationMinutes = services.stream().mapToInt(SalonService::getDurationMinutes).sum();
        LocalTime endTime = request.getStartTime().plusMinutes(totalDurationMinutes);

        slotAvailabilityService.ensureSlotAvailable(salon, employee, request.getAppointmentDate(),
                request.getStartTime(), endTime, null);

        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal taxAmount = BigDecimal.ZERO;
        List<AppointmentService> lineItems = new ArrayList<>();
        for (SalonService service : services) {
            BigDecimal price = service.getEffectivePrice();
            BigDecimal lineTax = price.multiply(service.getTaxPercentage())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            totalAmount = totalAmount.add(price);
            taxAmount = taxAmount.add(lineTax);
            lineItems.add(AppointmentService.builder()
                    .service(service)
                    .serviceName(service.getName())
                    .price(price)
                    .durationMinutes(service.getDurationMinutes())
                    .quantity(1)
                    .build());
        }

        BigDecimal discountAmount = BigDecimal.ZERO;
        Coupon appliedCoupon = null;
        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
            CouponValidationResult result = couponEngine.validateAndCalculate(
                    request.getCouponCode(), customer, salon.getId(), totalAmount);
            appliedCoupon = result.coupon();
            discountAmount = result.discountAmount();
        }

        BigDecimal finalAmount = totalAmount.add(taxAmount).subtract(discountAmount);
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            finalAmount = BigDecimal.ZERO;
        }

        Appointment appointment = Appointment.builder()
                .bookingNumber(generateUniqueBookingNumber())
                .customer(customer)
                .salon(salon)
                .employee(employee)
                .appointmentDate(request.getAppointmentDate())
                .startTime(request.getStartTime())
                .endTime(endTime)
                .status(BookingStatus.PENDING)
                .totalAmount(totalAmount)
                .discountAmount(discountAmount)
                .taxAmount(taxAmount)
                .finalAmount(finalAmount)
                .couponCode(appliedCoupon != null ? appliedCoupon.getCode() : null)
                .notes(request.getNotes())
                .build();
        appointment = appointmentRepository.save(appointment);

        for (AppointmentService lineItem : lineItems) {
            lineItem.setAppointment(appointment);
        }
        appointmentServiceRepository.saveAll(lineItems);

        Payment payment = Payment.builder()
                .appointment(appointment)
                .user(customer)
                .amount(finalAmount)
                .currency(AppConstants.CURRENCY_INR)
                .paymentMethod(request.getPaymentMethod())
                .build();
        paymentRepository.save(payment);

        if (appliedCoupon != null) {
            appliedCoupon.setTimesUsed(appliedCoupon.getTimesUsed() + 1);
            couponRepository.save(appliedCoupon);

            CouponUsage usage = CouponUsage.builder()
                    .coupon(appliedCoupon)
                    .user(customer)
                    .appointment(appointment)
                    .usedAt(Instant.now())
                    .discountAmount(discountAmount)
                    .build();
            couponUsageRepository.save(usage);
        }

        eventPublisher.publishEvent(new BookingCreatedEvent(appointment));

        return toFullResponse(appointment, lineItems);
    }

    @Override
    @Transactional(readOnly = true)
    public AppointmentResponse getById(String idOrBookingNumber) {
        Appointment appointment = resolveAppointment(idOrBookingNumber);
        ensureCanView(appointment);
        List<AppointmentService> lineItems = appointmentServiceRepository.findAllByAppointmentId(appointment.getId());
        return toFullResponse(appointment, lineItems);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<AppointmentSummaryResponse> listForCustomer(BookingStatus status, LocalDate from, LocalDate to,
                                                                       Pageable pageable) {
        Long customerId = securityUtils.getCurrentUserId();
        Specification<Appointment> spec = Specification.where(AppointmentSpecification.hasCustomer(customerId))
                .and(AppointmentSpecification.hasStatus(status))
                .and(AppointmentSpecification.dateBetween(from, to));
        Page<Appointment> page = appointmentRepository.findAll(spec, pageable);
        return toSummaryPagedResponse(page);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<AppointmentSummaryResponse> listForSalon(Long salonId, BookingStatus status, LocalDate from,
                                                                    LocalDate to, Pageable pageable) {
        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Salon", "id", salonId));
        ensureSalonOwnerOrAdmin(salon);

        Specification<Appointment> spec = Specification.where(AppointmentSpecification.hasSalon(salonId))
                .and(AppointmentSpecification.hasStatus(status))
                .and(AppointmentSpecification.dateBetween(from, to));
        Page<Appointment> page = appointmentRepository.findAll(spec, pageable);
        return toSummaryPagedResponse(page);
    }

    @Override
    @Transactional
    public AppointmentResponse reschedule(Long appointmentId, RescheduleRequest request) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", "id", appointmentId));

        if (!isCustomerOwner(appointment) && !isSalonOwner(appointment) && !isAdmin()) {
            throw new ForbiddenException("You are not allowed to modify this booking");
        }
        if (appointment.getStatus() != BookingStatus.PENDING && appointment.getStatus() != BookingStatus.CONFIRMED) {
            throw new BadRequestException("Only pending or confirmed bookings can be rescheduled");
        }
        if (request.getNewAppointmentDate().equals(LocalDate.now()) && request.getNewStartTime().isBefore(LocalTime.now())) {
            throw new BadRequestException("Cannot reschedule to a time in the past");
        }

        Employee employee = appointment.getEmployee();
        if (request.getNewEmployeeId() != null) {
            employee = employeeRepository.findByIdAndSalonIdAndDeletedFalse(request.getNewEmployeeId(), appointment.getSalon().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", request.getNewEmployeeId()));
        }

        List<AppointmentService> lineItems = appointmentServiceRepository.findAllByAppointmentId(appointment.getId());
        int totalDurationMinutes = lineItems.stream().mapToInt(AppointmentService::getDurationMinutes).sum();
        LocalTime newEndTime = request.getNewStartTime().plusMinutes(totalDurationMinutes);

        slotAvailabilityService.ensureSlotAvailable(appointment.getSalon(), employee, request.getNewAppointmentDate(),
                request.getNewStartTime(), newEndTime, appointment.getId());

        appointment.setAppointmentDate(request.getNewAppointmentDate());
        appointment.setStartTime(request.getNewStartTime());
        appointment.setEndTime(newEndTime);
        appointment.setEmployee(employee);
        appointment = appointmentRepository.save(appointment);

        eventPublisher.publishEvent(new BookingModifiedEvent(appointment));

        return toFullResponse(appointment, lineItems);
    }

    @Override
    @Transactional
    public AppointmentResponse cancel(Long appointmentId, CancelRequest request) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", "id", appointmentId));

        if (!isCustomerOwner(appointment) && !isSalonOwner(appointment) && !isAdmin()) {
            throw new ForbiddenException("You are not allowed to cancel this booking");
        }
        if (appointment.getStatus() == BookingStatus.CANCELLED || appointment.getStatus() == BookingStatus.COMPLETED
                || appointment.getStatus() == BookingStatus.REJECTED) {
            throw new BadRequestException("This booking cannot be cancelled from its current status");
        }

        Instant appointmentStart = appointment.getAppointmentDate().atTime(appointment.getStartTime())
                .atZone(ZoneId.systemDefault()).toInstant();
        if (Instant.now().isAfter(appointmentStart)) {
            throw new BadRequestException("Cannot cancel a booking after its scheduled time has started");
        }

        appointment.setStatus(BookingStatus.CANCELLED);
        appointment.setCancelledReason(request != null ? request.getReason() : null);
        appointment.setCancelledAt(Instant.now());
        appointment = appointmentRepository.save(appointment);

        eventPublisher.publishEvent(new BookingCancelledEvent(appointment));

        List<AppointmentService> lineItems = appointmentServiceRepository.findAllByAppointmentId(appointment.getId());
        return toFullResponse(appointment, lineItems);
    }

    @Override
    @Transactional
    public AppointmentResponse confirmBySalon(Long salonId, Long appointmentId) {
        Appointment appointment = getAppointmentForSalon(salonId, appointmentId);
        if (appointment.getStatus() != BookingStatus.PENDING) {
            throw new BadRequestException("Only pending bookings can be confirmed");
        }
        appointment.setStatus(BookingStatus.CONFIRMED);
        appointment = appointmentRepository.save(appointment);

        List<AppointmentService> lineItems = appointmentServiceRepository.findAllByAppointmentId(appointment.getId());
        return toFullResponse(appointment, lineItems);
    }

    @Override
    @Transactional
    public AppointmentResponse rejectBySalon(Long salonId, Long appointmentId, CancelRequest request) {
        Appointment appointment = getAppointmentForSalon(salonId, appointmentId);
        if (appointment.getStatus() != BookingStatus.PENDING) {
            throw new BadRequestException("Only pending bookings can be rejected");
        }
        appointment.setStatus(BookingStatus.REJECTED);
        appointment.setCancelledReason(request != null ? request.getReason() : null);
        appointment.setCancelledAt(Instant.now());
        appointment = appointmentRepository.save(appointment);

        eventPublisher.publishEvent(new BookingRejectedEvent(appointment));

        List<AppointmentService> lineItems = appointmentServiceRepository.findAllByAppointmentId(appointment.getId());
        return toFullResponse(appointment, lineItems);
    }

    @Override
    @Transactional
    public AppointmentResponse markCompleted(Long salonId, Long appointmentId) {
        Appointment appointment = getAppointmentForSalon(salonId, appointmentId);
        if (appointment.getStatus() != BookingStatus.CONFIRMED) {
            throw new BadRequestException("Only confirmed bookings can be marked completed");
        }
        appointment.setStatus(BookingStatus.COMPLETED);
        appointment.setCompletedAt(Instant.now());
        appointment = appointmentRepository.save(appointment);

        eventPublisher.publishEvent(new BookingCompletedEvent(appointment));

        List<AppointmentService> lineItems = appointmentServiceRepository.findAllByAppointmentId(appointment.getId());
        return toFullResponse(appointment, lineItems);
    }

    @Override
    @Transactional
    public AppointmentResponse markNoShow(Long salonId, Long appointmentId) {
        Appointment appointment = getAppointmentForSalon(salonId, appointmentId);
        if (appointment.getStatus() != BookingStatus.PENDING && appointment.getStatus() != BookingStatus.CONFIRMED) {
            throw new BadRequestException("Only pending or confirmed bookings can be marked as no-show");
        }
        appointment.setStatus(BookingStatus.NO_SHOW);
        appointment = appointmentRepository.save(appointment);

        List<AppointmentService> lineItems = appointmentServiceRepository.findAllByAppointmentId(appointment.getId());
        return toFullResponse(appointment, lineItems);
    }

    @Override
    @Transactional(readOnly = true)
    public CreateBookingRequest rebook(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", "id", appointmentId));

        if (!isCustomerOwner(appointment) && !isAdmin()) {
            throw new ForbiddenException("You are not allowed to rebook this appointment");
        }
        if (appointment.getStatus() != BookingStatus.COMPLETED) {
            throw new BadRequestException("Only completed bookings can be rebooked");
        }

        List<Long> serviceIds = appointmentServiceRepository.findAllByAppointmentId(appointment.getId()).stream()
                .map(item -> item.getService().getId())
                .toList();

        return CreateBookingRequest.builder()
                .salonId(appointment.getSalon().getId())
                .employeeId(appointment.getEmployee() != null ? appointment.getEmployee().getId() : null)
                .serviceIds(serviceIds)
                .notes(appointment.getNotes())
                .build();
    }

    private Appointment getAppointmentForSalon(Long salonId, Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", "id", appointmentId));
        if (!appointment.getSalon().getId().equals(salonId)) {
            throw new ResourceNotFoundException("Appointment", "id", appointmentId);
        }
        ensureSalonOwnerOrAdmin(appointment.getSalon());
        return appointment;
    }

    private Appointment resolveAppointment(String idOrBookingNumber) {
        if (idOrBookingNumber != null && idOrBookingNumber.chars().allMatch(Character::isDigit) && !idOrBookingNumber.isBlank()) {
            return appointmentRepository.findById(Long.parseLong(idOrBookingNumber))
                    .orElseThrow(() -> new ResourceNotFoundException("Appointment", "id", idOrBookingNumber));
        }
        return appointmentRepository.findByBookingNumber(idOrBookingNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", "bookingNumber", idOrBookingNumber));
    }

    private void ensureCanView(Appointment appointment) {
        if (isCustomerOwner(appointment) || isSalonOwner(appointment) || isAdmin()) {
            return;
        }
        throw new ForbiddenException("You are not allowed to view this booking");
    }

    private void ensureSalonOwnerOrAdmin(Salon salon) {
        Long currentUserId = securityUtils.getCurrentUserId();
        boolean ownsSalon = salon.getOwner() != null && salon.getOwner().getUser() != null
                && salon.getOwner().getUser().getId().equals(currentUserId);
        if (!ownsSalon && !isAdmin()) {
            throw new ForbiddenException("You do not have access to this salon's bookings");
        }
    }

    private boolean isCustomerOwner(Appointment appointment) {
        return appointment.getCustomer().getId().equals(securityUtils.getCurrentUserId());
    }

    private boolean isSalonOwner(Appointment appointment) {
        Salon salon = appointment.getSalon();
        Long currentUserId = securityUtils.getCurrentUserId();
        return salon.getOwner() != null && salon.getOwner().getUser() != null
                && salon.getOwner().getUser().getId().equals(currentUserId);
    }

    private boolean isAdmin() {
        return securityUtils.getCurrentUser().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_SUPER_ADMIN"));
    }

    private String generateUniqueBookingNumber() {
        String bookingNumber;
        do {
            bookingNumber = RandomCodeGenerator.generateReferenceNumber(AppConstants.BOOKING_PREFIX);
        } while (appointmentRepository.findByBookingNumber(bookingNumber).isPresent());
        return bookingNumber;
    }

    private AppointmentResponse toFullResponse(Appointment appointment, List<AppointmentService> lineItems) {
        AppointmentResponse response = appointmentMapper.toResponse(appointment);
        response.setServices(appointmentServiceMapper.toResponseList(lineItems));
        return response;
    }

    private PagedResponse<AppointmentSummaryResponse> toSummaryPagedResponse(Page<Appointment> page) {
        List<AppointmentSummaryResponse> content = page.getContent().stream()
                .map(this::toSummaryResponse)
                .toList();
        return PagedResponse.from(page, content);
    }

    private AppointmentSummaryResponse toSummaryResponse(Appointment appointment) {
        AppointmentSummaryResponse response = appointmentMapper.toSummary(appointment);
        response.setEmployeeName(appointment.getEmployee() != null ? appointment.getEmployee().getFullName() : null);
        List<String> serviceNames = appointmentServiceRepository.findAllByAppointmentId(appointment.getId()).stream()
                .map(AppointmentService::getServiceName)
                .toList();
        response.setServiceNames(serviceNames);
        return response;
    }
}
