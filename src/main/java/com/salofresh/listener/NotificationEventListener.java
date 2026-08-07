package com.salofresh.listener;

import com.salofresh.common.enums.NotificationChannel;
import com.salofresh.common.enums.NotificationType;
import com.salofresh.email.EmailService;
import com.salofresh.email.EmailTemplateBuilder;
import com.salofresh.entity.Appointment;
import com.salofresh.entity.Payment;
import com.salofresh.entity.User;
import com.salofresh.event.AccountLockedEvent;
import com.salofresh.event.BookingCancelledEvent;
import com.salofresh.event.BookingCompletedEvent;
import com.salofresh.event.BookingCreatedEvent;
import com.salofresh.event.BookingModifiedEvent;
import com.salofresh.event.BookingRejectedEvent;
import com.salofresh.event.BookingReminderEvent;
import com.salofresh.event.NewReviewEvent;
import com.salofresh.event.OtpGeneratedEvent;
import com.salofresh.event.PaymentFailedEvent;
import com.salofresh.event.PaymentSuccessEvent;
import com.salofresh.event.RefundProcessedEvent;
import com.salofresh.event.RewardPointsEarnedEvent;
import com.salofresh.event.ReviewReplyEvent;
import com.salofresh.event.SalonApprovedEvent;
import com.salofresh.event.SalonRejectedEvent;
import com.salofresh.event.SalonSuspendedEvent;
import com.salofresh.event.UserRegisteredEvent;
import com.salofresh.event.WalletTransactionEvent;
import com.salofresh.notification.NotificationService;
import com.salofresh.push.PushNotificationService;
import com.salofresh.sms.SmsService;
import com.salofresh.whatsapp.WhatsAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final EmailService emailService;
    private final SmsService smsService;
    private final PushNotificationService pushNotificationService;
    private final WhatsAppService whatsAppService;
    private final EmailTemplateBuilder emailTemplateBuilder;

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onUserRegistered(UserRegisteredEvent event) {
        User user = event.user();
        String message = "Welcome to SaloFresh, %s! Your account has been created successfully.".formatted(user.getFirstName());
        notificationService.createAndDispatch(user, NotificationType.GENERIC, NotificationChannel.EMAIL,
                "Welcome to SaloFresh", message, user.getId().toString(), "USER");
        emailService.sendHtmlEmail(user.getEmail(), "Welcome to SaloFresh",
                emailTemplateBuilder.buildGenericNotificationEmail(user.getFirstName(), "Welcome to SaloFresh!", message));
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onOtpGenerated(OtpGeneratedEvent event) {
        if (event.otpChannel() == com.salofresh.common.enums.OtpChannel.SMS) {
            smsService.sendSms(event.identifier(), "Your SaloFresh OTP is %s. It expires in 10 minutes."
                    .formatted(event.otpCode()));
        } else {
            emailService.sendHtmlEmail(event.identifier(), "Your SaloFresh Verification Code",
                    emailTemplateBuilder.buildOtpEmail(event.recipientName(), event.otpCode(), 10));
        }
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onAccountLocked(AccountLockedEvent event) {
        User user = event.user();
        notificationService.createAndDispatch(user, NotificationType.ACCOUNT_LOCKED, NotificationChannel.EMAIL,
                "Account Locked", "Your account has been temporarily locked due to multiple failed login attempts.",
                user.getId().toString(), "USER");
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onBookingCreated(BookingCreatedEvent event) {
        Appointment appointment = event.appointment();
        User customer = appointment.getCustomer();
        String message = "Your booking %s at %s is %s.".formatted(
                appointment.getBookingNumber(), appointment.getSalon().getName(), appointment.getStatus());
        notificationService.createAndDispatch(customer, NotificationType.BOOKING_CONFIRMATION, NotificationChannel.EMAIL,
                "Booking Confirmation", message, appointment.getId().toString(), "APPOINTMENT");
        emailService.sendHtmlEmail(customer.getEmail(), "Your SaloFresh Booking Confirmation",
                emailTemplateBuilder.buildBookingConfirmationEmail(customer.getFullName(), appointment.getBookingNumber(),
                        appointment.getSalon().getName(),
                        appointment.getAppointmentDate().format(DateTimeFormatter.ISO_LOCAL_DATE),
                        appointment.getStartTime().toString()));
        safePush(() -> pushNotificationService.sendToUser(customer, "Booking Confirmation", message,
                Map.of("type", "BOOKING_CONFIRMATION", "appointmentId", appointment.getId().toString())));
        if (customer.getPhone() != null) {
            safePush(() -> whatsAppService.sendMessage(customer.getPhone(), message));
        }
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onBookingModified(BookingModifiedEvent event) {
        Appointment appointment = event.appointment();
        notificationService.createAndDispatch(appointment.getCustomer(), NotificationType.BOOKING_MODIFIED,
                NotificationChannel.EMAIL, "Booking Updated",
                "Your booking %s has been updated.".formatted(appointment.getBookingNumber()),
                appointment.getId().toString(), "APPOINTMENT");
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onBookingCancelled(BookingCancelledEvent event) {
        Appointment appointment = event.appointment();
        String message = "Your booking %s has been cancelled.".formatted(appointment.getBookingNumber());
        notificationService.createAndDispatch(appointment.getCustomer(), NotificationType.BOOKING_CANCELLED,
                NotificationChannel.EMAIL, "Booking Cancelled", message,
                appointment.getId().toString(), "APPOINTMENT");
        safePush(() -> pushNotificationService.sendToUser(appointment.getCustomer(), "Booking Cancelled", message,
                Map.of("type", "BOOKING_CANCELLED", "appointmentId", appointment.getId().toString())));
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onBookingRejected(BookingRejectedEvent event) {
        Appointment appointment = event.appointment();
        notificationService.createAndDispatch(appointment.getCustomer(), NotificationType.BOOKING_REJECTED,
                NotificationChannel.EMAIL, "Booking Rejected",
                "Your booking %s was rejected by the salon.".formatted(appointment.getBookingNumber()),
                appointment.getId().toString(), "APPOINTMENT");
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onBookingCompleted(BookingCompletedEvent event) {
        Appointment appointment = event.appointment();
        notificationService.createAndDispatch(appointment.getCustomer(), NotificationType.BOOKING_COMPLETED,
                NotificationChannel.EMAIL, "Booking Completed",
                "Thank you for visiting %s! We hope you enjoyed the service.".formatted(appointment.getSalon().getName()),
                appointment.getId().toString(), "APPOINTMENT");
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onBookingReminder(BookingReminderEvent event) {
        Appointment appointment = event.appointment();
        notificationService.createAndDispatch(appointment.getCustomer(), NotificationType.BOOKING_REMINDER,
                NotificationChannel.SMS, "Booking Reminder",
                "Reminder: you have an appointment at %s on %s at %s.".formatted(appointment.getSalon().getName(),
                        appointment.getAppointmentDate(), appointment.getStartTime()),
                appointment.getId().toString(), "APPOINTMENT");
        if (appointment.getCustomer().getPhone() != null) {
            smsService.sendSms(appointment.getCustomer().getPhone(),
                    "Reminder: your SaloFresh appointment at %s is today at %s.".formatted(
                            appointment.getSalon().getName(), appointment.getStartTime()));
        }
        String reminderMessage = "Reminder: you have an appointment at %s on %s at %s.".formatted(
                appointment.getSalon().getName(), appointment.getAppointmentDate(), appointment.getStartTime());
        safePush(() -> pushNotificationService.sendToUser(appointment.getCustomer(), "Booking Reminder", reminderMessage,
                Map.of("type", "BOOKING_REMINDER", "appointmentId", appointment.getId().toString())));
        if (appointment.getCustomer().getPhone() != null) {
            safePush(() -> whatsAppService.sendMessage(appointment.getCustomer().getPhone(), reminderMessage));
        }
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onPaymentSuccess(PaymentSuccessEvent event) {
        Payment payment = event.payment();
        String message = "Your payment of %s %s was successful.".formatted(payment.getCurrency(), payment.getAmount());
        notificationService.createAndDispatch(payment.getUser(), NotificationType.PAYMENT_SUCCESS, NotificationChannel.EMAIL,
                "Payment Successful", message,
                payment.getId().toString(), "PAYMENT");
        safePush(() -> pushNotificationService.sendToUser(payment.getUser(), "Payment Successful", message,
                Map.of("type", "PAYMENT_SUCCESS", "paymentId", payment.getId().toString())));
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onPaymentFailed(PaymentFailedEvent event) {
        Payment payment = event.payment();
        String message = "Your payment of %s %s could not be processed.".formatted(payment.getCurrency(), payment.getAmount());
        notificationService.createAndDispatch(payment.getUser(), NotificationType.PAYMENT_FAILED, NotificationChannel.EMAIL,
                "Payment Failed", message,
                payment.getId().toString(), "PAYMENT");
        safePush(() -> pushNotificationService.sendToUser(payment.getUser(), "Payment Failed", message,
                Map.of("type", "PAYMENT_FAILED", "paymentId", payment.getId().toString())));
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onRefundProcessed(RefundProcessedEvent event) {
        var refund = event.refund();
        User user = refund.getPayment().getUser();
        notificationService.createAndDispatch(user, NotificationType.REFUND_PROCESSED, NotificationChannel.EMAIL,
                "Refund Processed", "A refund of %s has been processed to your original payment method.".formatted(refund.getAmount()),
                refund.getId().toString(), "REFUND");
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onSalonApproved(SalonApprovedEvent event) {
        var salon = event.salon();
        User owner = salon.getOwner().getUser();
        notificationService.createAndDispatch(owner, NotificationType.SALON_APPROVED, NotificationChannel.EMAIL,
                "Salon Approved", "Your salon '%s' has been approved and is now live.".formatted(salon.getName()),
                salon.getId().toString(), "SALON");
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onSalonRejected(SalonRejectedEvent event) {
        var salon = event.salon();
        User owner = salon.getOwner().getUser();
        notificationService.createAndDispatch(owner, NotificationType.SALON_REJECTED, NotificationChannel.EMAIL,
                "Salon Rejected", "Your salon '%s' was rejected. Reason: %s".formatted(salon.getName(), event.reason()),
                salon.getId().toString(), "SALON");
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onSalonSuspended(SalonSuspendedEvent event) {
        var salon = event.salon();
        User owner = salon.getOwner().getUser();
        notificationService.createAndDispatch(owner, NotificationType.SALON_SUSPENDED, NotificationChannel.EMAIL,
                "Salon Suspended", "Your salon '%s' has been suspended. Reason: %s".formatted(salon.getName(), event.reason()),
                salon.getId().toString(), "SALON");
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onNewReview(NewReviewEvent event) {
        var review = event.review();
        User owner = review.getSalon().getOwner().getUser();
        String message = "%s left a %d-star review for %s.".formatted(review.getCustomer().getFullName(),
                review.getSalonRating(), review.getSalon().getName());
        notificationService.createAndDispatch(owner, NotificationType.NEW_REVIEW, NotificationChannel.IN_APP,
                "New Review", message,
                review.getId().toString(), "REVIEW");
        safePush(() -> pushNotificationService.sendToUser(owner, "New Review", message,
                Map.of("type", "NEW_REVIEW", "reviewId", review.getId().toString())));
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onReviewReply(ReviewReplyEvent event) {
        var review = event.review();
        notificationService.createAndDispatch(review.getCustomer(), NotificationType.REVIEW_REPLY, NotificationChannel.IN_APP,
                "Salon Replied to Your Review", "%s replied to your review.".formatted(review.getSalon().getName()),
                review.getId().toString(), "REVIEW");
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onWalletTransaction(WalletTransactionEvent event) {
        var tx = event.walletTransaction();
        NotificationType type = tx.getType() == com.salofresh.common.enums.WalletTransactionType.CREDIT
                ? NotificationType.WALLET_CREDIT : NotificationType.WALLET_DEBIT;
        notificationService.createAndDispatch(event.user(), type, NotificationChannel.IN_APP,
                "Wallet " + tx.getType().name(), "Your wallet was %s with %s. New balance: %s".formatted(
                        tx.getType() == com.salofresh.common.enums.WalletTransactionType.CREDIT ? "credited" : "debited",
                        tx.getAmount(), tx.getBalanceAfter()),
                tx.getId().toString(), "WALLET");
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onRewardPointsEarned(RewardPointsEarnedEvent event) {
        var rp = event.rewardPoint();
        notificationService.createAndDispatch(event.user(), NotificationType.REWARD_POINTS_EARNED, NotificationChannel.IN_APP,
                "Reward Points Earned", "You earned %d reward points. New balance: %d points".formatted(rp.getPoints(), rp.getBalanceAfter()),
                rp.getId().toString(), "REWARD_POINT");
    }

    /**
     * Runs a best-effort push/WhatsApp delivery, swallowing and logging any exception so that a
     * misbehaving or unconfigured provider never breaks the surrounding event handler. The
     * underlying push/WhatsApp services already catch their own delivery failures internally;
     * this is an extra safety net around argument evaluation (e.g. unexpected nulls).
     */
    private void safePush(Runnable action) {
        try {
            action.run();
        } catch (Exception ex) {
            log.error("Best-effort push/WhatsApp dispatch failed", ex);
        }
    }
}
