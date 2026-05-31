package com.moida.common.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * POST /api/auth/refresh 요청 본문.
 * 클라이언트는 로그인 시 받아둔 refreshToken 을 그대로 보낸다.
 */
@Getter
@NoArgsConstructor
public class RefreshTokenRequest {
    @NotBlank
    private String refreshToken;
}
