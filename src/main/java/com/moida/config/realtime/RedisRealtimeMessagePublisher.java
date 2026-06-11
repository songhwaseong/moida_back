package com.moida.config.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis 릴레이가 활성일 때(운영, 멀티 인스턴스) 사용하는 구현.
 *
 * <p>로컬 브로커로 직접 전달하지 않고 Redis 채널로만 publish 한다.
 * 실제 클라이언트 전달은 모든 인스턴스의 {@link RedisRealtimeSubscriber} 가 수행한다.
 * (자기 자신을 포함한 전 인스턴스가 한 번씩 로컬 전달 → 정확히 한 번 전달, 무한 루프 없음)
 */
@Component
@ConditionalOnProperty(prefix = "moida.realtime.redis-relay", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class RedisRealtimeMessagePublisher implements RealtimeMessagePublisher {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void broadcast(String destination, Object payload) {
        publish(RealtimeMessageEnvelope.broadcast(destination, payload));
    }

    @Override
    public void sendToUser(String user, String destination, Object payload) {
        publish(RealtimeMessageEnvelope.toUser(user, destination, payload));
    }

    private void publish(RealtimeMessageEnvelope envelope) {
        try {
            redisTemplate.convertAndSend(RELAY_CHANNEL, objectMapper.writeValueAsString(envelope));
        } catch (Exception e) {
            // Redis 장애 시에도 메인 흐름(채팅 저장/알림 저장)은 깨지지 않도록 로깅만 한다.
            log.warn("[RedisRealtimeMessagePublisher] relay publish failed kind={}, dest={}: {}",
                    envelope.kind(), envelope.destination(), e.getMessage());
        }
    }
}
