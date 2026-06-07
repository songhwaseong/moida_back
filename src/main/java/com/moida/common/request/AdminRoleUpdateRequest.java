package com.moida.common.request;

public record AdminRoleUpdateRequest(
        String role,
        String reason
) {
}
