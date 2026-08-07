package com.salofresh.security;

import com.salofresh.common.enums.TokenType;
import com.salofresh.constant.SecurityConstants;
import com.salofresh.repository.BlacklistedTokenRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Validates the JWT carried on the STOMP CONNECT frame's {@code Authorization} native header and,
 * on success, attaches a {@link StompPrincipal} (wrapping the authenticated user's id) to the
 * session. CONNECT frames without a valid, non-blacklisted access token are rejected outright by
 * throwing, which causes Spring to send an ERROR frame back and close the session - this is what
 * enforces authentication for the WebSocket channel, since the raw {@code /ws/**} HTTP handshake
 * endpoint itself has to be listed as a public endpoint for the SockJS handshake to succeed.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final BlacklistedTokenRepository blacklistedTokenRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            Long userId = authenticate(accessor);
            accessor.setUser(new StompPrincipal(userId));
        }

        return message;
    }

    private Long authenticate(StompHeaderAccessor accessor) {
        String token = resolveToken(accessor);
        if (!StringUtils.hasText(token) || !jwtTokenProvider.validateToken(token)) {
            throw new MessagingException("Missing or invalid Authorization token for STOMP CONNECT");
        }

        Claims claims = jwtTokenProvider.parseClaims(token);
        String tokenType = claims.get(SecurityConstants.CLAIM_TOKEN_TYPE, String.class);
        if (!TokenType.ACCESS.name().equals(tokenType)) {
            throw new MessagingException("STOMP CONNECT requires an access token");
        }
        if (blacklistedTokenRepository.existsByTokenId(claims.getId())) {
            throw new MessagingException("Token has been revoked");
        }

        Long userId = jwtTokenProvider.getUserIdFromToken(token);
        try {
            userDetailsService.loadUserById(userId);
        } catch (Exception ex) {
            throw new MessagingException("No such user for STOMP CONNECT token", ex);
        }

        return userId;
    }

    private String resolveToken(StompHeaderAccessor accessor) {
        String bearerToken = accessor.getFirstNativeHeader(SecurityConstants.AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(SecurityConstants.TOKEN_PREFIX)) {
            return bearerToken.substring(SecurityConstants.TOKEN_PREFIX.length());
        }
        return null;
    }
}
