package com.moida.common.request;

public record AdminSettlementStatusUpdateRequest(
        String status,
        String reason
) {
}
