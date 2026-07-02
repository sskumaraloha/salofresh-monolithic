package com.salofresh.sms;

import com.salofresh.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Sends SMS via the Twilio REST API. Requires valid Twilio credentials to be configured;
 * activated only when {@code app.sms.provider=twilio}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TwilioSmsProvider implements SmsProvider {

    private static final String TWILIO_API_URL = "https://api.twilio.com/2010-04-01/Accounts/%s/Messages.json";

    private final AppProperties appProperties;
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String getProviderName() {
        return "twilio";
    }

    @Override
    public void send(String phoneNumber, String message) {
        AppProperties.Sms.Twilio twilio = appProperties.getSms().getTwilio();
        String url = TWILIO_API_URL.formatted(twilio.getAccountSid());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        String credentials = twilio.getAccountSid() + ":" + twilio.getAuthToken();
        headers.set("Authorization", "Basic " + Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8)));

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("To", phoneNumber);
        body.add("From", twilio.getFromNumber());
        body.add("Body", message);

        restTemplate.postForEntity(url, new HttpEntity<>(body, headers), String.class);
    }
}
