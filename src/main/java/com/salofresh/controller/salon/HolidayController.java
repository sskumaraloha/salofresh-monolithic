package com.salofresh.controller.salon;

import com.salofresh.constant.AppConstants;
import com.salofresh.dto.salon.HolidayRequest;
import com.salofresh.dto.salon.HolidayResponse;
import com.salofresh.response.ApiResponse;
import com.salofresh.service.salon.HolidayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@RequiredArgsConstructor
@RequestMapping(AppConstants.API_BASE_PATH + "/salons/{salonId}/holidays")
@Tag(name = "Salon Holidays", description = "Owner-managed holiday calendar for a salon")
@PreAuthorize("hasRole('SALON_OWNER')")
public class HolidayController {

    private final HolidayService holidayService;

    @GetMapping
    @Operation(summary = "List holidays configured for a salon")
    public ResponseEntity<ApiResponse<List<HolidayResponse>>> list(@PathVariable Long salonId) {
        return ResponseEntity.ok(ApiResponse.success("Holidays fetched successfully", holidayService.list(salonId)));
    }

    @PostMapping
    @Operation(summary = "Add a holiday to a salon's calendar")
    public ResponseEntity<ApiResponse<HolidayResponse>> create(@PathVariable Long salonId,
                                                                @Valid @RequestBody HolidayRequest request) {
        HolidayResponse response = holidayService.create(salonId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Holiday added successfully", response));
    }

    @PutMapping("/{holidayId}")
    @Operation(summary = "Update a holiday entry")
    public ResponseEntity<ApiResponse<HolidayResponse>> update(@PathVariable Long salonId,
                                                                @PathVariable Long holidayId,
                                                                @Valid @RequestBody HolidayRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Holiday updated successfully",
                holidayService.update(salonId, holidayId, request)));
    }

    @DeleteMapping("/{holidayId}")
    @Operation(summary = "Remove a holiday entry")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long salonId, @PathVariable Long holidayId) {
        holidayService.delete(salonId, holidayId);
        return ResponseEntity.ok(ApiResponse.success("Holiday removed successfully"));
    }
}
