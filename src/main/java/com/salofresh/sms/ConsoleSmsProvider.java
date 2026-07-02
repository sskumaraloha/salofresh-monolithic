package com.salofresh.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ConsoleSmsProvider implements SmsProvider {

    @Override
    public String getProviderName() {
        return "console";
    }

    @Override
    public void send(String phoneNumber, String message) {
        log.info("[SMS -> {}]: {}", phoneNumber, message);
    }
}
