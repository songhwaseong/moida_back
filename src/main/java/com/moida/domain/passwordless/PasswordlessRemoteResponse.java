package com.moida.domain.passwordless;

import com.fasterxml.jackson.databind.JsonNode;

record PasswordlessRemoteResponse(
        boolean success,
        String code,
        String message,
        JsonNode data
) {
}
