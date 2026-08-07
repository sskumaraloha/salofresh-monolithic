package com.salofresh.scheduler;

import com.salofresh.common.enums.WaitlistStatus;
import com.salofresh.entity.Waitlist;
import com.salofresh.repository.WaitlistRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Daily housekeeping for the waitlist: any entry still WAITING once its preferred date has
 * passed is no longer actionable and is marked EXPIRED.
 */
@Component
@RequiredArgsConstructor
public class WaitlistCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(WaitlistCleanupScheduler.class);

    private final WaitlistRepository waitlistRepository;

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void expireStaleWaitlistEntries() {
        List<Waitlist> stale = waitlistRepository.findAllByStatusAndPreferredDateBefore(WaitlistStatus.WAITING, LocalDate.now());
        if (stale.isEmpty()) {
            return;
        }
        stale.forEach(waitlist -> waitlist.setStatus(WaitlistStatus.EXPIRED));
        waitlistRepository.saveAll(stale);
        log.info("Expired {} stale waitlist entries", stale.size());
    }
}
