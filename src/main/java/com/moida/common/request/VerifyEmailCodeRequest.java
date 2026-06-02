package com.moida.common.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class VerifyEmailCodeRequest {
    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String code;
}
