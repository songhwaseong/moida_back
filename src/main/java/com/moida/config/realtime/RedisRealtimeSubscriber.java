package com.moida.config.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis 릴레이 채널을 구독해, 다른(혹은 자기) 인스턴스가 보낸 메시지를
 * 이 인스턴스의 로컬 STOMP 브로커로 전달한다.
 *
 * <p>여기서는 {@link SimpMessagingTemplate} 을 직접 호출한다 — 이 경로는 Redis 로 되돌려 보내지 않으므로
 * 무한 루프가 생기지 않는다. 개인 큐(USER) 의 경우, 해당 사용자가 이 인스턴스에 연결돼 있을 때만 실제 전달된다.
 */
@Component
@ConditionalOnProperty(prefix = "moida.realtime.redis-relay", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class RedisRealtimeSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            RealtimeMessageEnvelope envelope =
                    objectMapper.readValue(message.getBody(), RealtimeMessageEnvelope.class);
            switch (envelope.kind()) {
                case BROADCAST -> messagingTemplate.convertAndSend(envelope.destination(), envelope.payload());
                case USER -> messagingTemplate.convertAndSendToUser(
                        envelope.user(), envelope.destination(), envelope.payload());
            }
        } catch (Exception e) {
            log.warn("[RedisRealtimeSubscriber] failed to relay message to local broker: {}", e.getMessage());
        }
    }
}
