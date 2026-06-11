package com.moida.config.realtime;

/**
 * WebSocket(STOMP) 실시간 메시지 송신 추상화.
 *
 * <p>왜 이 인터페이스가 필요한가:
 * EC2 가 2대 이상이면 각 인스턴스의 인메모리 STOMP 브로커는 서로 격리되어 있어서,
 * 한 인스턴스에서 {@code convertAndSend} 한 메시지는 다른 인스턴스에 붙은 클라이언트에게 도달하지 않는다.
 * 이 인터페이스를 통해 송신하면, 운영 프로파일에서는 Redis Pub/Sub 로 모든 인스턴스에 전파된다.
 *
 * <p><b>주의:</b> 새로운 실시간 송신을 추가할 때는 {@code SimpMessagingTemplate} 을 직접 쓰지 말고
 * 반드시 이 인터페이스를 사용해야 한다. 직접 쓰면 그 기능만 멀티 인스턴스 환경에서 동작하지 않는다.
 *
 * <p>구현체:
 * <ul>
 *   <li>{@link LocalRealtimeMessagePublisher} — relay 비활성(로컬/테스트). 로컬 브로커로 직접 전달.</li>
 *   <li>{@link RedisRealtimeMessagePublisher} — relay 활성(운영). Redis 채널로 publish → 전 인스턴스 전파.</li>
 * </ul>
 */
public interface RealtimeMessagePublisher {

    /** Redis 릴레이가 사용하는 Pub/Sub 채널 이름. */
    String RELAY_CHANNEL = "moida:ws:relay";

    /**
     * 공개 토픽으로 브로드캐스트한다. (예: {@code /topic/products/{id}/chat})
     * 해당 토픽을 구독한 모든 클라이언트가, 어느 인스턴스에 붙어 있든 수신한다.
     */
    void broadcast(String destination, Object payload);

    /**
     * 특정 사용자 개인 큐로 전송한다. (예: destination {@code /queue/notifications})
     * {@code user} 는 STOMP Principal.getName() (= Member.email) 기준이며,
     * 그 사용자가 연결된 인스턴스에서만 실제로 전달된다.
     */
    void sendToUser(String user, String destination, Object payload);
}
