package com.moida.common.response;

import com.moida.common.request.NotificationSettingRequest;

public record NotificationSettingResponse(
        boolean bidEnabled,
        boolean priceEnabled,
        boolean chatEnabled,
        boolean tradeEnabled,
        boolean marketingEnabled
) {

    public static NotificationSettingResponse defaults() {
        return new NotificationSettingResponse(true, true, true, true, false);
    }

    public static NotificationSettingResponse from(NotificationSettingRequest request) {
        return new NotificationSettingResponse(
                request.bidEnabled(),
                request.priceEnabled(),
                request.chatEnabled(),
                request.tradeEnabled(),
                request.marketingEnabled()
        );
    }
}
