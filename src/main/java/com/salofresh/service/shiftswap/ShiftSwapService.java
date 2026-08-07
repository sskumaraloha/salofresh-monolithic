package com.salofresh.service.shiftswap;

import com.salofresh.common.enums.ShiftSwapStatus;
import com.salofresh.dto.shiftswap.CreateShiftSwapRequest;
import com.salofresh.dto.shiftswap.ShiftSwapDecisionRequest;
import com.salofresh.dto.shiftswap.ShiftSwapResponse;
import com.salofresh.response.PagedResponse;
import org.springframework.data.domain.Pageable;

/**
 * Request/approval workflow for staff-initiated shift swaps.
 *
 * <p>NON-GOAL: approving a swap request never rewrites {@code EmployeeSchedule} rows. This is a
 * visibility and coordination tool for who has agreed to cover a shift; any actual rota change
 * remains a separate, explicit action by the salon owner.</p>
 */
public interface ShiftSwapService {

    /**
     * Creates a new swap request on behalf of {@code employeeId}. The caller (identified by
     * {@code callerUserId}) must either be that employee's own linked user, or the salon owner
     * (required when the employee has no platform login of their own).
     */
    ShiftSwapResponse create(Long callerUserId, Long salonId, Long employeeId, CreateShiftSwapRequest request);

    /**
     * Approves or rejects a pending swap request. Owner-only.
     */
    ShiftSwapResponse decide(Long ownerUserId, Long salonId, Long requestId, ShiftSwapDecisionRequest request);

    /**
     * Cancels a still-pending swap request. Allowed for the requesting employee's own linked user
     * or the salon owner.
     */
    ShiftSwapResponse cancel(Long callerUserId, Long salonId, Long requestId);

    /**
     * Lists all swap requests for the salon, optionally filtered by status. Owner-only.
     */
    PagedResponse<ShiftSwapResponse> listForSalon(Long ownerUserId, Long salonId, ShiftSwapStatus statusFilter,
                                                   Pageable pageable);

    /**
     * Lists swap requests raised by a specific employee. Allowed for that employee's own linked
     * user or the salon owner.
     */
    PagedResponse<ShiftSwapResponse> listForEmployee(Long callerUserId, Long salonId, Long employeeId,
                                                      Pageable pageable);
}
