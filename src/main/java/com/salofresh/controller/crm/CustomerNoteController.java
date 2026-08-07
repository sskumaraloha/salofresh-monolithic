package com.salofresh.controller.crm;

import com.salofresh.constant.AppConstants;
import com.salofresh.dto.crm.CustomerNoteRequest;
import com.salofresh.dto.crm.CustomerNoteResponse;
import com.salofresh.dto.crm.CustomerProfileSummaryResponse;
import com.salofresh.response.ApiResponse;
import com.salofresh.response.PagedResponse;
import com.salofresh.security.SecurityUtils;
import com.salofresh.service.crm.CustomerNoteService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Salon-staff-facing CRM notes on a customer, private to the salon and never
 * exposed to the customer themselves. Every endpoint is currently owner-only
 * (see {@link com.salofresh.service.impl.crm.CustomerNoteServiceImpl} for the
 * documented ownership model, including why employee read access is not
 * wired up over HTTP yet).
 */
@RestController
@RequestMapping(AppConstants.API_BASE_PATH + "/salons/{salonId}/customers/{customerId}")
@RequiredArgsConstructor
@Tag(name = "Customer CRM Notes", description = "Salon-staff-only CRM notes and customer 360 profile")
public class CustomerNoteController {

    private final CustomerNoteService customerNoteService;
    private final SecurityUtils securityUtils;

    @PostMapping("/notes")
    @PreAuthorize("hasRole('SALON_OWNER')")
    @Operation(summary = "Add a CRM note for a customer of this salon")
    public ResponseEntity<ApiResponse<CustomerNoteResponse>> create(
            @PathVariable Long salonId,
            @PathVariable Long customerId,
            @Valid @RequestBody CustomerNoteRequest request) {
        CustomerNoteResponse response = customerNoteService.create(
                securityUtils.getCurrentUserId(), salonId, customerId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Customer note created successfully", response));
    }

    @GetMapping("/notes")
    @PreAuthorize("hasRole('SALON_OWNER')")
    @Operation(summary = "List CRM notes for a customer of this salon, most recent first")
    public ResponseEntity<ApiResponse<PagedResponse<CustomerNoteResponse>>> list(
            @PathVariable Long salonId,
            @PathVariable Long customerId,
            @PageableDefault(size = 20) Pageable pageable) {
        PagedResponse<CustomerNoteResponse> response = customerNoteService.listForCustomer(
                securityUtils.getCurrentUserId(), salonId, customerId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Customer notes fetched successfully", response));
    }

    @PutMapping("/notes/{noteId}")
    @PreAuthorize("hasRole('SALON_OWNER')")
    @Operation(summary = "Update a CRM note (original author or salon owner only)")
    public ResponseEntity<ApiResponse<CustomerNoteResponse>> update(
            @PathVariable Long salonId,
            @PathVariable Long customerId,
            @PathVariable Long noteId,
            @Valid @RequestBody CustomerNoteRequest request) {
        CustomerNoteResponse response = customerNoteService.update(
                securityUtils.getCurrentUserId(), salonId, customerId, noteId, request);
        return ResponseEntity.ok(ApiResponse.success("Customer note updated successfully", response));
    }

    @DeleteMapping("/notes/{noteId}")
    @PreAuthorize("hasRole('SALON_OWNER')")
    @Operation(summary = "Soft delete a CRM note (salon owner only)")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long salonId,
            @PathVariable Long customerId,
            @PathVariable Long noteId) {
        customerNoteService.delete(securityUtils.getCurrentUserId(), salonId, customerId, noteId);
        return ResponseEntity.ok(ApiResponse.success("Customer note deleted successfully"));
    }

    @GetMapping("/profile")
    @PreAuthorize("hasRole('SALON_OWNER')")
    @Operation(summary = "Get the aggregated CRM profile (customer 360) for a customer at this salon")
    public ResponseEntity<ApiResponse<CustomerProfileSummaryResponse>> getProfile(
            @PathVariable Long salonId,
            @PathVariable Long customerId) {
        CustomerProfileSummaryResponse response = customerNoteService.getCustomerProfileSummary(
                securityUtils.getCurrentUserId(), salonId, customerId);
        return ResponseEntity.ok(ApiResponse.success("Customer profile fetched successfully", response));
    }
}
