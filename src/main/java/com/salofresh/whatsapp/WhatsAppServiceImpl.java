package com.salofresh.whatsapp;

import com.salofresh.config.AppProperties;
import com.salofresh.entity.WhatsAppLog;
import com.salofresh.repository.WhatsAppLogRepository;
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
public class WhatsAppServiceImpl implements WhatsAppService {

    private static final String STATUS_SENT = "SENT";
    private static final String STATUS_FAILED = "FAILED";

    private final AppProperties appProperties;
    private final WhatsAppLogRepository whatsAppLogRepository;
    private final Map<String, WhatsAppProvider> providersByName;

    public WhatsAppServiceImpl(AppProperties appProperties, WhatsAppLogRepository whatsAppLogRepository,
                                List<WhatsAppProvider> providers) {
        this.appProperties = appProperties;
        this.whatsAppLogRepository = whatsAppLogRepository;
        this.providersByName = providers.stream()
                .collect(Collectors.toMap(WhatsAppProvider::getProviderName, Function.identity()));
    }

    @Override
    @Async("taskExecutor")
    public void sendMessage(String phoneNumber, String message) {
        String providerName = appProperties.getWhatsapp().getProvider();
        WhatsAppProvider provider = providersByName.getOrDefault(providerName, providersByName.get("console"));

        WhatsAppLog.WhatsAppLogBuilder logBuilder = WhatsAppLog.builder().recipient(phoneNumber).message(message);
        try {
            provider.send(phoneNumber, message);
            whatsAppLogRepository.save(logBuilder.status(STATUS_SENT).sentAt(Instant.now()).build());
        } catch (Exception ex) {
            log.error("Failed to send WhatsApp message to {} via provider {}", phoneNumber, providerName, ex);
            whatsAppLogRepository.save(logBuilder.status(STATUS_FAILED).errorMessage(ex.getMessage()).build());
        }
    }
}
