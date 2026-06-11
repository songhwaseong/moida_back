package com.moida.common.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Passwordless 평상시 해지(비밀번호 확인) 요청.
 * 일반 로그인(/auth/login)이 차단된 상태에서도 해지가 가능하도록
 * 로그인 세션 발급 없이 이메일+비밀번호만 직접 검증한다.
 */
public record PasswordlessWithdrawRequest(
        @NotBlank(message = "이메일을 입력해주세요.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email,

        @NotBlank(message = "비밀번호를 입력해주세요.")
        String password
) {
}
