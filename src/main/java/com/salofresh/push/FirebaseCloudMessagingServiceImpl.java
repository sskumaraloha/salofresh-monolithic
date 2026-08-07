package com.salofresh.push;

import com.google.auth.oauth2.GoogleCredentials;
import com.salofresh.config.AppProperties;
import com.salofresh.entity.DeviceToken;
import com.salofresh.entity.User;
import com.salofresh.repository.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Delivers push notifications via the Firebase Cloud Messaging HTTP v1 API
 * ({@code https://fcm.googleapis.com/v1/projects/{projectId}/messages:send}).
 * <p>
 * The v1 API requires an OAuth2 bearer token (the legacy static server-key based
 * {@code fcm/send} API is deprecated), so an access token is minted on demand from the
 * Google service-account credential configured via {@code app.push.fcm.service-account-json}
 * using the {@code google-auth-library-oauth2-http} client. The credential is cached and
 * transparently refreshed (tokens are valid for ~1 hour).
 * <p>
 * All failures (missing configuration, network errors, invalid/expired tokens, etc.) are logged
 * and swallowed — push delivery is fire-and-forget and must never fail the caller's transaction.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FirebaseCloudMessagingServiceImpl implements PushNotificationService {

    private static final String FCM_SCOPE = "https://www.googleapis.com/auth/firebase.messaging";
    private static final String FCM_URL_TEMPLATE = "https://fcm.googleapis.com/v1/projects/%s/messages:send";

    private final AppProperties appProperties;
    private final DeviceTokenRepository deviceTokenRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    private volatile GoogleCredentials cachedCredentials;

    @Override
    @Async("taskExecutor")
    public void sendToUser(User user, String title, String body, Map<String, String> data) {
        if (user == null || user.getId() == null) {
            return;
        }
        List<DeviceToken> tokens = deviceTokenRepository.findAllByUserIdAndActiveTrue(user.getId());
        for (DeviceToken deviceToken : tokens) {
            sendToToken(deviceToken.getToken(), title, body, data);
        }
    }

    @Override
    @Async("taskExecutor")
    public void sendToToken(String token, String title, String body, Map<String, String> data) {
        if (token == null || token.isBlank()) {
            return;
        }
        try {
            String projectId = appProperties.getPush().getFcm().getProjectId();
            if (projectId == null || projectId.isBlank()) {
                log.debug("FCM project id not configured; skipping push send to token {}", mask(token));
                return;
            }

            String accessToken = resolveAccessToken();
            if (accessToken == null) {
                log.debug("FCM credentials not configured or invalid; skipping push send to token {}", mask(token));
                return;
            }

            Map<String, Object> notification = new LinkedHashMap<>();
            notification.put("title", title);
            notification.put("body", body);

            Map<String, Object> message = new LinkedHashMap<>();
            message.put("token", token);
            message.put("notification", notification);
            if (data != null && !data.isEmpty()) {
                message.put("data", data);
            }

            Map<String, Object> payload = Map.of("message", message);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);

            String url = FCM_URL_TEMPLATE.formatted(projectId);
            restTemplate.postForEntity(url, new HttpEntity<>(payload, headers), String.class);
        } catch (Exception ex) {
            log.error("Failed to send push notification to token {}", mask(token), ex);
        }
    }

    private synchronized String resolveAccessToken() {
        try {
            String credentialJson = appProperties.getPush().getFcm().getServiceAccountJson();
            if (credentialJson == null || credentialJson.isBlank()) {
                return null;
            }
            if (cachedCredentials == null) {
                cachedCredentials = loadCredentials(credentialJson).createScoped(List.of(FCM_SCOPE));
            }
            cachedCredentials.refreshIfExpired();
            return cachedCredentials.getAccessToken().getTokenValue();
        } catch (Exception ex) {
            log.error("Failed to obtain FCM OAuth2 access token", ex);
            cachedCredentials = null;
            return null;
        }
    }

    private GoogleCredentials loadCredentials(String credentialJson) throws IOException {
        String trimmed = credentialJson.trim();
        try (InputStream inputStream = trimmed.startsWith("{")
                ? new ByteArrayInputStream(trimmed.getBytes(StandardCharsets.UTF_8))
                : new FileInputStream(trimmed)) {
            return GoogleCredentials.fromStream(inputStream);
        }
    }

    private String mask(String token) {
        return token.length() > 8 ? token.substring(0, 8) + "..." : "***";
    }
}
