package com.moida.domain.passwordless;

record PasswordlessRequestContext(
        Long memberId,
        String sessionId,
        String random,
        long expiresAt
) {
}
