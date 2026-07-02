package com.salofresh.controller.payment;

import com.salofresh.constant.AppConstants;
import com.salofresh.dto.payment.InitiatePaymentResponse;
import com.salofresh.dto.wallet.WalletResponse;
import com.salofresh.dto.wallet.WalletTopupRequest;
import com.salofresh.dto.wallet.WalletTransactionResponse;
import com.salofresh.entity.User;
import com.salofresh.entity.Wallet;
import com.salofresh.entity.WalletTransaction;
import com.salofresh.exception.ResourceNotFoundException;
import com.salofresh.repository.UserRepository;
import com.salofresh.response.ApiResponse;
import com.salofresh.response.PagedResponse;
import com.salofresh.security.SecurityUtils;
import com.salofresh.service.payment.PaymentService;
import com.salofresh.wallet.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(AppConstants.API_BASE_PATH + "/wallet")
@RequiredArgsConstructor
@Tag(name = "Wallet", description = "Customer wallet balance, transaction history and top-ups")
public class WalletController {

    private final WalletService walletService;
    private final PaymentService paymentService;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;

    @GetMapping("/balance")
    @Operation(summary = "Get the current user's wallet balance")
    public ApiResponse<WalletResponse> getBalance() {
        Wallet wallet = walletService.getOrCreateWallet(loadCurrentUser());
        return ApiResponse.success("Balance fetched", WalletResponse.builder()
                .balance(wallet.getBalance())
                .currency(wallet.getCurrency())
                .build());
    }

    @GetMapping("/history")
    @Operation(summary = "Paginated wallet transaction history for the current user")
    public ApiResponse<PagedResponse<WalletTransactionResponse>> getHistory(
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<WalletTransaction> transactions = walletService.getTransactionHistory(securityUtils.getCurrentUserId(), pageable);
        return ApiResponse.success("Wallet history fetched",
                PagedResponse.from(transactions, transactions.getContent().stream().map(this::toResponse).toList()));
    }

    @PostMapping("/topup")
    @Operation(summary = "Top up the wallet; CASH is settled immediately, gateway methods require a follow-up confirm call")
    public ApiResponse<InitiatePaymentResponse> topup(@Valid @RequestBody WalletTopupRequest request) {
        InitiatePaymentResponse response = paymentService.initiateWalletTopup(request, securityUtils.getCurrentUserId());
        return ApiResponse.success("Wallet top-up initiated", response);
    }

    private User loadCurrentUser() {
        Long userId = securityUtils.getCurrentUserId();
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }

    private WalletTransactionResponse toResponse(WalletTransaction transaction) {
        return WalletTransactionResponse.builder()
                .id(transaction.getId())
                .type(transaction.getType())
                .source(transaction.getSource())
                .amount(transaction.getAmount())
                .balanceAfter(transaction.getBalanceAfter())
                .referenceId(transaction.getReferenceId())
                .description(transaction.getDescription())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
