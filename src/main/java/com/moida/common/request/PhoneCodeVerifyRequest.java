package com.moida.common.request;

import com.moida.domain.auth.VerificationPurpose;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PhoneCodeVerifyRequest {

    @NotBlank(message = "휴대폰 번호는 필수 입력 사항입니다.")
    private String phone;

    @NotBlank(message = "인증번호는 필수 입력 사항입니다.")
    private String code;

    @NotNull
    private VerificationPurpose purpose;
}
