package com.salofresh.whatsapp;

import com.salofresh.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Sends WhatsApp messages via the Meta WhatsApp Business Cloud API. Requires a valid access token
 * and phone number id to be configured; activated only when {@code app.whatsapp.provider=meta}.
 */
@Component
@RequiredArgsConstructor
public class MetaWhatsAppProvider implements WhatsAppProvider {

    private static final String META_API_URL = "https://graph.facebook.com/v18.0/%s/messages";

    private final AppProperties appProperties;
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String getProviderName() {
        return "meta";
    }

    @Override
    public void send(String phoneNumber, String message) {
        AppProperties.Whatsapp.Meta meta = appProperties.getWhatsapp().getMeta();
        String url = META_API_URL.formatted(meta.getPhoneNumberId());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(meta.getAccessToken());

        Map<String, Object> text = new LinkedHashMap<>();
        text.put("body", message);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("messaging_product", "whatsapp");
        body.put("to", phoneNumber);
        body.put("type", "text");
        body.put("text", text);

        restTemplate.postForEntity(url, new HttpEntity<>(body, headers), String.class);
    }
}
