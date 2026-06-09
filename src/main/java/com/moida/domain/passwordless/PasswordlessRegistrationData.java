package com.moida.domain.passwordless;

record PasswordlessRegistrationData(
        String qr,
        String corpId,
        String registerKey,
        int terms,
        String serverUrl,
        String userId,
        String pushConnectorUrl,
        String pushConnectorToken
) {
}
