package com.salofresh.service.payment;

import com.salofresh.dto.payment.RefundRequest;
import com.salofresh.dto.payment.RefundResponse;

import java.util.List;

public interface RefundService {

    RefundResponse initiateRefund(Long paymentId, RefundRequest request, Long currentUserId);

    List<RefundResponse> listForPayment(Long paymentId, Long currentUserId);
}
