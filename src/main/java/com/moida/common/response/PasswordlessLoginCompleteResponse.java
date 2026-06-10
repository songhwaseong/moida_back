package com.moida.common.response;

public record PasswordlessLoginCompleteResponse(
        String status,
        LoginResponse login
) {
}
