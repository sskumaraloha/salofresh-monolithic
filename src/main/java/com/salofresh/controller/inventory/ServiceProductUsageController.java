package com.salofresh.controller.inventory;

import com.salofresh.constant.AppConstants;
import com.salofresh.dto.inventory.ServiceProductUsageRequest;
import com.salofresh.dto.inventory.ServiceProductUsageResponse;
import com.salofresh.response.ApiResponse;
import com.salofresh.service.inventory.ServiceProductUsageService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(AppConstants.API_BASE_PATH + "/salons/{salonId}/services/{serviceId}/product-usage")
@Tag(name = "Inventory - Service Product Usage", description = "Owner-managed links between a service and the products it consumes")
@PreAuthorize("hasRole('SALON_OWNER')")
public class ServiceProductUsageController {

    private final ServiceProductUsageService serviceProductUsageService;

    @GetMapping
    @Operation(summary = "List product usage links configured for a service")
    public ResponseEntity<ApiResponse<List<ServiceProductUsageResponse>>> list(
            @PathVariable Long salonId, @PathVariable Long serviceId) {
        return ResponseEntity.ok(ApiResponse.success("Product usage links fetched successfully",
                serviceProductUsageService.listForService(salonId, serviceId)));
    }

    @PostMapping
    @Operation(summary = "Link a product to a service with a per-service consumption quantity")
    public ResponseEntity<ApiResponse<ServiceProductUsageResponse>> link(
            @PathVariable Long salonId, @PathVariable Long serviceId,
            @Valid @RequestBody ServiceProductUsageRequest request) {
        ServiceProductUsageResponse response = serviceProductUsageService.link(salonId, serviceId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product linked to service successfully", response));
    }

    @DeleteMapping("/{usageId}")
    @Operation(summary = "Unlink a product from a service")
    public ResponseEntity<ApiResponse<Void>> unlink(
            @PathVariable Long salonId, @PathVariable Long serviceId, @PathVariable Long usageId) {
        serviceProductUsageService.unlink(salonId, serviceId, usageId);
        return ResponseEntity.ok(ApiResponse.success("Product unlinked from service successfully"));
    }
}
