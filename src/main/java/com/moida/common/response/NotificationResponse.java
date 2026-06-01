package com.moida.common.response;

import com.moida.domain.notification.Notification;

import java.time.format.DateTimeFormatter;

public record NotificationResponse(
        Long id,
        String type,
        String category,
        String title,
        String content,
        String linkUrl,
        boolean read,
        String createdAt
) {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType().name(),
                toCategory(notification.getType()),
                notification.getTitle(),
                notification.getContent(),
                notification.getLinkUrl(),
                Boolean.TRUE.equals(notification.getIsRead()),
                notification.getCreatedAt() == null ? null : notification.getCreatedAt().format(FORMATTER)
        );
    }

    private static String toCategory(Notification.NotificationType type) {
        return switch (type) {
            case BID_PLACED, BID_OUTBID -> "BID";
            case CHAT_MESSAGE -> "CHAT";
            case AUCTION_WON, AUCTION_LOST, AUCTION_ENDED -> "TRADE";
            case NOTICE, SANCTION, INQUIRY_NEW, INQUIRY_ANSWERED -> "SYSTEM";
        };
    }
}
