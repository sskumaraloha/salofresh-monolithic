package com.salofresh.controller.payment;

import com.salofresh.constant.AppConstants;
import com.salofresh.dto.payment.ConfirmPaymentRequest;
import com.salofresh.dto.payment.InitiatePaymentRequest;
import com.salofresh.dto.payment.InitiatePaymentResponse;
import com.salofresh.dto.payment.InvoiceResponse;
import com.salofresh.dto.payment.PaymentResponse;
import com.salofresh.dto.payment.RefundRequest;
import com.salofresh.dto.payment.RefundResponse;
import com.salofresh.response.ApiResponse;
import com.salofresh.response.PagedResponse;
import com.salofresh.security.SecurityUtils;
import com.salofresh.service.payment.PaymentService;
import com.salofresh.service.payment.RefundService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(AppConstants.API_BASE_PATH + "/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Appointment payment initiation, confirmation, invoices and refunds")
public class PaymentController {

    private final PaymentService paymentService;
    private final RefundService refundService;
    private final SecurityUtils securityUtils;

    @PostMapping("/{appointmentId}/initiate")
    @Operation(summary = "Initiate payment for an appointment's pending payment")
    public ApiResponse<InitiatePaymentResponse> initiate(@PathVariable Long appointmentId,
                                                           @Valid @RequestBody InitiatePaymentRequest request) {
        InitiatePaymentResponse response = paymentService.initiate(appointmentId, request, securityUtils.getCurrentUserId());
        return ApiResponse.success("Payment initiated", response);
    }

    @PostMapping("/{id}/confirm")
    @Operation(summary = "Confirm/capture a previously initiated payment")
    public ApiResponse<PaymentResponse> confirm(@PathVariable Long id, @Valid @RequestBody ConfirmPaymentRequest request) {
        PaymentResponse response = paymentService.confirm(id, request, securityUtils.getCurrentUserId());
        return ApiResponse.success("Payment confirmed", response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a payment by id")
    public ApiResponse<PaymentResponse> getById(@PathVariable Long id) {
        return ApiResponse.success("Payment fetched", paymentService.getById(id, securityUtils.getCurrentUserId()));
    }

    @GetMapping("/my")
    @Operation(summary = "List the current user's payments")
    public ApiResponse<PagedResponse<PaymentResponse>> listMine(
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ApiResponse.success("Payments fetched", paymentService.listForUser(securityUtils.getCurrentUserId(), pageable));
    }

    @GetMapping("/{id}/invoice")
    @Operation(summary = "Get a structured invoice for a successful payment")
    public ApiResponse<InvoiceResponse> getInvoice(@PathVariable Long id) {
        return ApiResponse.success("Invoice fetched", paymentService.getInvoice(id, securityUtils.getCurrentUserId()));
    }

    @PostMapping("/{id}/refund")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SALON_OWNER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Refund a payment (full or partial); restricted to the salon owner or an administrator")
    public ApiResponse<RefundResponse> refund(@PathVariable Long id, @Valid @RequestBody RefundRequest request) {
        RefundResponse response = refundService.initiateRefund(id, request, securityUtils.getCurrentUserId());
        return ApiResponse.success("Refund processed", response);
    }
}
