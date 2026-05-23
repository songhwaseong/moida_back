package com.moida.common.response;

import com.moida.domain.chat.ProductChatRoom;
import com.moida.domain.chat.ProductChatRoomStatus;

import java.time.format.DateTimeFormatter;

public record ProductChatRoomResponse(
        Long id,
        Long productId,
        String productName,
        ProductChatRoomStatus status,
        String lastMessage,
        long messageCount,
        String updatedAt
) {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    public static ProductChatRoomResponse from(ProductChatRoom room, long messageCount) {
        return new ProductChatRoomResponse(
                room.getId(),
                room.getProduct().getId(),
                room.getProduct().getName(),
                room.getStatus(),
                room.getLastMessage(),
                messageCount,
                room.getUpdatedAt().format(FORMATTER)
        );
    }
}
