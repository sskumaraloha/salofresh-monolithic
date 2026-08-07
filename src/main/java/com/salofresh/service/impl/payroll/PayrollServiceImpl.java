package com.salofresh.service.impl.payroll;

import com.salofresh.common.enums.BookingStatus;
import com.salofresh.common.enums.CommissionRuleType;
import com.salofresh.common.enums.PayrollStatus;
import com.salofresh.dto.payroll.GeneratePayrollRequest;
import com.salofresh.dto.payroll.PayrollRecordResponse;
import com.salofresh.entity.Appointment;
import com.salofresh.entity.AppointmentService;
import com.salofresh.entity.CommissionRule;
import com.salofresh.entity.Employee;
import com.salofresh.entity.PayrollRecord;
import com.salofresh.entity.Salon;
import com.salofresh.exception.BadRequestException;
import com.salofresh.exception.ForbiddenException;
import com.salofresh.exception.ResourceNotFoundException;
import com.salofresh.mapper.payroll.PayrollRecordMapper;
import com.salofresh.repository.AppointmentRepository;
import com.salofresh.repository.AppointmentServiceRepository;
import com.salofresh.repository.CommissionRuleRepository;
import com.salofresh.repository.EmployeeRepository;
import com.salofresh.repository.PayrollRecordRepository;
import com.salofresh.repository.SalonRepository;
import com.salofresh.response.PagedResponse;
import com.salofresh.service.payroll.PayrollService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PayrollServiceImpl implements PayrollService {

    private final PayrollRecordRepository payrollRecordRepository;
    private final EmployeeRepository employeeRepository;
    private final SalonRepository salonRepository;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentServiceRepository appointmentServiceRepository;
    private final CommissionRuleRepository commissionRuleRepository;
    private final PayrollRecordMapper payrollRecordMapper;

    @Override
    @Transactional
    public PayrollRecordResponse generatePayroll(Long ownerUserId, Long salonId, GeneratePayrollRequest request) {
        Salon salon = getSalonOrThrow(salonId);
        verifyOwnership(salon, ownerUserId);

        Employee employee = employeeRepository.findByIdAndSalonIdAndDeletedFalse(request.getEmployeeId(), salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", request.getEmployeeId()));

        if (request.getPeriodStart().isAfter(request.getPeriodEnd())) {
            throw new BadRequestException("Period start must not be after period end");
        }

        List<PayrollRecord> existing = payrollRecordRepository.findAllByEmployeeIdAndPeriodStartAndPeriodEnd(
                employee.getId(), request.getPeriodStart(), request.getPeriodEnd());
        if (!existing.isEmpty()) {
            throw new BadRequestException("A payroll record already exists for this employee and period");
        }

        BigDecimal deductions = request.getDeductions() != null ? request.getDeductions() : BigDecimal.ZERO;
        BigDecimal bonus = request.getBonus() != null ? request.getBonus() : BigDecimal.ZERO;

        BigDecimal commissionEarned = computeCommission(salonId, employee.getId(),
                request.getPeriodStart(), request.getPeriodEnd());

        BigDecimal netPay = request.getBaseSalary()
                .add(commissionEarned)
                .add(bonus)
                .subtract(deductions);

        PayrollRecord record = PayrollRecord.builder()
                .employee(employee)
                .salon(salon)
                .periodStart(request.getPeriodStart())
                .periodEnd(request.getPeriodEnd())
                .baseSalary(request.getBaseSalary())
                .commissionEarned(commissionEarned)
                .deductions(deductions)
                .bonus(bonus)
                .netPay(netPay)
                .status(PayrollStatus.DRAFT)
                .build();

        PayrollRecord saved = payrollRecordRepository.save(record);
        return payrollRecordMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public PayrollRecordResponse finalizePayroll(Long ownerUserId, Long payrollRecordId) {
        PayrollRecord record = payrollRecordRepository.findById(payrollRecordId)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll record", "id", payrollRecordId));
        verifyOwnership(record.getSalon(), ownerUserId);

        if (record.getStatus() != PayrollStatus.DRAFT) {
            throw new BadRequestException("Only DRAFT payroll records can be finalized");
        }

        record.setStatus(PayrollStatus.FINALIZED);
        PayrollRecord saved = payrollRecordRepository.save(record);
        return payrollRecordMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public PayrollRecordResponse markPaid(Long ownerUserId, Long payrollRecordId) {
        PayrollRecord record = payrollRecordRepository.findById(payrollRecordId)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll record", "id", payrollRecordId));
        verifyOwnership(record.getSalon(), ownerUserId);

        if (record.getStatus() != PayrollStatus.FINALIZED) {
            throw new BadRequestException("Only FINALIZED payroll records can be marked as paid");
        }

        record.setStatus(PayrollStatus.PAID);
        record.setPaidAt(Instant.now());
        PayrollRecord saved = payrollRecordRepository.save(record);
        return payrollRecordMapper.toResponse(saved);
    }

    @Override
    public PagedResponse<PayrollRecordResponse> listForSalon(Long ownerUserId, Long salonId, Pageable pageable) {
        Salon salon = getSalonOrThrow(salonId);
        verifyOwnership(salon, ownerUserId);

        Page<PayrollRecord> page = payrollRecordRepository.findAllBySalonIdOrderByPeriodStartDesc(salonId, pageable);
        List<PayrollRecordResponse> content = page.getContent().stream()
                .map(payrollRecordMapper::toResponse)
                .collect(Collectors.toList());
        return PagedResponse.from(page, content);
    }

    @Override
    public PagedResponse<PayrollRecordResponse> listForEmployee(Long currentUserId, Long employeeId, Pageable pageable) {
        Employee employee = employeeRepository.findById(employeeId)
                .filter(e -> !e.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId));

        boolean isSelf = employee.getUser() != null && employee.getUser().getId().equals(currentUserId);
        boolean isOwner = employee.getSalon() != null && employee.getSalon().getOwner() != null
                && employee.getSalon().getOwner().getUser() != null
                && employee.getSalon().getOwner().getUser().getId().equals(currentUserId);

        if (!isSelf && !isOwner) {
            throw new ForbiddenException("You do not have permission to view payroll records for this employee");
        }

        Page<PayrollRecord> page = payrollRecordRepository.findAllByEmployeeIdOrderByPeriodStartDesc(employeeId, pageable);
        List<PayrollRecordResponse> content = page.getContent().stream()
                .map(payrollRecordMapper::toResponse)
                .collect(Collectors.toList());
        return PagedResponse.from(page, content);
    }

    @Override
    public PayrollRecordResponse getById(Long ownerUserId, Long salonId, Long payrollRecordId) {
        PayrollRecord record = payrollRecordRepository.findByIdAndSalonId(payrollRecordId, salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll record", "id", payrollRecordId));
        verifyOwnership(record.getSalon(), ownerUserId);
        return payrollRecordMapper.toResponse(record);
    }

    /**
     * Computes the total commission earned by an employee for all COMPLETED appointments
     * whose appointment date falls within [periodStart, periodEnd] (inclusive).
     *
     * For each service line item on a completed appointment, the applicable commission
     * rule is resolved with the following precedence (first match wins):
     *   1. An active rule scoped to this exact employee AND the line item's service category.
     *   2. An active rule scoped to this employee with no category restriction.
     *   3. An active salon-wide rule (no employee) matching the line item's category.
     *   4. An active salon-wide rule with no employee and no category restriction.
     *   5. No match -> the line item contributes zero commission.
     */
    private BigDecimal computeCommission(Long salonId, Long employeeId, LocalDate periodStart, LocalDate periodEnd) {
        List<CommissionRule> activeRules = commissionRuleRepository.findAllBySalonIdAndActiveTrue(salonId);
        BigDecimal totalCommission = BigDecimal.ZERO;

        for (LocalDate date = periodStart; !date.isAfter(periodEnd); date = date.plusDays(1)) {
            List<Appointment> appointments = appointmentRepository
                    .findAllByEmployeeIdAndAppointmentDate(employeeId, date);

            for (Appointment appointment : appointments) {
                if (appointment.getStatus() != BookingStatus.COMPLETED) {
                    continue;
                }

                List<AppointmentService> lineItems =
                        appointmentServiceRepository.findAllByAppointmentId(appointment.getId());

                for (AppointmentService lineItem : lineItems) {
                    Long categoryId = resolveCategoryId(lineItem);
                    CommissionRule rule = resolveApplicableRule(activeRules, employeeId, categoryId);
                    totalCommission = totalCommission.add(calculateLineItemCommission(rule, lineItem));
                }
            }
        }

        return totalCommission.setScale(2, RoundingMode.HALF_UP);
    }

    private Long resolveCategoryId(AppointmentService lineItem) {
        if (lineItem.getService() == null || lineItem.getService().getCategory() == null) {
            return null;
        }
        return lineItem.getService().getCategory().getId();
    }

    private CommissionRule resolveApplicableRule(List<CommissionRule> rules, Long employeeId, Long categoryId) {
        // 1. Employee-specific rule matching the exact category.
        if (categoryId != null) {
            Optional<CommissionRule> match = rules.stream()
                    .filter(r -> isForEmployee(r, employeeId) && isForCategory(r, categoryId))
                    .findFirst();
            if (match.isPresent()) {
                return match.get();
            }
        }

        // 2. Employee-specific rule with no category restriction.
        Optional<CommissionRule> match = rules.stream()
                .filter(r -> isForEmployee(r, employeeId) && r.getCategory() == null)
                .findFirst();
        if (match.isPresent()) {
            return match.get();
        }

        // 3. Salon-wide rule (no employee) matching the exact category.
        if (categoryId != null) {
            match = rules.stream()
                    .filter(r -> r.getEmployee() == null && isForCategory(r, categoryId))
                    .findFirst();
            if (match.isPresent()) {
                return match.get();
            }
        }

        // 4. Salon-wide rule with no employee and no category restriction.
        match = rules.stream()
                .filter(r -> r.getEmployee() == null && r.getCategory() == null)
                .findFirst();

        // 5. No match -> null (zero commission).
        return match.orElse(null);
    }

    private boolean isForEmployee(CommissionRule rule, Long employeeId) {
        return rule.getEmployee() != null && rule.getEmployee().getId().equals(employeeId);
    }

    private boolean isForCategory(CommissionRule rule, Long categoryId) {
        return rule.getCategory() != null && rule.getCategory().getId().equals(categoryId);
    }

    private BigDecimal calculateLineItemCommission(CommissionRule rule, AppointmentService lineItem) {
        if (rule == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal quantity = BigDecimal.valueOf(lineItem.getQuantity());
        BigDecimal price = lineItem.getPrice() != null ? lineItem.getPrice() : BigDecimal.ZERO;

        if (rule.getRuleType() == CommissionRuleType.PERCENTAGE) {
            return price.multiply(quantity)
                    .multiply(rule.getValue())
                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        }

        // FLAT_PER_SERVICE
        return rule.getValue().multiply(quantity);
    }

    private Salon getSalonOrThrow(Long salonId) {
        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Salon", "id", salonId));
        if (salon.isDeleted()) {
            throw new ResourceNotFoundException("Salon", "id", salonId);
        }
        return salon;
    }

    private void verifyOwnership(Salon salon, Long ownerUserId) {
        if (salon.getOwner() == null || salon.getOwner().getUser() == null
                || !salon.getOwner().getUser().getId().equals(ownerUserId)) {
            throw new ForbiddenException("You do not have permission to manage payroll for this salon");
        }
    }
}
