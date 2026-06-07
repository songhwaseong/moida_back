package com.moida.common.request;

public record AdminAuditEventRequest(
        String actionType,
        String reason
) {
}
