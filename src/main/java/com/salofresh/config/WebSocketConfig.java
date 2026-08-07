package com.salofresh.config;

import com.salofresh.security.StompAuthChannelInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.util.CollectionUtils;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP/SockJS setup for real-time chat delivery.
 *
 * <p>Authentication for the WebSocket channel is enforced entirely by
 * {@link StompAuthChannelInterceptor} on the STOMP CONNECT frame (see that class for details) -
 * the raw {@code /ws/**} HTTP endpoint used for the SockJS handshake itself is listed in
 * {@link com.salofresh.constant.SecurityConstants#PUBLIC_ENDPOINTS} since Spring Security has no
 * visibility into STOMP frames carried over an already-established WebSocket/SockJS connection.
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final AppProperties appProperties;
    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        var allowedOrigins = appProperties.getCors().getAllowedOrigins();
        String[] originPatterns = CollectionUtils.isEmpty(allowedOrigins)
                ? new String[] {"*"}
                : allowedOrigins.toArray(new String[0]);

        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(originPatterns)
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthChannelInterceptor);
    }
}
