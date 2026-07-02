package com.salofresh.service.payment;

import com.salofresh.common.enums.PaymentMethod;
import com.salofresh.common.enums.PaymentStatus;
import com.salofresh.common.enums.WalletTransactionSource;
import com.salofresh.dto.payment.InitiatePaymentRequest;
import com.salofresh.dto.payment.InitiatePaymentResponse;
import com.salofresh.entity.Payment;
import com.salofresh.entity.User;
import com.salofresh.exception.BadRequestException;
import com.salofresh.exception.ForbiddenException;
import com.salofresh.mapper.payment.PaymentMapper;
import com.salofresh.repository.AppointmentRepository;
import com.salofresh.repository.AppointmentServiceRepository;
import com.salofresh.repository.PaymentRepository;
import com.salofresh.repository.TransactionRepository;
import com.salofresh.repository.UserRepository;
import com.salofresh.security.SecurityUtils;
import com.salofresh.service.impl.payment.PaymentGatewayFactory;
import com.salofresh.service.impl.payment.PaymentServiceImpl;
import com.salofresh.wallet.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private AppointmentServiceRepository appointmentServiceRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PaymentMapper paymentMapper;
    @Mock
    private PaymentGatewayFactory gatewayFactory;
    @Mock
    private WalletService walletService;
    @Mock
    private SecurityUtils securityUtils;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentServiceImpl(paymentRepository, transactionRepository, appointmentRepository,
                appointmentServiceRepository, userRepository, paymentMapper, gatewayFactory, walletService,
                securityUtils, eventPublisher);
    }

    private User buildUser(long id) {
        return User.builder().id(id).email("payer@example.com").firstName("Pay").lastName("Er").build();
    }

    private Payment buildPendingPayment(User user) {
        return Payment.builder()
                .id(500L)
                .user(user)
                .amount(new BigDecimal("499.00"))
                .currency("INR")
                .paymentStatus(PaymentStatus.PENDING)
                .build();
    }

    @Test
    void initiate_withCashMethod_leavesPaymentPending() {
        User user = buildUser(1L);
        Payment payment = buildPendingPayment(user);
        InitiatePaymentRequest request = InitiatePaymentRequest.builder().paymentMethod(PaymentMethod.CASH).build();

        when(paymentRepository.findByAppointmentId(77L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        InitiatePaymentResponse response = paymentService.initiate(77L, request, 1L);

        assertThat(response.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        verify(walletService, never()).debit(any(), any(), any(), anyString(), anyString());
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    void initiate_withWalletMethodAndSufficientBalance_marksPaymentSuccessAndPublishesEvent() {
        User user = buildUser(2L);
        Payment payment = buildPendingPayment(user);
        InitiatePaymentRequest request = InitiatePaymentRequest.builder().paymentMethod(PaymentMethod.WALLET).build();

        when(paymentRepository.findByAppointmentId(80L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        InitiatePaymentResponse response = paymentService.initiate(80L, request, 2L);

        assertThat(response.getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(payment.getPaidAt()).isNotNull();
        assertThat(payment.getInvoiceNumber()).isNotBlank();
        verify(walletService).debit(eq(user), eq(payment.getAmount()), eq(WalletTransactionSource.BOOKING_PAYMENT),
                anyString(), anyString());
        verify(transactionRepository).save(any());
        verify(eventPublisher).publishEvent(any(Object.class));
    }

    @Test
    void initiate_withWalletMethodAndInsufficientBalance_marksPaymentFailed() {
        User user = buildUser(3L);
        Payment payment = buildPendingPayment(user);
        InitiatePaymentRequest request = InitiatePaymentRequest.builder().paymentMethod(PaymentMethod.WALLET).build();

        when(paymentRepository.findByAppointmentId(90L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        org.mockito.Mockito.doThrow(new BadRequestException("Insufficient wallet balance"))
                .when(walletService).debit(any(), any(), any(), anyString(), anyString());

        InitiatePaymentResponse response = paymentService.initiate(90L, request, 3L);

        assertThat(response.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getFailureReason()).isEqualTo("Insufficient wallet balance");
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void initiate_whenCallerIsNotThePayer_throwsForbidden() {
        User payer = buildUser(4L);
        Payment payment = buildPendingPayment(payer);
        InitiatePaymentRequest request = InitiatePaymentRequest.builder().paymentMethod(PaymentMethod.CASH).build();

        when(paymentRepository.findByAppointmentId(95L)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.initiate(95L, request, 999L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void initiate_whenPaymentAlreadySucceeded_throwsBadRequest() {
        User user = buildUser(5L);
        Payment payment = buildPendingPayment(user);
        payment.setPaymentStatus(PaymentStatus.SUCCESS);
        InitiatePaymentRequest request = InitiatePaymentRequest.builder().paymentMethod(PaymentMethod.CASH).build();

        when(paymentRepository.findByAppointmentId(96L)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.initiate(96L, request, 5L))
                .isInstanceOf(BadRequestException.class);
    }
}
