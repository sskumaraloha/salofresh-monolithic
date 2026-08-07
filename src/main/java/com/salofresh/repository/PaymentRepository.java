package com.salofresh.repository;

import com.salofresh.common.enums.PaymentStatus;
import com.salofresh.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByAppointmentId(Long appointmentId);

    Optional<Payment> findByGatewayOrderId(String gatewayOrderId);

    Optional<Payment> findByInvoiceNumber(String invoiceNumber);

    Page<Payment> findAllByUserId(Long userId, Pageable pageable);

    // Added for fraud/anomaly detection: fetch recent payments of a given status to group by
    // user in memory when looking for repeated-failure patterns.
    List<Payment> findAllByPaymentStatusAndCreatedAtAfter(PaymentStatus paymentStatus, Instant after);
}
