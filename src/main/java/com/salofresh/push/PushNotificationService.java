package com.salofresh.push;

import com.salofresh.entity.User;

import java.util.Map;

public interface PushNotificationService {

    /**
     * Sends a push notification to every active device token registered for the given user.
     * Best-effort: individual token failures are logged and swallowed, never thrown.
     */
    void sendToUser(User user, String title, String body, Map<String, String> data);

    /**
     * Sends a push notification to a single device token. Best-effort: failures are logged and
     * swallowed, never thrown.
     */
    void sendToToken(String token, String title, String body, Map<String, String> data);
}
