package com.salofresh.service.impl.employee;

import com.salofresh.common.enums.BookingStatus;
import com.salofresh.common.enums.EmploymentStatus;
import com.salofresh.dto.employee.EmployeeCreateRequest;
import com.salofresh.dto.employee.EmployeePerformanceResponse;
import com.salofresh.dto.employee.EmployeeResponse;
import com.salofresh.dto.employee.EmployeeUpdateRequest;
import com.salofresh.entity.Appointment;
import com.salofresh.entity.Employee;
import com.salofresh.entity.Salon;
import com.salofresh.entity.SalonService;
import com.salofresh.exception.BadRequestException;
import com.salofresh.exception.ForbiddenException;
import com.salofresh.exception.ResourceNotFoundException;
import com.salofresh.file.FileStorageService;
import com.salofresh.mapper.employee.EmployeeMapper;
import com.salofresh.repository.AppointmentRepository;
import com.salofresh.repository.EmployeeRepository;
import com.salofresh.repository.SalonRepository;
import com.salofresh.repository.SalonServiceRepository;
import com.salofresh.response.PagedResponse;
import com.salofresh.security.SecurityUtils;
import com.salofresh.service.employee.EmployeeService;
import com.salofresh.specification.EmployeeSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private static final String EMPLOYEE_PHOTO_SUBDIRECTORY = "employee-photos";

    private final EmployeeRepository employeeRepository;
    private final SalonRepository salonRepository;
    private final SalonServiceRepository salonServiceRepository;
    private final AppointmentRepository appointmentRepository;
    private final EmployeeMapper employeeMapper;
    private final FileStorageService fileStorageService;
    private final SecurityUtils securityUtils;

    @Override
    @Transactional
    public EmployeeResponse create(Long salonId, EmployeeCreateRequest request) {
        Salon salon = getSalonOrThrow(salonId);
        verifyOwnership(salon);

        Employee employee = employeeMapper.toEntity(request);
        employee.setSalon(salon);
        employee.setServices(resolveServices(salon, request.getServiceIds()));

        Employee saved = employeeRepository.save(employee);
        return employeeMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public EmployeeResponse update(Long salonId, Long employeeId, EmployeeUpdateRequest request) {
        Employee employee = getEmployeeOrThrow(salonId, employeeId);
        verifyOwnership(employee.getSalon());

        employeeMapper.updateEntityFromRequest(request, employee);
        if (request.getServiceIds() != null) {
            employee.setServices(resolveServices(employee.getSalon(), request.getServiceIds()));
        }

        Employee saved = employeeRepository.save(employee);
        return employeeMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long salonId, Long employeeId) {
        Employee employee = getEmployeeOrThrow(salonId, employeeId);
        verifyOwnership(employee.getSalon());

        employee.setDeleted(true);
        employeeRepository.save(employee);
    }

    @Override
    public EmployeeResponse getById(Long salonId, Long employeeId) {
        Employee employee = getEmployeeOrThrow(salonId, employeeId);
        return employeeMapper.toResponse(employee);
    }

    @Override
    public PagedResponse<EmployeeResponse> listBySalon(Long salonId, EmploymentStatus status, Long serviceId, Pageable pageable) {
        Specification<Employee> spec = Specification
                .where(EmployeeSpecification.hasSalon(salonId))
                .and(EmployeeSpecification.isNotDeleted())
                .and(EmployeeSpecification.hasEmploymentStatus(status))
                .and(EmployeeSpecification.offersService(serviceId));

        Page<Employee> page = employeeRepository.findAll(spec, pageable);
        List<EmployeeResponse> content = page.getContent().stream()
                .map(employeeMapper::toResponse)
                .collect(Collectors.toList());
        return PagedResponse.from(page, content);
    }

    @Override
    @Transactional
    public EmployeeResponse assignServices(Long salonId, Long employeeId, List<Long> serviceIds) {
        Employee employee = getEmployeeOrThrow(salonId, employeeId);
        verifyOwnership(employee.getSalon());

        employee.setServices(resolveServices(employee.getSalon(), serviceIds));
        Employee saved = employeeRepository.save(employee);
        return employeeMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public EmployeeResponse uploadPhoto(Long salonId, Long employeeId, MultipartFile file) {
        Employee employee = getEmployeeOrThrow(salonId, employeeId);
        verifyOwnership(employee.getSalon());

        String previousPhotoUrl = employee.getProfileImageUrl();
        String newPhotoUrl = fileStorageService.store(file, EMPLOYEE_PHOTO_SUBDIRECTORY);
        employee.setProfileImageUrl(newPhotoUrl);
        Employee saved = employeeRepository.save(employee);

        if (previousPhotoUrl != null && !previousPhotoUrl.isBlank()) {
            fileStorageService.delete(previousPhotoUrl);
        }

        return employeeMapper.toResponse(saved);
    }

    @Override
    public EmployeePerformanceResponse getPerformance(Long salonId, Long employeeId) {
        Employee employee = getEmployeeOrThrow(salonId, employeeId);
        verifyOwnership(employee.getSalon());

        Specification<Appointment> byEmployee =
                (root, query, cb) -> cb.equal(root.get("employee").get("id"), employeeId);
        Specification<Appointment> completedSpec = Specification.where(byEmployee)
                .and((root, query, cb) -> cb.equal(root.get("status"), BookingStatus.COMPLETED));
        Specification<Appointment> cancelledSpec = Specification.where(byEmployee)
                .and((root, query, cb) -> cb.equal(root.get("status"), BookingStatus.CANCELLED));

        long completedCount = appointmentRepository.count(completedSpec);
        long cancelledCount = appointmentRepository.count(cancelledSpec);
        BigDecimal totalRevenue = appointmentRepository.findAll(completedSpec).stream()
                .map(Appointment::getFinalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return EmployeePerformanceResponse.builder()
                .employeeId(employeeId)
                .totalAppointmentsCompleted(completedCount)
                .totalAppointmentsCancelled(cancelledCount)
                .ratingAverage(employee.getRatingAverage())
                .totalRevenueGenerated(totalRevenue)
                .build();
    }

    private Salon getSalonOrThrow(Long salonId) {
        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Salon", "id", salonId));
        if (salon.isDeleted()) {
            throw new ResourceNotFoundException("Salon", "id", salonId);
        }
        return salon;
    }

    private Employee getEmployeeOrThrow(Long salonId, Long employeeId) {
        return employeeRepository.findByIdAndSalonIdAndDeletedFalse(employeeId, salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId));
    }

    private void verifyOwnership(Salon salon) {
        Long currentUserId = securityUtils.getCurrentUserId();
        if (salon.getOwner() == null || salon.getOwner().getUser() == null
                || !salon.getOwner().getUser().getId().equals(currentUserId)) {
            throw new ForbiddenException("You do not have permission to manage employees for this salon");
        }
    }

    private Set<SalonService> resolveServices(Salon salon, List<Long> serviceIds) {
        if (serviceIds == null || serviceIds.isEmpty()) {
            return new HashSet<>();
        }
        Set<SalonService> services = new HashSet<>();
        for (Long serviceId : serviceIds) {
            SalonService service = salonServiceRepository.findByIdAndSalonIdAndDeletedFalse(serviceId, salon.getId())
                    .orElseThrow(() -> new BadRequestException(
                            "Service id " + serviceId + " does not belong to this salon"));
            services.add(service);
        }
        return services;
    }
}
