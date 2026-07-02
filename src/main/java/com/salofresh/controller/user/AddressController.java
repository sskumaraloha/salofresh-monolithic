package com.salofresh.controller.user;

import com.salofresh.constant.AppConstants;
import com.salofresh.dto.user.AddressRequest;
import com.salofresh.dto.user.AddressResponse;
import com.salofresh.response.ApiResponse;
import com.salofresh.security.SecurityUtils;
import com.salofresh.service.user.AddressService;
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
@RequestMapping(AppConstants.API_BASE_PATH + "/users/me/addresses")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "User Addresses", description = "Manage the authenticated user's saved addresses")
public class AddressController {

    private final AddressService addressService;
    private final SecurityUtils securityUtils;

    @GetMapping
    @Operation(summary = "List the current user's addresses")
    public ResponseEntity<ApiResponse<List<AddressResponse>>> listAddresses() {
        List<AddressResponse> response = addressService.listAddresses(securityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success("Addresses retrieved successfully", response));
    }

    @PostMapping
    @Operation(summary = "Add a new address for the current user")
    public ResponseEntity<ApiResponse<AddressResponse>> createAddress(@Valid @RequestBody AddressRequest request) {
        AddressResponse response = addressService.createAddress(securityUtils.getCurrentUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Address created successfully", response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing address owned by the current user")
    public ResponseEntity<ApiResponse<AddressResponse>> updateAddress(@PathVariable Long id,
                                                                       @Valid @RequestBody AddressRequest request) {
        AddressResponse response = addressService.updateAddress(securityUtils.getCurrentUserId(), id, request);
        return ResponseEntity.ok(ApiResponse.success("Address updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an address owned by the current user")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(@PathVariable Long id) {
        addressService.deleteAddress(securityUtils.getCurrentUserId(), id);
        return ResponseEntity.ok(ApiResponse.success("Address deleted successfully"));
    }

    @PutMapping("/{id}/default")
    @Operation(summary = "Mark an address as the default address for the current user")
    public ResponseEntity<ApiResponse<AddressResponse>> setDefault(@PathVariable Long id) {
        AddressResponse response = addressService.setDefault(securityUtils.getCurrentUserId(), id);
        return ResponseEntity.ok(ApiResponse.success("Default address updated successfully", response));
    }
}
