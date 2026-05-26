package com.moida.common.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// 네이버 사용자 정보 API(https://openapi.naver.com/v1/nid/me) 응답
// 실제 유저 정보는 "response" 블록 안에 중첩되어 있음
@JsonIgnoreProperties(ignoreUnknown = true)
public record NaverUserResponse(
        String resultcode,
        String message,
        NaverUser response  // 실제 사용자 정보가 담긴 중첩 블록
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record NaverUser(
            String id,
            String email,
            String name,
            String nickname,
            String mobile         // 전화번호 (필요 시 활용)
    ) {}
}
