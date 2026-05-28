package com.moida.common.request;

public record DeactivateAccountRequest(
        String password,
        String confirmationText,
        String reasonCode,
        String reasonDetail
) {
}
