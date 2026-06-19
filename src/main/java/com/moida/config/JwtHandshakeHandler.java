package com.moida.config;

import com.moida.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

/**
 * WebSocket 핸드셰이크(HTTP 업그레이드) 시점에 JWT 로 인증하고, 그 결과를
 * {@code session.getPrincipal()} 로 세팅하는 핸드셰이크 핸들러.
 *
 * 왜 핸드셰이크에서 하나(채널 인터셉터로는 부족한 이유):
 *   - Spring 의 DefaultSimpUserRegistry 는 SessionConnectedEvent 의 user(=session.getPrincipal())
 *     를 보고 "이 이메일 = 이 세션" 매핑을 만든다.
 *   - 채널 인터셉터(StompHeaderAccessor.setUser)로 CONNECT 프레임에 user 를 넣어도
 *     session.getPrincipal() 자체는 바뀌지 않아서, registry 가 비고 convertAndSendToUser 가
 *     대상 세션을 못 찾는다(개인 알림 = user-destination 전달 실패).
 *   - 핸드셰이크에서 Principal 을 세팅하면 이후 모든 프레임(SUBSCRIBE/SEND 포함)에 자동 전파되고
 *     registry 도 정상 채워진다.
 *
 * 토큰 전달:
 *   - 핸드셰이크는 순수 HTTP 요청이라 STOMP connectHeaders 를 볼 수 없으므로,
 *     클라이언트가 WS URL 에 {@code ?token=<JWT>} 쿼리파라미터로 붙여 보낸다.
 *   - JWT 는 base64url 이라 쿼리스트링에 그대로 실어도 안전하다(특수문자 없음).
 */
@Component
@RequiredArgsConstructor
public class JwtHandshakeHandler extends DefaultHandshakeHandler {

    private static final String TOKEN_PARAM = "token=";

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = extractToken(request);
        // access 토큰만 인증에 사용한다(refresh 토큰으로 WS 를 연결하는 것을 차단).
        if (StringUtils.hasText(token)
                && jwtTokenProvider.validateToken(token)
                && jwtTokenProvider.isTokenType(token, JwtTokenProvider.TYPE_ACCESS)) {
            return jwtTokenProvider.getAuthentication(token);
        }
        // 토큰이 없거나 무효하면 익명 세션으로 둔다(예: 토큰을 안 붙인 다른 용도의 연결).
        return super.determineUser(request, wsHandler, attributes);
    }

    private String extractToken(ServerHttpRequest request) {
        String query = request.getURI().getQuery();
        if (!StringUtils.hasText(query)) {
            return null;
        }
        for (String param : query.split("&")) {
            if (param.startsWith(TOKEN_PARAM)) {
                return param.substring(TOKEN_PARAM.length());
            }
        }
        return null;
    }
}
