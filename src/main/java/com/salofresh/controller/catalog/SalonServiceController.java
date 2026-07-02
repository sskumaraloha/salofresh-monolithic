package com.salofresh.controller.catalog;

import com.salofresh.constant.AppConstants;
import com.salofresh.dto.catalog.SalonServiceCreateRequest;
import com.salofresh.dto.catalog.SalonServiceResponse;
import com.salofresh.dto.catalog.SalonServiceUpdateRequest;
import com.salofresh.response.ApiResponse;
import com.salofresh.service.catalog.SalonServiceCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping(AppConstants.API_BASE_PATH + "/salons/{salonId}/services")
@RequiredArgsConstructor
@Tag(name = "Salon Services", description = "Management and discovery of services offered by a salon")
public class SalonServiceController {

    private final SalonServiceCatalogService salonServiceCatalogService;

    @GetMapping
    @PreAuthorize("permitAll()")
    @Operation(summary = "List services for a salon",
            description = "Public callers only see ACTIVE services; pass managementView=true (owner only) to see all")
    public ResponseEntity<ApiResponse<List<SalonServiceResponse>>> list(
            @PathVariable Long salonId,
            @RequestParam(name = "managementView", defaultValue = "false") boolean managementView) {
        return ResponseEntity.ok(ApiResponse.success("Services fetched successfully",
                salonServiceCatalogService.listBySalon(salonId, managementView)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Get a single service by id")
    public ResponseEntity<ApiResponse<SalonServiceResponse>> getById(@PathVariable Long salonId, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Service fetched successfully",
                salonServiceCatalogService.getById(salonId, id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('SALON_OWNER')")
    @Operation(summary = "Create a new service for a salon (owner only)")
    public ResponseEntity<ApiResponse<SalonServiceResponse>> create(
            @PathVariable Long salonId, @Valid @RequestBody SalonServiceCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Service created successfully",
                salonServiceCatalogService.create(salonId, request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SALON_OWNER')")
    @Operation(summary = "Update an existing service (owner only)")
    public ResponseEntity<ApiResponse<SalonServiceResponse>> update(
            @PathVariable Long salonId, @PathVariable Long id, @Valid @RequestBody SalonServiceUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Service updated successfully",
                salonServiceCatalogService.update(salonId, id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SALON_OWNER')")
    @Operation(summary = "Soft-delete a service (owner only)")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long salonId, @PathVariable Long id) {
        salonServiceCatalogService.delete(salonId, id);
        return ResponseEntity.ok(ApiResponse.success("Service deleted successfully"));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('SALON_OWNER')")
    @Operation(summary = "Toggle a service's ACTIVE/INACTIVE status (owner only)")
    public ResponseEntity<ApiResponse<SalonServiceResponse>> toggleStatus(@PathVariable Long salonId, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Service status updated successfully",
                salonServiceCatalogService.toggleStatus(salonId, id)));
    }

    @PostMapping("/{id}/image")
    @PreAuthorize("hasRole('SALON_OWNER')")
    @Operation(summary = "Upload/replace a service's image (owner only)")
    public ResponseEntity<ApiResponse<SalonServiceResponse>> uploadImage(
            @PathVariable Long salonId, @PathVariable Long id, @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success("Service image uploaded successfully",
                salonServiceCatalogService.uploadImage(salonId, id, file)));
    }
}
