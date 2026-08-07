package com.salofresh.service.impl.membership;

import com.salofresh.dto.membership.MembershipPlanCreateRequest;
import com.salofresh.dto.membership.MembershipPlanResponse;
import com.salofresh.dto.membership.MembershipPlanUpdateRequest;
import com.salofresh.entity.MembershipPlan;
import com.salofresh.entity.Salon;
import com.salofresh.exception.ForbiddenException;
import com.salofresh.exception.ResourceNotFoundException;
import com.salofresh.mapper.membership.MembershipPlanMapper;
import com.salofresh.repository.MembershipPlanRepository;
import com.salofresh.repository.SalonRepository;
import com.salofresh.security.SecurityUtils;
import com.salofresh.service.membership.MembershipPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MembershipPlanServiceImpl implements MembershipPlanService {

    private final MembershipPlanRepository membershipPlanRepository;
    private final SalonRepository salonRepository;
    private final MembershipPlanMapper membershipPlanMapper;
    private final SecurityUtils securityUtils;

    @Override
    public MembershipPlanResponse create(Long salonId, MembershipPlanCreateRequest request) {
        Salon salon = getSalonOrThrow(salonId);
        verifyOwnership(salon);

        MembershipPlan plan = MembershipPlan.builder()
                .salon(salon)
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .validityDays(request.getValidityDays())
                .totalSessions(request.getTotalSessions())
                .active(true)
                .build();

        return membershipPlanMapper.toResponse(membershipPlanRepository.save(plan));
    }

    @Override
    public MembershipPlanResponse update(Long salonId, Long planId, MembershipPlanUpdateRequest request) {
        MembershipPlan plan = getPlanOrThrow(salonId, planId);
        verifyOwnership(plan.getSalon());

        plan.setName(request.getName());
        plan.setDescription(request.getDescription());
        plan.setPrice(request.getPrice());
        plan.setValidityDays(request.getValidityDays());
        plan.setTotalSessions(request.getTotalSessions());
        if (request.getActive() != null) {
            plan.setActive(request.getActive());
        }

        return membershipPlanMapper.toResponse(membershipPlanRepository.save(plan));
    }

    @Override
    public void delete(Long salonId, Long planId) {
        MembershipPlan plan = getPlanOrThrow(salonId, planId);
        verifyOwnership(plan.getSalon());
        plan.setActive(false);
        plan.setDeleted(true);
        membershipPlanRepository.save(plan);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MembershipPlanResponse> listActiveForSalon(Long salonId) {
        return membershipPlanRepository.findAllBySalonIdAndActiveTrue(salonId).stream()
                .map(membershipPlanMapper::toResponse)
                .toList();
    }

    private Salon getSalonOrThrow(Long salonId) {
        return salonRepository.findById(salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Salon", "id", salonId));
    }

    private MembershipPlan getPlanOrThrow(Long salonId, Long planId) {
        return membershipPlanRepository.findByIdAndSalonId(planId, salonId)
                .orElseThrow(() -> new ResourceNotFoundException("MembershipPlan", "id", planId));
    }

    private void verifyOwnership(Salon salon) {
        Long currentUserId = securityUtils.getCurrentUserId();
        if (salon.getOwner() == null || salon.getOwner().getUser() == null
                || !salon.getOwner().getUser().getId().equals(currentUserId)) {
            throw new ForbiddenException("You do not have permission to manage membership plans for this salon");
        }
    }
}
