package com.salofresh.service.impl.waitlist;

import com.salofresh.common.enums.WaitlistStatus;
import com.salofresh.dto.waitlist.JoinWaitlistRequest;
import com.salofresh.dto.waitlist.WaitlistResponse;
import com.salofresh.entity.Employee;
import com.salofresh.entity.Salon;
import com.salofresh.entity.SalonService;
import com.salofresh.entity.User;
import com.salofresh.entity.Waitlist;
import com.salofresh.exception.BadRequestException;
import com.salofresh.exception.ForbiddenException;
import com.salofresh.exception.ResourceNotFoundException;
import com.salofresh.exception.SlotUnavailableException;
import com.salofresh.mapper.waitlist.WaitlistMapper;
import com.salofresh.repository.EmployeeRepository;
import com.salofresh.repository.SalonRepository;
import com.salofresh.repository.SalonServiceRepository;
import com.salofresh.repository.UserRepository;
import com.salofresh.repository.WaitlistRepository;
import com.salofresh.response.PagedResponse;
import com.salofresh.service.booking.SlotAvailabilityService;
import com.salofresh.service.waitlist.WaitlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WaitlistServiceImpl implements WaitlistService {

    private final WaitlistRepository waitlistRepository;
    private final SalonRepository salonRepository;
    private final EmployeeRepository employeeRepository;
    private final SalonServiceRepository salonServiceRepository;
    private final UserRepository userRepository;
    private final WaitlistMapper waitlistMapper;
    private final SlotAvailabilityService slotAvailabilityService;

    @Override
    @Transactional
    public WaitlistResponse join(Long userId, JoinWaitlistRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        Salon salon = salonRepository.findByIdAndDeletedFalse(request.getSalonId())
                .orElseThrow(() -> new ResourceNotFoundException("Salon", "id", request.getSalonId()));

        Employee employee = null;
        if (request.getEmployeeId() != null) {
            employee = employeeRepository.findByIdAndSalonIdAndDeletedFalse(request.getEmployeeId(), salon.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", request.getEmployeeId()));
        }

        SalonService service = null;
        if (request.getServiceId() != null) {
            service = salonServiceRepository.findByIdAndSalonIdAndDeletedFalse(request.getServiceId(), salon.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Service", "id", request.getServiceId()));
        }

        if (request.getPreferredDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("Preferred date cannot be in the past");
        }
        if (request.getPreferredStartTime() != null && request.getPreferredEndTime() != null
                && !request.getPreferredStartTime().isBefore(request.getPreferredEndTime())) {
            throw new BadRequestException("Preferred start time must be before preferred end time");
        }

        // Best-effort check: whether the slot already looks available today is purely informational.
        // Waitlist joining is opt-in regardless of the outcome, so any exception here is swallowed.
        if (request.getPreferredStartTime() != null && request.getPreferredEndTime() != null) {
            try {
                slotAvailabilityService.ensureSlotAvailable(salon, employee, request.getPreferredDate(),
                        request.getPreferredStartTime(), request.getPreferredEndTime(), null);
            } catch (SlotUnavailableException ignored) {
                // Slot is unavailable right now - that's precisely why the user wants to join the waitlist.
            }
        }

        Waitlist waitlist = Waitlist.builder()
                .user(user)
                .salon(salon)
                .employee(employee)
                .service(service)
                .preferredDate(request.getPreferredDate())
                .preferredStartTime(request.getPreferredStartTime())
                .preferredEndTime(request.getPreferredEndTime())
                .status(WaitlistStatus.WAITING)
                .build();
        waitlist = waitlistRepository.save(waitlist);
        return waitlistMapper.toResponse(waitlist);
    }

    @Override
    @Transactional
    public void cancel(Long userId, Long waitlistId) {
        Waitlist waitlist = waitlistRepository.findByIdAndUserId(waitlistId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Waitlist entry", "id", waitlistId));
        if (waitlist.getStatus() == WaitlistStatus.CANCELLED) {
            return;
        }
        waitlist.setStatus(WaitlistStatus.CANCELLED);
        waitlistRepository.save(waitlist);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<WaitlistResponse> listForUser(Long userId, Pageable pageable) {
        Page<Waitlist> page = waitlistRepository.findAllByUserIdOrderByCreatedAtDesc(userId, pageable);
        return toPagedResponse(page);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<WaitlistResponse> listForSalon(Long ownerUserId, Long salonId, WaitlistStatus status, Pageable pageable) {
        Salon salon = salonRepository.findByIdAndDeletedFalse(salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Salon", "id", salonId));
        if (salon.getOwner() == null || salon.getOwner().getUser() == null
                || !salon.getOwner().getUser().getId().equals(ownerUserId)) {
            throw new ForbiddenException("You do not have permission to view this salon's waitlist");
        }

        // WaitlistRepository intentionally exposes only the derived-query methods documented for this
        // feature (no generic "by salon, paginated, filterable by status" query), so the salon-owner
        // demand view is filtered/paginated in memory here rather than adding another repository method.
        List<Waitlist> filtered = waitlistRepository.findAll().stream()
                .filter(w -> w.getSalon() != null && salonId.equals(w.getSalon().getId()))
                .filter(w -> status == null || w.getStatus() == status)
                .sorted(Comparator.comparing(Waitlist::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .toList();

        int total = filtered.size();
        int start = Math.min((int) pageable.getOffset(), total);
        int end = Math.min(start + pageable.getPageSize(), total);
        Page<Waitlist> page = new PageImpl<>(filtered.subList(start, end), pageable, total);
        return toPagedResponse(page);
    }

    private PagedResponse<WaitlistResponse> toPagedResponse(Page<Waitlist> page) {
        List<WaitlistResponse> content = page.getContent().stream()
                .map(waitlistMapper::toResponse)
                .toList();
        return PagedResponse.from(page, content);
    }
}
