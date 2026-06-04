package com.moida.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthChannelInterceptor webSocketAuthChannelInterceptor;
    private final JwtHandshakeHandler jwtHandshakeHandler;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 클라이언트는 /topic/... (공개 브로드캐스트, 예: 상품 채팅) 또는
        // /user/queue/... (개인 알림) 을 구독하고, 앱 메시지는 /app/... 으로 보낸다.
        // /queue 는 SimpMessagingTemplate.convertAndSendToUser(...) 가 /user/queue/{destination} 으로
        // 라우팅할 때 필요한 베이스 prefix 이다.
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 로컬에서는 Vite가 /ws를 프록시하고, 운영 nginx도 같은 경로를 전달한다.
        // 핸드셰이크 시 ?token=<JWT> 로 인증해 session.getPrincipal() 을 세팅한다
        // (개인 알림 user-destination 이 동작하려면 세션에 Principal 이 있어야 함).
        registry.addEndpoint("/ws")
                .setHandshakeHandler(jwtHandshakeHandler)
                .setAllowedOriginPatterns("*");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // 들어오는 STOMP 프레임에 JWT 인증을 적용한다.
        registration.interceptors(webSocketAuthChannelInterceptor);
    }
}
