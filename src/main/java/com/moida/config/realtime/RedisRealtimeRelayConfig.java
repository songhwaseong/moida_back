package com.moida.config.realtime;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * Redis 릴레이 활성 시에만 메시지 리스너 컨테이너를 등록한다.
 * (relay 비활성 = 로컬/테스트에서는 이 컨테이너가 만들어지지 않으므로 Redis 연결을 시도하지 않는다.)
 */
@Configuration
@ConditionalOnProperty(prefix = "moida.realtime.redis-relay", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class RedisRealtimeRelayConfig {

    @Bean
    public RedisMessageListenerContainer realtimeRelayListenerContainer(
            RedisConnectionFactory connectionFactory,
            RedisRealtimeSubscriber subscriber
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(subscriber, new ChannelTopic(RealtimeMessagePublisher.RELAY_CHANNEL));
        return container;
    }
}
