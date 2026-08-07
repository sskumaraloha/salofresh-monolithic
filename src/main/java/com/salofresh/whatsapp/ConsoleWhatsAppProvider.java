package com.salofresh.whatsapp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ConsoleWhatsAppProvider implements WhatsAppProvider {

    @Override
    public String getProviderName() {
        return "console";
    }

    @Override
    public void send(String phoneNumber, String message) {
        log.info("[WHATSAPP -> {}]: {}", phoneNumber, message);
    }
}
