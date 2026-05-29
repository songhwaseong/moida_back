package com.moida.common.request;

public record NotificationSettingRequest(
        boolean bidEnabled,
        boolean priceEnabled,
        boolean chatEnabled,
        boolean tradeEnabled,
        boolean marketingEnabled
) {
}
