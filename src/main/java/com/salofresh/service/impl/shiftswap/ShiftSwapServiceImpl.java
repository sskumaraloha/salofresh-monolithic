package com.salofresh.service.impl.shiftswap;

import com.salofresh.common.enums.NotificationChannel;
import com.salofresh.common.enums.NotificationType;
import com.salofresh.common.enums.ShiftSwapStatus;
import com.salofresh.dto.shiftswap.CreateShiftSwapRequest;
import com.salofresh.dto.shiftswap.ShiftSwapDecisionRequest;
import com.salofresh.dto.shiftswap.ShiftSwapResponse;
import com.salofresh.entity.Employee;
import com.salofresh.entity.Salon;
import com.salofresh.entity.ShiftSwapRequest;
import com.salofresh.entity.User;
import com.salofresh.exception.BadRequestException;
import com.salofresh.exception.ForbiddenException;
import com.salofresh.exception.ResourceNotFoundException;
import com.salofresh.mapper.shiftswap.ShiftSwapMapper;
import com.salofresh.notification.NotificationService;
import com.salofresh.repository.EmployeeRepository;
import com.salofresh.repository.SalonRepository;
import com.salofresh.repository.ShiftSwapRequestRepository;
import com.salofresh.response.PagedResponse;
import com.salofresh.service.shiftswap.ShiftSwapService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShiftSwapServiceImpl implements ShiftSwapService {

    private final ShiftSwapRequestRepository shiftSwapRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final SalonRepository salonRepository;
    private final ShiftSwapMapper shiftSwapMapper;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public ShiftSwapResponse create(Long callerUserId, Long salonId, Long employeeId, CreateShiftSwapRequest request) {
        Employee employee = getEmployeeOrThrow(salonId, employeeId);
        verifyRequesterOrOwner(employee, callerUserId);

        if (!request.getOriginalEndTime().isAfter(request.getOriginalStartTime())) {
            throw new BadRequestException("Original end time must be after the original start time");
        }

        Employee targetEmployee = null;
        if (request.getTargetEmployeeId() != null) {
            if (request.getTargetEmployeeId().equals(employee.getId())) {
                throw new BadRequestException("You cannot name yourself as the covering employee");
            }
            targetEmployee = employeeRepository.findByIdAndSalonIdAndDeletedFalse(request.getTargetEmployeeId(), salonId)
                    .orElseThrow(() -> new BadRequestException("Target employee must belong to the same salon"));
        }

        ShiftSwapRequest swap = ShiftSwapRequest.builder()
                .salon(employee.getSalon())
                .requestingEmployee(employee)
                .targetEmployee(targetEmployee)
                .shiftDate(request.getShiftDate())
                .originalStartTime(request.getOriginalStartTime())
                .originalEndTime(request.getOriginalEndTime())
                .reason(request.getReason())
                .build();

        ShiftSwapRequest saved = shiftSwapRequestRepository.save(swap);

        notifyTargetEmployeeNamed(saved);
        notifyOwnerOfNewRequest(saved);

        return shiftSwapMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public ShiftSwapResponse decide(Long ownerUserId, Long salonId, Long requestId, ShiftSwapDecisionRequest request) {
        Salon salon = getSalonOrThrow(salonId);
        verifyOwnership(salon, ownerUserId);

        ShiftSwapRequest swap = getRequestOrThrow(salonId, requestId);
        if (swap.getStatus() != ShiftSwapStatus.PENDING) {
            throw new BadRequestException("Only pending shift-swap requests can be approved or rejected");
        }

        boolean approve = Boolean.TRUE.equals(request.getApprove());
        swap.setStatus(approve ? ShiftSwapStatus.APPROVED : ShiftSwapStatus.REJECTED);
        swap.setRespondedBy(salon.getOwner().getUser());
        swap.setRespondedAt(Instant.now());
        if (request.getNote() != null && !request.getNote().isBlank()) {
            String existingReason = swap.getReason() == null ? "" : swap.getReason();
            swap.setReason(existingReason + " [Decision note: " + request.getNote() + "]");
        }

        ShiftSwapRequest saved = shiftSwapRequestRepository.save(swap);

        notifyRequesterOfDecision(saved);

        return shiftSwapMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public ShiftSwapResponse cancel(Long callerUserId, Long salonId, Long requestId) {
        ShiftSwapRequest swap = getRequestOrThrow(salonId, requestId);

        boolean isOwner = isSalonOwner(swap.getSalon(), callerUserId);
        boolean isSelf = swap.getRequestingEmployee().getUser() != null
                && swap.getRequestingEmployee().getUser().getId().equals(callerUserId);
        if (!isOwner && !isSelf) {
            throw new ForbiddenException("You do not have permission to cancel this shift-swap request");
        }

        if (swap.getStatus() != ShiftSwapStatus.PENDING) {
            throw new BadRequestException("Only pending shift-swap requests can be cancelled");
        }

        swap.setStatus(ShiftSwapStatus.CANCELLED);
        ShiftSwapRequest saved = shiftSwapRequestRepository.save(swap);
        return shiftSwapMapper.toResponse(saved);
    }

    @Override
    public PagedResponse<ShiftSwapResponse> listForSalon(Long ownerUserId, Long salonId, ShiftSwapStatus statusFilter,
                                                          Pageable pageable) {
        Salon salon = getSalonOrThrow(salonId);
        verifyOwnership(salon, ownerUserId);

        Page<ShiftSwapRequest> page = statusFilter != null
                ? shiftSwapRequestRepository.findAllBySalonIdAndStatusOrderByCreatedAtDesc(salonId, statusFilter, pageable)
                : shiftSwapRequestRepository.findAllBySalonIdOrderByCreatedAtDesc(salonId, pageable);

        return toPagedResponse(page);
    }

    @Override
    public PagedResponse<ShiftSwapResponse> listForEmployee(Long callerUserId, Long salonId, Long employeeId,
                                                             Pageable pageable) {
        Employee employee = getEmployeeOrThrow(salonId, employeeId);
        verifyRequesterOrOwner(employee, callerUserId);

        Page<ShiftSwapRequest> page =
                shiftSwapRequestRepository.findAllByRequestingEmployeeIdOrderByCreatedAtDesc(employeeId, pageable);

        return toPagedResponse(page);
    }

    private PagedResponse<ShiftSwapResponse> toPagedResponse(Page<ShiftSwapRequest> page) {
        List<ShiftSwapResponse> content = page.getContent().stream()
                .map(shiftSwapMapper::toResponse)
                .collect(Collectors.toList());
        return PagedResponse.from(page, content);
    }

    private Salon getSalonOrThrow(Long salonId) {
        return salonRepository.findByIdAndDeletedFalse(salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Salon", "id", salonId));
    }

    private Employee getEmployeeOrThrow(Long salonId, Long employeeId) {
        return employeeRepository.findByIdAndSalonIdAndDeletedFalse(employeeId, salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId));
    }

    private ShiftSwapRequest getRequestOrThrow(Long salonId, Long requestId) {
        return shiftSwapRequestRepository.findByIdAndSalonId(requestId, salonId)
                .orElseThrow(() -> new ResourceNotFoundException("ShiftSwapRequest", "id", requestId));
    }

    private boolean isSalonOwner(Salon salon, Long userId) {
        return salon.getOwner() != null && salon.getOwner().getUser() != null
                && salon.getOwner().getUser().getId().equals(userId);
    }

    private void verifyOwnership(Salon salon, Long userId) {
        if (!isSalonOwner(salon, userId)) {
            throw new ForbiddenException("You do not have permission to manage shift-swap requests for this salon");
        }
    }

    private void verifyRequesterOrOwner(Employee employee, Long userId) {
        boolean isOwner = isSalonOwner(employee.getSalon(), userId);
        boolean isSelf = employee.getUser() != null && employee.getUser().getId().equals(userId);
        if (!isOwner && !isSelf) {
            throw new ForbiddenException("You do not have permission to access shift-swap requests for this employee");
        }
    }

    private void notifyTargetEmployeeNamed(ShiftSwapRequest swap) {
        Employee target = swap.getTargetEmployee();
        if (target == null || target.getUser() == null) {
            return;
        }
        User targetUser = target.getUser();
        notificationService.createAndDispatch(targetUser, NotificationType.GENERIC, NotificationChannel.IN_APP,
                "Shift Swap Request",
                "%s asked you to cover their shift on %s (%s - %s).".formatted(
                        swap.getRequestingEmployee().getFullName(), swap.getShiftDate(),
                        swap.getOriginalStartTime(), swap.getOriginalEndTime()),
                swap.getId().toString(), "SHIFT_SWAP_REQUEST");
    }

    private void notifyOwnerOfNewRequest(ShiftSwapRequest swap) {
        User ownerUser = swap.getSalon().getOwner() == null ? null : swap.getSalon().getOwner().getUser();
        if (ownerUser == null) {
            return;
        }
        notificationService.createAndDispatch(ownerUser, NotificationType.GENERIC, NotificationChannel.IN_APP,
                "New Shift Swap Request",
                "%s requested a shift swap for %s (%s - %s) and it needs your approval.".formatted(
                        swap.getRequestingEmployee().getFullName(), swap.getShiftDate(),
                        swap.getOriginalStartTime(), swap.getOriginalEndTime()),
                swap.getId().toString(), "SHIFT_SWAP_REQUEST");
    }

    private void notifyRequesterOfDecision(ShiftSwapRequest swap) {
        User requesterUser = swap.getRequestingEmployee().getUser();
        if (requesterUser == null) {
            return;
        }
        String verb = swap.getStatus() == ShiftSwapStatus.APPROVED ? "approved" : "rejected";
        notificationService.createAndDispatch(requesterUser, NotificationType.GENERIC, NotificationChannel.IN_APP,
                "Shift Swap Request " + (swap.getStatus() == ShiftSwapStatus.APPROVED ? "Approved" : "Rejected"),
                "Your shift-swap request for %s (%s - %s) was %s.".formatted(
                        swap.getShiftDate(), swap.getOriginalStartTime(), swap.getOriginalEndTime(), verb),
                swap.getId().toString(), "SHIFT_SWAP_REQUEST");
    }
}
