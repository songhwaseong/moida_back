package com.moida.common.response;

import com.moida.domain.notification.NotificationSetting;

public record NotificationSettingResponse(
        boolean bidEnabled,
        boolean priceEnabled,
        boolean chatEnabled,
        boolean tradeEnabled,
        boolean marketingEnabled
) {
    public static NotificationSettingResponse from(NotificationSetting setting) {
        return new NotificationSettingResponse(
                setting.isBidEnabled(),
                setting.isPriceEnabled(),
                setting.isChatEnabled(),
                setting.isTradeEnabled(),
                setting.isMarketingEnabled()
        );
    }
}
