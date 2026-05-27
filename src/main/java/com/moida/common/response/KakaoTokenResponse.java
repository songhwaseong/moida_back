package com.moida.common.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

// 카카오 토큰 발급 API(https://kauth.kakao.com/oauth/token) 응답에서
// access_token 하나만 필요하므로 나머지 필드는 ignoreUnknown으로 무시
@JsonIgnoreProperties(ignoreUnknown = true)
public class KakaoTokenResponse {
    @JsonProperty("access_token") // 카카오 응답 키 이름이 snake_case라 매핑 필요
    private String accessToken;

    public String getAccessToken() {
        return accessToken;
    }
}
