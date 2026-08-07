package com.salofresh.service.membership;

import com.salofresh.dto.membership.MembershipPlanCreateRequest;
import com.salofresh.dto.membership.MembershipPlanResponse;
import com.salofresh.dto.membership.MembershipPlanUpdateRequest;

import java.util.List;

public interface MembershipPlanService {

    MembershipPlanResponse create(Long salonId, MembershipPlanCreateRequest request);

    MembershipPlanResponse update(Long salonId, Long planId, MembershipPlanUpdateRequest request);

    void delete(Long salonId, Long planId);

    /**
     * Publicly visible, active membership plans offered by a salon.
     */
    List<MembershipPlanResponse> listActiveForSalon(Long salonId);
}
