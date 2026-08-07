package com.salofresh.controller.waitlist;

import com.salofresh.common.enums.WaitlistStatus;
import com.salofresh.constant.AppConstants;
import com.salofresh.dto.waitlist.JoinWaitlistRequest;
import com.salofresh.dto.waitlist.WaitlistResponse;
import com.salofresh.response.ApiResponse;
import com.salofresh.response.PagedResponse;
import com.salofresh.security.SecurityUtils;
import com.salofresh.service.waitlist.WaitlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Waitlist", description = "Notify-me / waitlist for fully booked (or preferred) salon slots")
public class WaitlistController {

    private final WaitlistService waitlistService;
    private final SecurityUtils securityUtils;

    @PostMapping(AppConstants.API_BASE_PATH + "/waitlist")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Join the waitlist for a salon/employee/date")
    public ResponseEntity<ApiResponse<WaitlistResponse>> join(@Valid @RequestBody JoinWaitlistRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        WaitlistResponse response = waitlistService.join(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("You have been added to the waitlist", response));
    }

    @DeleteMapping(AppConstants.API_BASE_PATH + "/waitlist/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Cancel the current user's own waitlist entry")
    public ResponseEntity<ApiResponse<Void>> cancel(@PathVariable Long id) {
        Long userId = securityUtils.getCurrentUserId();
        waitlistService.cancel(userId, id);
        return ResponseEntity.ok(ApiResponse.success("Waitlist entry cancelled successfully"));
    }

    @GetMapping(AppConstants.API_BASE_PATH + "/waitlist/my")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List the current user's own waitlist entries")
    public ResponseEntity<ApiResponse<PagedResponse<WaitlistResponse>>> myWaitlist(
            @PageableDefault(size = 20) Pageable pageable) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Waitlist entries fetched successfully",
                waitlistService.listForUser(userId, pageable)));
    }

    @GetMapping(AppConstants.API_BASE_PATH + "/salons/{salonId}/waitlist")
    @PreAuthorize("hasRole('SALON_OWNER')")
    @Operation(summary = "List waitlist demand for a salon (salon owner only)")
    public ResponseEntity<ApiResponse<PagedResponse<WaitlistResponse>>> listForSalon(
            @PathVariable Long salonId,
            @RequestParam(required = false) WaitlistStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        Long ownerUserId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Salon waitlist fetched successfully",
                waitlistService.listForSalon(ownerUserId, salonId, status, pageable)));
    }
}
