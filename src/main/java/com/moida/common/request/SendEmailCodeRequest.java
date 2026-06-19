package com.moida.common.request;

import com.moida.domain.auth.VerificationPurpose;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class SendEmailCodeRequest {
    @NotBlank
    @Email
    private String email;

    @NotNull
    private VerificationPurpose purpose;
}
