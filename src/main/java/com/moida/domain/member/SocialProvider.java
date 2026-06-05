package com.moida.domain.member;

public enum SocialProvider {
    KAKAO,
    NAVER,
    GOOGLE;

    public static SocialProvider from(String value) {
        return SocialProvider.valueOf(value.trim().toUpperCase());
    }
}
