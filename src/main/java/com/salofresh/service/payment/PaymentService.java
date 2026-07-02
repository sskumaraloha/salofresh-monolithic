package com.salofresh.service.payment;

import com.salofresh.dto.payment.ConfirmPaymentRequest;
import com.salofresh.dto.payment.InitiatePaymentRequest;
import com.salofresh.dto.payment.InitiatePaymentResponse;
import com.salofresh.dto.payment.InvoiceResponse;
import com.salofresh.dto.payment.PaymentResponse;
import com.salofresh.dto.wallet.WalletTopupRequest;
import com.salofresh.response.PagedResponse;
import org.springframework.data.domain.Pageable;

public interface PaymentService {

    /**
     * Starts payment for an appointment's existing (booking-module-created) PENDING payment row.
     * For CASH the payment remains PENDING until {@link #confirm} is called. For WALLET the debit
     * happens synchronously and the payment is finalized immediately. For gateway methods a
     * gateway order is created and returned for the client to complete checkout.
     */
    InitiatePaymentResponse initiate(Long appointmentId, InitiatePaymentRequest request, Long currentUserId);

    /**
     * Creates a standalone (non-appointment) payment for topping up a user's wallet, following the
     * same method-specific initiation rules as {@link #initiate}.
     */
    InitiatePaymentResponse initiateWalletTopup(WalletTopupRequest request, Long currentUserId);

    /**
     * Confirms/captures a previously initiated payment: verifies the gateway signature for
     * card/UPI/Razorpay/Stripe payments, or marks a CASH payment as collected. WALLET payments are
     * finalized at initiation time and cannot be confirmed again.
     */
    PaymentResponse confirm(Long paymentId, ConfirmPaymentRequest request, Long currentUserId);

    PaymentResponse getById(Long paymentId, Long currentUserId);

    PagedResponse<PaymentResponse> listForUser(Long currentUserId, Pageable pageable);

    InvoiceResponse getInvoice(Long paymentId, Long currentUserId);
}
