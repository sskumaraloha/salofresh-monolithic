package com.salofresh.push;

import com.salofresh.common.enums.DevicePlatform;
import com.salofresh.entity.DeviceToken;
import com.salofresh.entity.User;

import java.util.List;

public interface DeviceTokenService {

    /**
     * Registers (or re-registers) a push device token for the given user. If the token already
     * exists — whether previously owned by this user or a different one (e.g. a device changing
     * accounts) — it is reassigned to this user, its platform updated, and marked active.
     */
    void registerToken(User user, String token, DevicePlatform platform);

    /**
     * Deactivates a device token so it no longer receives push notifications.
     */
    void unregisterToken(String token);

    List<DeviceToken> listActiveTokensForUser(Long userId);
}
