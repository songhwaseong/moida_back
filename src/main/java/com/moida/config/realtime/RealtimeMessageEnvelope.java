package com.moida.config.realtime;

/**
 * Redis 채널로 인스턴스 간 전달되는 실시간 메시지 봉투(envelope).
 *
 * <p>{@code payload} 는 publish 시 Spring 의 ObjectMapper 로 직렬화되고,
 * 구독 측에서는 일반 객체(Map 등)로 역직렬화된 뒤 STOMP 컨버터가 다시 JSON 으로 직렬화한다.
 * 클라이언트는 JSON 필드만 읽으므로 타입 정보 없이도 동일한 형태로 전달된다.
 */
public record RealtimeMessageEnvelope(
        Kind kind,
        String destination,
        String user,
        Object payload
) {
    public enum Kind {
        /** 공개 토픽 브로드캐스트 (convertAndSend) */
        BROADCAST,
        /** 사용자 개인 큐 (convertAndSendToUser) */
        USER
    }

    public static RealtimeMessageEnvelope broadcast(String destination, Object payload) {
        return new RealtimeMessageEnvelope(Kind.BROADCAST, destination, null, payload);
    }

    public static RealtimeMessageEnvelope toUser(String user, String destination, Object payload) {
        return new RealtimeMessageEnvelope(Kind.USER, destination, user, payload);
    }
}
