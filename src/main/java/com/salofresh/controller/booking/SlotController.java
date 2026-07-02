package com.salofresh.controller.booking;

import com.salofresh.constant.AppConstants;
import com.salofresh.dto.booking.AvailableSlotResponse;
import com.salofresh.dto.booking.SlotQueryRequest;
import com.salofresh.response.ApiResponse;
import com.salofresh.service.booking.SlotAvailabilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(AppConstants.API_BASE_PATH + "/salons/{salonId}/slots")
@RequiredArgsConstructor
@Tag(name = "Slots", description = "Public salon availability browsing")
public class SlotController {

    private final SlotAvailabilityService slotAvailabilityService;

    @GetMapping
    @PreAuthorize("permitAll()")
    @Operation(summary = "Get available booking slots for a salon on a given date")
    public ResponseEntity<ApiResponse<List<AvailableSlotResponse>>> getAvailableSlots(
            @PathVariable Long salonId, @Valid @ModelAttribute SlotQueryRequest query) {
        List<AvailableSlotResponse> slots = slotAvailabilityService.getAvailableSlots(
                salonId, query.getDate(), query.getEmployeeId(), query.getServiceIds());
        return ResponseEntity.ok(ApiResponse.success("Available slots fetched successfully", slots));
    }
}
