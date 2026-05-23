package com.moida.common.response;

import com.moida.domain.chat.ProductChatMessage;
import com.moida.domain.chat.ProductChatRoomStatus;

import java.time.format.DateTimeFormatter;

public record ProductChatMessageResponse(
        Long id,
        Long roomId,
        Long productId,
        ProductChatRoomStatus roomStatus,
        Long senderId,
        String senderName,
        boolean seller,
        boolean mine,
        String content,
        boolean deleted,
        String createdAt
) {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    public static ProductChatMessageResponse from(ProductChatMessage message, Long currentMemberId) {
        Long senderId = message.getSender().getId();
        Long sellerId = message.getRoom().getProduct().getSeller().getId();
        boolean deleted = Boolean.TRUE.equals(message.getIsDeleted());

        return new ProductChatMessageResponse(
                message.getId(),
                message.getRoom().getId(),
                message.getRoom().getProduct().getId(),
                message.getRoom().getStatus(),
                senderId,
                message.getSender().getName(),
                senderId.equals(sellerId),
                currentMemberId != null && currentMemberId.equals(senderId),
                deleted ? "관리자에 의해 삭제된 메시지입니다." : message.getContent(),
                deleted,
                message.getCreatedAt().format(FORMATTER)
        );
    }

    public ProductChatMessageResponse withMine(Long currentMemberId) {
        return new ProductChatMessageResponse(
                id,
                roomId,
                productId,
                roomStatus,
                senderId,
                senderName,
                seller,
                currentMemberId != null && currentMemberId.equals(senderId),
                content,
                deleted,
                createdAt
        );
    }
}
