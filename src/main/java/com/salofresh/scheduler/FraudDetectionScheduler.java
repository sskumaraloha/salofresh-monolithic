package com.salofresh.scheduler;

import com.salofresh.service.fraud.FraudDetectionService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Hourly job that runs each fraud/anomaly heuristic in {@link FraudDetectionService}. Each
 * detector is wrapped in its own try/catch so that a failure in one (e.g. a transient DB issue)
 * does not prevent the others from running.
 */
@Component
@RequiredArgsConstructor
public class FraudDetectionScheduler {

    private static final Logger log = LoggerFactory.getLogger(FraudDetectionScheduler.class);

    private final FraudDetectionService fraudDetectionService;

    @Scheduled(cron = "0 0 * * * *")
    public void runFraudDetection() {
        runDetector("detectRepeatedFailedPayments", fraudDetectionService::detectRepeatedFailedPayments);
        runDetector("detectSuspiciousReviewPatterns", fraudDetectionService::detectSuspiciousReviewPatterns);
        runDetector("detectReferralAbuse", fraudDetectionService::detectReferralAbuse);
    }

    private void runDetector(String name, Runnable detector) {
        try {
            detector.run();
        } catch (Exception ex) {
            log.error("Fraud detector '{}' failed", name, ex);
        }
    }
}
