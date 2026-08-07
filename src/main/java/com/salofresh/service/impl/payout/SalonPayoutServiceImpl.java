package com.salofresh.service.impl.payout;

import com.salofresh.common.enums.BookingStatus;
import com.salofresh.common.enums.MembershipStatus;
import com.salofresh.common.enums.PayoutStatus;
import com.salofresh.config.AppProperties;
import com.salofresh.dto.payout.GeneratePayoutRequest;
import com.salofresh.dto.payout.ProcessPayoutRequest;
import com.salofresh.dto.payout.SalonPayoutResponse;
import com.salofresh.entity.Appointment;
import com.salofresh.entity.PlatformSubscription;
import com.salofresh.entity.Salon;
import com.salofresh.entity.SalonOwner;
import com.salofresh.entity.SalonPayout;
import com.salofresh.exception.BadRequestException;
import com.salofresh.exception.ForbiddenException;
import com.salofresh.exception.ResourceNotFoundException;
import com.salofresh.mapper.payout.SalonPayoutMapper;
import com.salofresh.repository.AppointmentRepository;
import com.salofresh.repository.PlatformSubscriptionRepository;
import com.salofresh.repository.SalonPayoutRepository;
import com.salofresh.repository.SalonRepository;
import com.salofresh.security.SecurityUtils;
import com.salofresh.service.payout.SalonPayoutService;
import com.salofresh.util.RandomCodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Computes salon settlement payouts from completed-appointment revenue and the applicable
 * platform commission. The commission percentage is read from the salon owner's active
 * {@link PlatformSubscription} plan when that module's repository is available and returns a
 * result; otherwise the configured default ({@code app.payout.default-commission-percentage})
 * is used. {@link PlatformSubscriptionRepository} is injected as an {@link Optional} so this
 * feature keeps working even if that module's bean is absent from the context.
 */
@Service
@RequiredArgsConstructor
public class SalonPayoutServiceImpl implements SalonPayoutService {

    private static final Set<String> ADMIN_AUTHORITIES = Set.of("ROLE_ADMIN", "ROLE_SUPER_ADMIN");

    private final SalonPayoutRepository salonPayoutRepository;
    private final SalonRepository salonRepository;
    private final AppointmentRepository appointmentRepository;
    private final Optional<PlatformSubscriptionRepository> platformSubscriptionRepository;
    private final SalonPayoutMapper salonPayoutMapper;
    private final SecurityUtils securityUtils;
    private final AppProperties appProperties;

