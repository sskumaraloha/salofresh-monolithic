package com.salofresh.sms;

import com.salofresh.config.AppProperties;
import com.salofresh.entity.SmsLog;
import com.salofresh.repository.SmsLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SmsServiceImpl implements SmsService {

    private static final String STATUS_SENT = "SENT";
    private static final String STATUS_FAILED = "FAILED";

    private final AppProperties appProperties;
    private final SmsLogRepository smsLogRepository;
    private final Map<String, SmsProvider> providersByName;

    public SmsServiceImpl(AppProperties appProperties, SmsLogRepository smsLogRepository, List<SmsProvider> providers) {
        this.appProperties = appProperties;
        this.smsLogRepository = smsLogRepository;
        this.providersByName = providers.stream()
                .collect(Collectors.toMap(SmsProvider::getProviderName, Function.identity()));
    }

    @Override
    @Async("taskExecutor")
    public void sendSms(String phoneNumber, String message) {
        String providerName = appProperties.getSms().getProvider();
        SmsProvider provider = providersByName.getOrDefault(providerName, providersByName.get("console"));

        SmsLog.SmsLogBuilder logBuilder = SmsLog.builder().recipient(phoneNumber).message(message);
        try {
            provider.send(phoneNumber, message);
            smsLogRepository.save(logBuilder.status(STATUS_SENT).sentAt(Instant.now()).build());
        } catch (Exception ex) {
            log.error("Failed to send SMS to {} via provider {}", phoneNumber, providerName, ex);
            smsLogRepository.save(logBuilder.status(STATUS_FAILED).errorMessage(ex.getMessage()).build());
        }
    }
}
