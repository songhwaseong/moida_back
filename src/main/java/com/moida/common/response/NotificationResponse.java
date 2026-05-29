package com.moida.common.response;

import com.moida.domain.notification.Notification;

import java.time.format.DateTimeFormatter;

public record NotificationResponse(
        Long id,
        String type,
        NotificationCategory category,
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
                NotificationCategory.from(notification.getType()),
                notification.getTitle(),
                notification.getContent(),
                notification.getLinkUrl(),
                Boolean.TRUE.equals(notification.getIsRead()),
                notification.getCreatedAt() == null ? null : notification.getCreatedAt().format(FORMATTER)
        );
    }

    public enum NotificationCategory {
        BID,
        PRICE,
        CHAT,
        TRADE,
        MARKETING,
        SYSTEM;

        // 세부 이벤트 타입은 알림 탭에서 쓰는 상위 카테고리로 묶어 내려줍니다.
        private static NotificationCategory from(Notification.NotificationType type) {
            return switch (type) {
                case BID_PLACED, BID_OUTBID, AUCTION_WON, AUCTION_LOST, AUCTION_ENDED -> BID;
                case CHAT_MESSAGE -> CHAT;
                default -> SYSTEM;
            };
        }
    }
}