    @Override
    @Transactional
    public SalonPayoutResponse generatePayout(Long adminUserId, GeneratePayoutRequest request) {
        if (request.getPeriodStart().isAfter(request.getPeriodEnd())) {
            throw new BadRequestException("Period start date must not be after period end date");
        }

        Salon salon = findSalon(request.getSalonId());
        assertNoOverlappingPayout(salon.getId(), request);

        BigDecimal grossRevenue = computeGrossRevenue(salon, request);
        BigDecimal commissionPercentage = resolveCommissionPercentage(salon);
        BigDecimal platformCommissionAmount = grossRevenue
                .multiply(commissionPercentage)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal netPayoutAmount = grossRevenue.subtract(platformCommissionAmount);

        SalonPayout payout = SalonPayout.builder()
                .salon(salon)
                .periodStart(request.getPeriodStart())
                .periodEnd(request.getPeriodEnd())
                .grossRevenue(grossRevenue)
                .platformCommissionAmount(platformCommissionAmount)
                .netPayoutAmount(netPayoutAmount)
                .status(PayoutStatus.PENDING)
                .build();

        SalonPayout saved = salonPayoutRepository.save(payout);
        return salonPayoutMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public SalonPayoutResponse processPayout(Long adminUserId, Long payoutId, ProcessPayoutRequest request) {
        SalonPayout payout = findPayout(payoutId);
        if (payout.getStatus() != PayoutStatus.PENDING) {
            throw new BadRequestException("Only a PENDING payout can be processed; current status is " + payout.getStatus());
        }

        String reference = (request != null && request.getPayoutReference() != null && !request.getPayoutReference().isBlank())
                ? request.getPayoutReference()
                : RandomCodeGenerator.generateReferenceNumber("PYT");

        payout.setPayoutReference(reference);
        payout.setStatus(PayoutStatus.PROCESSED);
        SalonPayout saved = salonPayoutRepository.save(payout);
        return salonPayoutMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public SalonPayoutResponse markPaid(Long adminUserId, Long payoutId) {
        SalonPayout payout = findPayout(payoutId);
        if (payout.getStatus() != PayoutStatus.PROCESSED) {
            throw new BadRequestException("Only a PROCESSED payout can be marked as paid; current status is " + payout.getStatus());
        }

        payout.setStatus(PayoutStatus.PAID);
        if (payout.getProcessedAt() == null) {
            payout.setProcessedAt(Instant.now());
        }
        SalonPayout saved = salonPayoutRepository.save(payout);
        return salonPayoutMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SalonPayoutResponse> listAll(Long adminUserId, Pageable pageable) {
        return salonPayoutRepository.findAllByOrderByPeriodStartDesc(pageable).map(salonPayoutMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SalonPayoutResponse> listForSalon(Long callerUserId, Long salonId, Pageable pageable) {
        Salon salon = findSalon(salonId);
        assertCanView(callerUserId, salon);
        return salonPayoutRepository.findAllBySalonIdOrderByPeriodStartDesc(salonId, pageable).map(salonPayoutMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public SalonPayoutResponse getById(Long callerUserId, Long payoutId) {
        SalonPayout payout = findPayout(payoutId);
        assertCanView(callerUserId, payout.getSalon());
        return salonPayoutMapper.toResponse(payout);
    }

    private void assertNoOverlappingPayout(Long salonId, GeneratePayoutRequest request) {
        List<SalonPayout> existing = salonPayoutRepository
                .findAllBySalonIdOrderByPeriodStartDesc(salonId, Pageable.unpaged())
                .getContent();
        boolean overlaps = existing.stream().anyMatch(payout ->
                !request.getPeriodEnd().isBefore(payout.getPeriodStart())
                        && !request.getPeriodStart().isAfter(payout.getPeriodEnd()));
        if (overlaps) {
            throw new BadRequestException("A payout already exists for this salon that overlaps the requested period");
        }
    }

    private BigDecimal computeGrossRevenue(Salon salon, GeneratePayoutRequest request) {
        List<Appointment> appointments = appointmentRepository
                .findAllBySalonId(salon.getId(), Pageable.unpaged())
                .getContent();

        BigDecimal total = BigDecimal.ZERO;
        for (Appointment appointment : appointments) {
            if (appointment.getStatus() == BookingStatus.COMPLETED
                    && appointment.getFinalAmount() != null
                    && !appointment.getAppointmentDate().isBefore(request.getPeriodStart())
                    && !appointment.getAppointmentDate().isAfter(request.getPeriodEnd())) {
                total = total.add(appointment.getFinalAmount());
            }
        }
        return total;
    }

    /**
     * Resolves the commission percentage from the salon owner's active platform subscription
     * plan when available, falling back to the configured platform default otherwise.
     */
    private BigDecimal resolveCommissionPercentage(Salon salon) {
        SalonOwner owner = salon.getOwner();
        if (owner != null && platformSubscriptionRepository.isPresent()) {
            Optional<PlatformSubscription> subscription = platformSubscriptionRepository.get()
                    .findFirstBySalonOwnerIdAndStatusOrderByEndDateDesc(owner.getId(), MembershipStatus.ACTIVE);
            if (subscription.isPresent() && subscription.get().getPlan() != null
                    && subscription.get().getPlan().getCommissionPercentage() != null) {
                return subscription.get().getPlan().getCommissionPercentage();
            }
        }
        return appProperties.getPayout().getDefaultCommissionPercentage();
    }

    private void assertCanView(Long callerUserId, Salon salon) {
        if (isAdmin()) {
            return;
        }
        if (salon.getOwner() == null || salon.getOwner().getUser() == null
                || !salon.getOwner().getUser().getId().equals(callerUserId)) {
            throw new ForbiddenException("You do not have permission to view this salon's payouts");
        }
    }

    private boolean isAdmin() {
        if (!securityUtils.isAuthenticated()) {
            return false;
        }
        return securityUtils.getCurrentUser().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(ADMIN_AUTHORITIES::contains);
    }

    private Salon findSalon(Long salonId) {
        return salonRepository.findByIdAndDeletedFalse(salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Salon", "id", salonId));
    }

    private SalonPayout findPayout(Long payoutId) {
        return salonPayoutRepository.findById(payoutId)
                .orElseThrow(() -> new ResourceNotFoundException("SalonPayout", "id", payoutId));
    }
}
