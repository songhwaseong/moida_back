package com.moida.common.request;

import jakarta.validation.constraints.NotBlank;

public record PasswordlessRequestTokenRequest(
        @NotBlank(message = "인증 요청 정보가 없습니다.")
        String requestToken
) {
}
