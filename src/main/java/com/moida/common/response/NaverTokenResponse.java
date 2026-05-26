package com.moida.common.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

// 네이버 토큰 발급 API(https://nid.naver.com/oauth2.0/token) 응답
@JsonIgnoreProperties(ignoreUnknown = true)
public class NaverTokenResponse {

    @JsonProperty("access_token") // 카카오와 동일하게 snake_case 매핑
    private String accessToken;

    public String getAccessToken() {
        return accessToken;
    }
}