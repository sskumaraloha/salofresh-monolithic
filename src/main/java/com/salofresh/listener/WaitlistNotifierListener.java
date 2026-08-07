package com.salofresh.listener;

import com.salofresh.common.enums.NotificationChannel;
import com.salofresh.common.enums.NotificationType;
import com.salofresh.common.enums.WaitlistStatus;
import com.salofresh.entity.Appointment;
import com.salofresh.entity.Employee;
import com.salofresh.entity.Waitlist;
import com.salofresh.event.BookingCancelledEvent;
import com.salofresh.event.BookingRejectedEvent;
import com.salofresh.exception.SlotUnavailableException;
import com.salofresh.notification.NotificationService;
import com.salofresh.repository.WaitlistRepository;
import com.salofresh.service.booking.SlotAvailabilityService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Reacts to appointments that stop occupying their slot (cancelled or rejected) and auto-notifies
 * any customer waiting for that salon/employee/date whose preferred window overlaps the slot that
 * just freed up. Deliberately separate from {@link NotificationEventListener}: this is a new
 * domain concern (waitlist matching), not one of the notifications already wired there.
 */
@Component
@RequiredArgsConstructor
public class WaitlistNotifierListener {

    private static final Logger log = LoggerFactory.getLogger(WaitlistNotifierListener.class);

    private final WaitlistRepository waitlistRepository;
    private final SlotAvailabilityService slotAvailabilityService;
    private final NotificationService notificationService;

    @Async("taskExecutor")
    @EventListener
    @Transactional
    public void onBookingCancelled(BookingCancelledEvent event) {
        notifyWaitlistForFreedSlot(event.appointment());
    }

    @Async("taskExecutor")
    @EventListener
    @Transactional
    public void onBookingRejected(BookingRejectedEvent event) {
        notifyWaitlistForFreedSlot(event.appointment());
    }

    private void notifyWaitlistForFreedSlot(Appointment appointment) {
        LocalDate date = appointment.getAppointmentDate();
        LocalTime freedStart = appointment.getStartTime();
        LocalTime freedEnd = appointment.getEndTime();

        List<Waitlist> candidates = new ArrayList<>();
        if (appointment.getEmployee() != null) {
            candidates.addAll(waitlistRepository.findAllByEmployeeIdAndPreferredDateAndStatus(
                    appointment.getEmployee().getId(), date, WaitlistStatus.WAITING));
        }
        // Also consider waitlist entries with no employee preference (any employee at the salon works).
        waitlistRepository.findAllBySalonIdAndPreferredDateAndStatus(appointment.getSalon().getId(), date, WaitlistStatus.WAITING)
                .stream()
                .filter(w -> w.getEmployee() == null)
                .forEach(candidates::add);

        for (Waitlist waitlist : candidates) {
            if (!overlaps(waitlist, freedStart, freedEnd)) {
                continue;
            }

            LocalTime start = waitlist.getPreferredStartTime() != null ? waitlist.getPreferredStartTime() : freedStart;
            LocalTime end = waitlist.getPreferredEndTime() != null ? waitlist.getPreferredEndTime() : freedEnd;
            Employee employeeToCheck = waitlist.getEmployee();

            try {
                slotAvailabilityService.ensureSlotAvailable(waitlist.getSalon(), employeeToCheck, date, start, end, null);
            } catch (SlotUnavailableException ex) {
                // Not genuinely free (yet) for this candidate - skip it and try the next one.
                continue;
            }

            waitlist.setStatus(WaitlistStatus.NOTIFIED);
            waitlist.setNotifiedAt(Instant.now());
            waitlistRepository.save(waitlist);

            notificationService.createAndDispatch(waitlist.getUser(), NotificationType.GENERIC, NotificationChannel.IN_APP,
                    "A slot just opened up",
                    "Good news! A slot at %s on %s has become available. Book now before it's gone."
                            .formatted(waitlist.getSalon().getName(), date),
                    waitlist.getId().toString(), "WAITLIST");

            log.info("Notified user {} that waitlist entry {} at salon {} is now available",
                    waitlist.getUser().getId(), waitlist.getId(), waitlist.getSalon().getId());
        }
    }

    private boolean overlaps(Waitlist waitlist, LocalTime freedStart, LocalTime freedEnd) {
        LocalTime preferredStart = waitlist.getPreferredStartTime();
        LocalTime preferredEnd = waitlist.getPreferredEndTime();
        if (preferredStart == null || preferredEnd == null) {
            return true;
        }
        return preferredStart.isBefore(freedEnd) && preferredEnd.isAfter(freedStart);
    }
}
