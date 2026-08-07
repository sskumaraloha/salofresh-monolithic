package com.salofresh.controller.membership;

import com.salofresh.constant.AppConstants;
import com.salofresh.dto.membership.MembershipPlanCreateRequest;
import com.salofresh.dto.membership.MembershipPlanResponse;
import com.salofresh.dto.membership.MembershipPlanUpdateRequest;
import com.salofresh.response.ApiResponse;
import com.salofresh.service.membership.MembershipPlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(AppConstants.API_BASE_PATH + "/salons/{salonId}/membership-plans")
@RequiredArgsConstructor
@Tag(name = "Membership Plans", description = "Salon-defined membership/subscription package management and discovery")
public class MembershipPlanController {

    private final MembershipPlanService membershipPlanService;

    @GetMapping
    @PreAuthorize("permitAll()")
    @Operation(summary = "List the active membership plans offered by a salon")
    public ApiResponse<List<MembershipPlanResponse>> list(@PathVariable Long salonId) {
        return ApiResponse.success("Membership plans fetched successfully", membershipPlanService.listActiveForSalon(salonId));
    }

    @PostMapping
    @PreAuthorize("hasRole('SALON_OWNER')")
    @Operation(summary = "Create a new membership plan for a salon (owner only)")
    public ApiResponse<MembershipPlanResponse> create(@PathVariable Long salonId,
                                                        @Valid @RequestBody MembershipPlanCreateRequest request) {
        return ApiResponse.success("Membership plan created successfully", membershipPlanService.create(salonId, request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SALON_OWNER')")
    @Operation(summary = "Update an existing membership plan (owner only)")
    public ApiResponse<MembershipPlanResponse> update(@PathVariable Long salonId, @PathVariable Long id,
                                                        @Valid @RequestBody MembershipPlanUpdateRequest request) {
        return ApiResponse.success("Membership plan updated successfully", membershipPlanService.update(salonId, id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SALON_OWNER')")
    @Operation(summary = "Deactivate/soft-delete a membership plan (owner only)")
    public ApiResponse<Void> delete(@PathVariable Long salonId, @PathVariable Long id) {
        membershipPlanService.delete(salonId, id);
        return ApiResponse.success("Membership plan deleted successfully");
    }
}
