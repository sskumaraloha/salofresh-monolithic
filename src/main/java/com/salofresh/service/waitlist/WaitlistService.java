package com.salofresh.service.waitlist;

import com.salofresh.common.enums.WaitlistStatus;
import com.salofresh.dto.waitlist.JoinWaitlistRequest;
import com.salofresh.dto.waitlist.WaitlistResponse;
import com.salofresh.response.PagedResponse;
import org.springframework.data.domain.Pageable;

/**
 * Waitlist / notify-me service: lets a customer register interest in a salon/employee/date
 * that is fully booked (or simply preferred) and get notified automatically when a matching
 * slot frees up.
 */
public interface WaitlistService {

    WaitlistResponse join(Long userId, JoinWaitlistRequest request);

    void cancel(Long userId, Long waitlistId);

    PagedResponse<WaitlistResponse> listForUser(Long userId, Pageable pageable);

    PagedResponse<WaitlistResponse> listForSalon(Long ownerUserId, Long salonId, WaitlistStatus status, Pageable pageable);
}
