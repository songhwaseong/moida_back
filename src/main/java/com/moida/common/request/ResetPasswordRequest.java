package com.moida.common.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class ResetPasswordRequest {
    @NotBlank
    @Email
    private String email;

    @NotBlank @Size(min = 8)
    private String newPassword;
}
