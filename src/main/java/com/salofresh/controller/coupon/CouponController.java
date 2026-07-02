package com.salofresh.controller.coupon;

import com.salofresh.constant.AppConstants;
import com.salofresh.dto.coupon.CouponCreateRequest;
import com.salofresh.dto.coupon.CouponResponse;
import com.salofresh.dto.coupon.CouponUpdateRequest;
import com.salofresh.dto.coupon.ValidateCouponRequest;
import com.salofresh.dto.coupon.ValidateCouponResponse;
import com.salofresh.response.ApiResponse;
import com.salofresh.response.PagedResponse;
import com.salofresh.security.SecurityUtils;
import com.salofresh.service.coupon.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(AppConstants.API_BASE_PATH + "/coupons")
@RequiredArgsConstructor
@Tag(name = "Coupons", description = "Admin coupon management and customer coupon preview/validation")
public class CouponController {

    private final CouponService couponService;
    private final SecurityUtils securityUtils;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Create a new coupon")
    public ApiResponse<CouponResponse> create(@Valid @RequestBody CouponCreateRequest request) {
        return ApiResponse.success("Coupon created", couponService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Update an existing coupon")
    public ApiResponse<CouponResponse> update(@PathVariable Long id, @Valid @RequestBody CouponUpdateRequest request) {
        return ApiResponse.success("Coupon updated", couponService.update(id, request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Get a coupon by id")
    public ApiResponse<CouponResponse> getById(@PathVariable Long id) {
        return ApiResponse.success("Coupon fetched", couponService.getById(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "List coupons")
    public ApiResponse<PagedResponse<CouponResponse>> list(
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size,
            @RequestParam(defaultValue = "false") boolean activeOnly) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ApiResponse.success("Coupons fetched", couponService.list(pageable, activeOnly));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Deactivate a coupon")
    public ApiResponse<Void> deactivate(@PathVariable Long id) {
        couponService.deactivate(id);
        return ApiResponse.success("Coupon deactivated");
    }

    @PostMapping("/validate")
    @Operation(summary = "Preview a coupon's discount before booking; returns valid=false with a friendly message instead of an error")
    public ApiResponse<ValidateCouponResponse> validate(@Valid @RequestBody ValidateCouponRequest request) {
        ValidateCouponResponse response = couponService.validate(request, securityUtils.getCurrentUserId());
        return ApiResponse.success("Coupon validated", response);
    }
}
