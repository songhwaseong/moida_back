package com.moida.common.request;

import com.moida.domain.auth.VerificationPurpose;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class VerifyEmailCodeRequest {
    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String code;

    @NotNull
    private VerificationPurpose purpose;
}
