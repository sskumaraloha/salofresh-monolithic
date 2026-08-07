package com.salofresh.controller.payroll;

import com.salofresh.constant.AppConstants;
import com.salofresh.dto.payroll.CommissionRuleRequest;
import com.salofresh.dto.payroll.CommissionRuleResponse;
import com.salofresh.response.ApiResponse;
import com.salofresh.security.SecurityUtils;
import com.salofresh.service.payroll.CommissionRuleService;
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
@RequestMapping(AppConstants.API_BASE_PATH + "/salons/{salonId}/commission-rules")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SALON_OWNER')")
@Tag(name = "Commission Rules", description = "Staff commission rule management (owner-only)")
public class CommissionRuleController {

    private final CommissionRuleService commissionRuleService;
    private final SecurityUtils securityUtils;

    @PostMapping
    @Operation(summary = "Create a commission rule for a salon (optionally scoped to an employee and/or category)")
    public ResponseEntity<ApiResponse<CommissionRuleResponse>> create(
            @PathVariable Long salonId,
            @Valid @RequestBody CommissionRuleRequest request) {
        CommissionRuleResponse response = commissionRuleService.create(
                securityUtils.getCurrentUserId(), salonId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Commission rule created successfully", response));
    }

    @PutMapping("/{ruleId}")
    @Operation(summary = "Update an existing commission rule")
    public ResponseEntity<ApiResponse<CommissionRuleResponse>> update(
            @PathVariable Long salonId,
            @PathVariable Long ruleId,
            @Valid @RequestBody CommissionRuleRequest request) {
        CommissionRuleResponse response = commissionRuleService.update(
                securityUtils.getCurrentUserId(), salonId, ruleId, request);
        return ResponseEntity.ok(ApiResponse.success("Commission rule updated successfully", response));
    }

    @DeleteMapping("/{ruleId}")
    @Operation(summary = "Delete a commission rule")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long salonId, @PathVariable Long ruleId) {
        commissionRuleService.delete(securityUtils.getCurrentUserId(), salonId, ruleId);
        return ResponseEntity.ok(ApiResponse.success("Commission rule deleted successfully"));
    }

    @GetMapping("/{ruleId}")
    @Operation(summary = "Get a commission rule by id")
    public ResponseEntity<ApiResponse<CommissionRuleResponse>> getById(
            @PathVariable Long salonId, @PathVariable Long ruleId) {
        CommissionRuleResponse response = commissionRuleService.getById(
                securityUtils.getCurrentUserId(), salonId, ruleId);
        return ResponseEntity.ok(ApiResponse.success("Commission rule fetched successfully", response));
    }

    @GetMapping
    @Operation(summary = "List all commission rules for a salon")
    public ResponseEntity<ApiResponse<List<CommissionRuleResponse>>> list(@PathVariable Long salonId) {
        List<CommissionRuleResponse> response = commissionRuleService.listForSalon(
                securityUtils.getCurrentUserId(), salonId);
        return ResponseEntity.ok(ApiResponse.success("Commission rules fetched successfully", response));
    }
}
