package com.moida.common.response;

public record PasswordlessLoginStartResponse(
        String requestToken,
        String sessionId,
        String oneTimeToken,
        String pushConnectorUrl,
        String pushConnectorToken,
        int expiresInSeconds
) {
}
