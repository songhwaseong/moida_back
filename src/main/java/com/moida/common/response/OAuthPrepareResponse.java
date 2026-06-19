package com.moida.common.response;

public record OAuthPrepareResponse(
        String state,
        String codeChallenge,
        boolean pkce
) {
}
