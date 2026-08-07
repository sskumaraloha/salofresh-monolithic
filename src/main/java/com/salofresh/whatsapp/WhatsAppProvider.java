package com.salofresh.whatsapp;

public interface WhatsAppProvider {

    String getProviderName();

    void send(String phoneNumber, String message) throws Exception;
}
