package com.salofresh.controller.salon;

import com.salofresh.constant.AppConstants;
import com.salofresh.dto.salon.SalonCreateRequest;
import com.salofresh.dto.salon.SalonResponse;
import com.salofresh.dto.salon.SalonSearchRequest;
import com.salofresh.dto.salon.SalonSummaryResponse;
import com.salofresh.dto.salon.SalonUpdateRequest;
import com.salofresh.response.ApiResponse;
import com.salofresh.response.PagedResponse;
import com.salofresh.service.salon.SalonService;
import com.salofresh.util.GeoUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Tag(name = "Salons", description = "Public salon discovery and salon-owner self-service management")
public class SalonController {

    private final SalonService salonService;

    @GetMapping(AppConstants.API_BASE_PATH + "/salons")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Search publicly visible, approved and active salons")
    public ResponseEntity<ApiResponse<PagedResponse<SalonSummaryResponse>>> search(@ModelAttribute SalonSearchRequest request) {
        Page<SalonSummaryResponse> page = salonService.search(request);
        return ResponseEntity.ok(ApiResponse.success("Salons fetched successfully", PagedResponse.from(page)));
    }

    @GetMapping(AppConstants.API_BASE_PATH + "/salons/{slug}")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Get full public detail of a salon by its slug")
    public ResponseEntity<ApiResponse<SalonResponse>> getBySlug(@PathVariable String slug,
                                                                 @RequestParam(required = false) Double latitude,
                                                                 @RequestParam(required = false) Double longitude) {
        SalonResponse response = salonService.getBySlug(slug);
        if (latitude != null && longitude != null && response.getLatitude() != null && response.getLongitude() != null) {
            double distanceKm = GeoUtils.distanceInKm(latitude, longitude, response.getLatitude(), response.getLongitude());
            response = response.toBuilder().distanceKm(distanceKm).build();
        }
        return ResponseEntity.ok(ApiResponse.success("Salon fetched successfully", response));
    }

    @PostMapping(AppConstants.API_BASE_PATH + "/salons")
    @PreAuthorize("hasRole('SALON_OWNER')")
    @Operation(summary = "Create a new salon for the authenticated salon owner")
    public ResponseEntity<ApiResponse<SalonResponse>> create(@Valid @RequestBody SalonCreateRequest request) {
        SalonResponse response = salonService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Salon created successfully. It is pending admin verification.", response));
    }

    @PutMapping(AppConstants.API_BASE_PATH + "/salons/{id}")
    @PreAuthorize("hasRole('SALON_OWNER')")
    @Operation(summary = "Update a salon owned by the authenticated salon owner")
    public ResponseEntity<ApiResponse<SalonResponse>> update(@PathVariable Long id,
                                                              @Valid @RequestBody SalonUpdateRequest request) {
        SalonResponse response = salonService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success("Salon updated successfully", response));
    }

    @DeleteMapping(AppConstants.API_BASE_PATH + "/salons/{id}")
    @PreAuthorize("hasRole('SALON_OWNER')")
    @Operation(summary = "Soft-delete a salon owned by the authenticated salon owner")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        salonService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Salon deleted successfully"));
    }

    @PutMapping(AppConstants.API_BASE_PATH + "/salons/{id}/pause")
    @PreAuthorize("hasRole('SALON_OWNER')")
    @Operation(summary = "Pause a salon, temporarily hiding it from public search/booking")
    public ResponseEntity<ApiResponse<SalonResponse>> pause(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Salon paused successfully", salonService.pause(id)));
    }

    @PutMapping(AppConstants.API_BASE_PATH + "/salons/{id}/activate")
    @PreAuthorize("hasRole('SALON_OWNER')")
    @Operation(summary = "Re-activate a paused salon")
    public ResponseEntity<ApiResponse<SalonResponse>> activate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Salon activated successfully", salonService.activate(id)));
    }

    @PutMapping(AppConstants.API_BASE_PATH + "/salons/{id}/banner")
    @PreAuthorize("hasRole('SALON_OWNER')")
    @Operation(summary = "Upload/replace the banner image of a salon")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadBanner(@PathVariable Long id,
                                                                          @RequestPart("file") MultipartFile file) {
        String bannerUrl = salonService.uploadBanner(id, file);
        return ResponseEntity.ok(ApiResponse.success("Banner uploaded successfully", Map.of("bannerImageUrl", bannerUrl)));
    }

    @GetMapping(AppConstants.API_BASE_PATH + "/owners/me/salons")
    @PreAuthorize("hasRole('SALON_OWNER')")
    @Operation(summary = "List all salons owned by the authenticated salon owner")
    public ResponseEntity<ApiResponse<List<SalonSummaryResponse>>> listMySalons() {
        return ResponseEntity.ok(ApiResponse.success("Salons fetched successfully", salonService.listMySalons()));
    }
}
