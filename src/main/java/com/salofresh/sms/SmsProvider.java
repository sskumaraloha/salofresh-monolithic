package com.salofresh.sms;

public interface SmsProvider {

    String getProviderName();

    void send(String phoneNumber, String message) throws Exception;
}
