package com.moida.common.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

// 구글 사용자 정보 API(https://www.googleapis.com/oauth2/v2/userinfo) 응답
// 카카오/네이버와 달리 중첩 구조 없이 최상위에 바로 유저 정보가 있음
@JsonIgnoreProperties(ignoreUnknown = true)
public record GoogleUserResponse(
        String id,
        String email,
        String name,
        @JsonProperty("verified_email") boolean verifiedEmail, // snake_case 매핑
        String picture                                          // 프로필 이미지 URL
) {}