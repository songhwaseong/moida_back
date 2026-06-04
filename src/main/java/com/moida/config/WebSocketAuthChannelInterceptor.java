package com.moida.config;

import com.moida.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    // CONNECT 때 인증한 Principal 을 세션에 보관해 두는 키.
    private static final String STOMP_USER_ATTR = "stompUser";

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand command = accessor.getCommand();
        // 명령이 없는 프레임(heartbeat 등)이나 이미 인증된 프레임은 그대로 둔다.
        if (command == null || accessor.getUser() != null) {
            return message;
        }

        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();

        // CONNECT: Authorization 헤더의 JWT 로 인증하고, 그 Principal 을 세션에 저장한다.
        // WebSocket/STOMP 트래픽은 일반 HTTP 보안 필터를 거치지 않으므로 여기서 직접 인증한다.
        if (StompCommand.CONNECT.equals(command)) {
            String token = resolveToken(accessor);
            if (!StringUtils.hasText(token) || !jwtTokenProvider.validateToken(token)) {
                return message;
            }
            Authentication authentication = jwtTokenProvider.getAuthentication(token);
            accessor.setUser(authentication);
            // SUBSCRIBE/SEND 프레임에는 Authorization 헤더가 없으므로(클라이언트가 CONNECT 때만 보냄),
            // 여기서 인증한 Principal 을 세션에 저장해 두고 이후 모든 프레임에 복원해 준다.
            // 이게 없으면 SUBSCRIBE 가 익명으로 등록되어 convertAndSendToUser(email) 가
            // 대상 세션을 찾지 못해 개인 알림(user-destination)이 전달되지 않는다.
            if (sessionAttributes != null) {
                sessionAttributes.put(STOMP_USER_ATTR, authentication);
            }
            return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
        }

        // CONNECT 이외(SUBSCRIBE/SEND 등): 세션에 저장해 둔 Principal 을 복원한다.
        if (sessionAttributes != null && sessionAttributes.get(STOMP_USER_ATTR) instanceof Authentication authentication) {
            accessor.setUser(authentication);
            return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
        }

        return message;
    }

    private String resolveToken(StompHeaderAccessor accessor) {
        String bearer = accessor.getFirstNativeHeader(AUTH_HEADER);
        if (StringUtils.hasText(bearer) && bearer.startsWith(BEARER_PREFIX)) {
            return bearer.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
