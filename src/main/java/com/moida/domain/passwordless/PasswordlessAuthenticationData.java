package com.moida.domain.passwordless;

record PasswordlessAuthenticationData(
        String servicePassword,
        String pushConnectorToken
) {
}
