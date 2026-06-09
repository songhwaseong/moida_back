package com.moida.common.response;

public record PasswordlessRegistrationStartResponse(
        String qr,
        String corpId,
        String registerKey,
        int terms,
        String serverUrl,
        String userId,
        String pushConnectorUrl,
        String pushConnectorToken,
        int expiresInSeconds
) {
}
