package com.moida.config.realtime;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis 릴레이가 비활성일 때(로컬/테스트, 단일 인스턴스) 사용하는 기본 구현.
 * 로컬 STOMP 브로커로 곧바로 전달한다.
 *
 * <p>{@code moida.realtime.redis-relay.enabled} 가 없거나 false 면 이 빈이 활성화된다.
 */
@Component
@ConditionalOnProperty(prefix = "moida.realtime.redis-relay", name = "enabled",
        havingValue = "false", matchIfMissing = true)
@RequiredArgsConstructor
public class LocalRealtimeMessagePublisher implements RealtimeMessagePublisher {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void broadcast(String destination, Object payload) {
        messagingTemplate.convertAndSend(destination, payload);
    }

    @Override
    public void sendToUser(String user, String destination, Object payload) {
        messagingTemplate.convertAndSendToUser(user, destination, payload);
    }
}
