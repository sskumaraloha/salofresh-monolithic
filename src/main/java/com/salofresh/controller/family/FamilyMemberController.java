package com.salofresh.controller.family;

import com.salofresh.constant.AppConstants;
import com.salofresh.dto.family.FamilyMemberRequest;
import com.salofresh.dto.family.FamilyMemberResponse;
import com.salofresh.response.ApiResponse;
import com.salofresh.security.SecurityUtils;
import com.salofresh.service.family.FamilyMemberService;
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
@RequestMapping(AppConstants.API_BASE_PATH + "/users/me/family-members")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Family Members", description = "Manage the authenticated user's family/dependent profiles for booking on their behalf")
public class FamilyMemberController {

    private final FamilyMemberService familyMemberService;
    private final SecurityUtils securityUtils;

    @GetMapping
    @Operation(summary = "List the current user's family members")
    public ResponseEntity<ApiResponse<List<FamilyMemberResponse>>> listFamilyMembers() {
        List<FamilyMemberResponse> response = familyMemberService.listFamilyMembers(securityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success("Family members retrieved successfully", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a family member owned by the current user")
    public ResponseEntity<ApiResponse<FamilyMemberResponse>> getFamilyMember(@PathVariable Long id) {
        FamilyMemberResponse response = familyMemberService.getFamilyMember(securityUtils.getCurrentUserId(), id);
        return ResponseEntity.ok(ApiResponse.success("Family member retrieved successfully", response));
    }

    @PostMapping
    @Operation(summary = "Add a new family member for the current user")
    public ResponseEntity<ApiResponse<FamilyMemberResponse>> createFamilyMember(
            @Valid @RequestBody FamilyMemberRequest request) {
        FamilyMemberResponse response = familyMemberService.createFamilyMember(securityUtils.getCurrentUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Family member created successfully", response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a family member owned by the current user")
    public ResponseEntity<ApiResponse<FamilyMemberResponse>> updateFamilyMember(
            @PathVariable Long id, @Valid @RequestBody FamilyMemberRequest request) {
        FamilyMemberResponse response =
                familyMemberService.updateFamilyMember(securityUtils.getCurrentUserId(), id, request);
        return ResponseEntity.ok(ApiResponse.success("Family member updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove a family member owned by the current user")
    public ResponseEntity<ApiResponse<Void>> deleteFamilyMember(@PathVariable Long id) {
        familyMemberService.deleteFamilyMember(securityUtils.getCurrentUserId(), id);
        return ResponseEntity.ok(ApiResponse.success("Family member removed successfully"));
    }
}
