package com.salofresh.security;

import java.security.Principal;

/**
 * Lightweight {@link Principal} attached to a STOMP session by {@link StompAuthChannelInterceptor}
 * once the CONNECT frame's JWT has been validated. The principal's name is the authenticated
 * user's id, letting {@code @MessageMapping} handlers recover the sender without needing the
 * full {@link UserPrincipal}/{@code UserDetails} graph on every frame.
 */
public class StompPrincipal implements Principal {

    private final String userId;

    public StompPrincipal(Long userId) {
        this.userId = String.valueOf(userId);
    }

    @Override
    public String getName() {
        return userId;
    }
}
