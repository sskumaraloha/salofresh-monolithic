package com.salofresh.controller.salon;

import com.salofresh.constant.AppConstants;
import com.salofresh.dto.salon.WorkingHoursRequest;
import com.salofresh.dto.salon.WorkingHoursResponse;
import com.salofresh.response.ApiResponse;
import com.salofresh.service.salon.WorkingHoursService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping(AppConstants.API_BASE_PATH + "/salons/{salonId}/working-hours")
@Tag(name = "Salon Working Hours", description = "Owner-managed weekly working hours for a salon")
@PreAuthorize("hasRole('SALON_OWNER')")
public class WorkingHoursController {

    private final WorkingHoursService workingHoursService;

    @GetMapping
    @Operation(summary = "List the weekly working-hours schedule of a salon")
    public ResponseEntity<ApiResponse<List<WorkingHoursResponse>>> list(@PathVariable Long salonId) {
        return ResponseEntity.ok(ApiResponse.success("Working hours fetched successfully", workingHoursService.list(salonId)));
    }

    @PutMapping
    @Operation(summary = "Replace the full weekly working-hours schedule of a salon")
    public ResponseEntity<ApiResponse<List<WorkingHoursResponse>>> bulkUpsert(
            @PathVariable Long salonId,
            @Valid @NotEmpty @RequestBody List<WorkingHoursRequest> requests) {
        return ResponseEntity.ok(ApiResponse.success("Working hours updated successfully",
                workingHoursService.bulkUpsert(salonId, requests)));
    }
}
