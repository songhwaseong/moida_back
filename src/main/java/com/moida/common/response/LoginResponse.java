package com.moida.common.response;

import com.moida.domain.member.Member;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;

/**
 * 로그인 성공 응답.
 * accessToken: 1시간 (jwt.access-token-validity) — 일반 API 호출에 사용.
 * refreshToken: 14일 (jwt.refresh-token-validity) — access 만료 시 /api/auth/refresh 로 새 토큰 발급용.
 * 두 토큰 모두 stateless JWT — 서버측 저장소에 보관하지 않는다 (revoke 필요 시 stateful 로 전환).
 */
@Getter
public class LoginResponse {
    private String accessToken;
    @JsonIgnore
    private String refreshToken;
    private Long id;
    private String name;
    private String email;
    private String role;
    private boolean isNewUser;

    public LoginResponse(String accessToken, String refreshToken, Member member, boolean isNewUser) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.id = member.getId();
        this.name = member.getName();
        this.email = member.getEmail();
        this.role = member.getRole().name();
        this.isNewUser = isNewUser;
    }
}
