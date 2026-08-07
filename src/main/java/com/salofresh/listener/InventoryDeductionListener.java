package com.salofresh.listener;

import com.salofresh.common.enums.NotificationChannel;
import com.salofresh.common.enums.NotificationType;
import com.salofresh.common.enums.StockTransactionType;
import com.salofresh.entity.Appointment;
import com.salofresh.entity.AppointmentService;
import com.salofresh.entity.Product;
import com.salofresh.entity.Salon;
import com.salofresh.entity.ServiceProductUsage;
import com.salofresh.entity.StockTransaction;
import com.salofresh.entity.User;
import com.salofresh.event.BookingCompletedEvent;
import com.salofresh.notification.NotificationService;
import com.salofresh.repository.AppointmentServiceRepository;
import com.salofresh.repository.ProductRepository;
import com.salofresh.repository.ServiceProductUsageRepository;
import com.salofresh.repository.StockTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Reacts to a completed appointment by auto-deducting stock for every product consumed by the
 * services on that booking, per {@link ServiceProductUsage} link. Deliberately a new, separate
 * listener from {@link NotificationEventListener} and {@link WaitlistNotifierListener}: this is a
 * new domain concern (inventory), not one of the notifications already wired there.
 *
 * <p>A deduction that would take a product's stock negative is still applied - the salon should
 * still see the completed booking reflected - but {@code currentStock} is clamped at 0 and the
 * recorded {@code StockTransaction} reason notes the clamp. Any unexpected failure while
 * processing a single usage link is caught and logged rather than propagated, so a misbehaving
 * product/usage row can never break the booking-completion flow.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryDeductionListener {

    private static final String REFERENCE_TYPE_APPOINTMENT = "APPOINTMENT";

    private final AppointmentServiceRepository appointmentServiceRepository;
    private final ServiceProductUsageRepository serviceProductUsageRepository;
    private final ProductRepository productRepository;
    private final StockTransactionRepository stockTransactionRepository;
    private final NotificationService notificationService;

    @Async("taskExecutor")
    @EventListener
    @Transactional
    public void onBookingCompleted(BookingCompletedEvent event) {
        Appointment appointment = event.appointment();
        try {
            List<AppointmentService> lineItems = appointmentServiceRepository.findAllByAppointmentId(appointment.getId());
            for (AppointmentService lineItem : lineItems) {
                deductForLineItem(appointment, lineItem);
            }
        } catch (Exception ex) {
            log.error("Failed to auto-deduct inventory for completed appointment {}", appointment.getId(), ex);
        }
    }

    private void deductForLineItem(Appointment appointment, AppointmentService lineItem) {
        if (lineItem.getService() == null) {
            return;
        }
        List<ServiceProductUsage> usages = serviceProductUsageRepository.findAllByServiceId(lineItem.getService().getId());
        for (ServiceProductUsage usage : usages) {
            deductForUsage(appointment, lineItem, usage);
        }
    }

    private void deductForUsage(Appointment appointment, AppointmentService lineItem, ServiceProductUsage usage) {
        try {
            Product product = usage.getProduct();
            if (product == null || product.isDeleted()) {
                return;
            }
            int deduction = usage.getQuantityPerService() * lineItem.getQuantity();
            if (deduction <= 0) {
                return;
            }

            int newStock = product.getCurrentStock() - deduction;
            boolean clamped = newStock < 0;
            product.setCurrentStock(Math.max(newStock, 0));
            productRepository.save(product);

            StockTransaction transaction = StockTransaction.builder()
                    .product(product)
                    .type(StockTransactionType.STOCK_OUT)
                    .quantity(deduction)
                    .reason(clamped
                            ? "Appointment completion (stock clamped at 0 - insufficient stock available)"
                            : "Appointment completion")
                    .referenceType(REFERENCE_TYPE_APPOINTMENT)
                    .referenceId(appointment.getBookingNumber())
                    .build();
            stockTransactionRepository.save(transaction);

            if (product.isBelowReorderLevel()) {
                notifyLowStock(product);
            }
        } catch (Exception ex) {
            log.error("Failed to auto-deduct stock for service-product-usage {} on appointment {}",
                    usage.getId(), appointment.getBookingNumber(), ex);
        }
    }

    private void notifyLowStock(Product product) {
        Salon salon = product.getSalon();
        if (salon == null || salon.getOwner() == null || salon.getOwner().getUser() == null) {
            return;
        }
        User owner = salon.getOwner().getUser();
        notificationService.createAndDispatch(owner, NotificationType.GENERIC, NotificationChannel.IN_APP,
                "Low stock alert",
                "%s at %s is low on stock (%d remaining, reorder level %d).".formatted(
                        product.getName(), salon.getName(), product.getCurrentStock(), product.getReorderLevel()),
                product.getId().toString(), "PRODUCT");
    }
}
