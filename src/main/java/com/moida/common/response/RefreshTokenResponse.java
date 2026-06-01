package com.moida.common.response;

import lombok.Getter;

/**
 * POST /api/auth/refresh 응답.
 * 새 accessToken 과 함께 rotation 된 refreshToken 도 같이 내려준다 — 클라이언트는 둘 다 교체 저장한다.
 *
 * Rotation 이유:
 *   - 매 갱신마다 refresh token 도 새것으로 발급해 과거 refresh 가 유출되어도 짧은 유효 창만 노출되게 함.
 *   - stateless 구조라 서버측에서 즉시 revoke 는 불가하지만 rotation 으로 위험 최소화.
 */
@Getter
public class RefreshTokenResponse {
    private final String accessToken;
    private final String refreshToken;

    public RefreshTokenResponse(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }
}
