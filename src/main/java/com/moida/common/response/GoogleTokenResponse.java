package com.moida.common.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

// 구글 토큰 발급 API(https://oauth2.googleapis.com/token) 응답
@JsonIgnoreProperties(ignoreUnknown = true)
public class GoogleTokenResponse {

    @JsonProperty("access_token")
    private String accessToken;

    public String getAccessToken() {
        return accessToken;
    }
}
