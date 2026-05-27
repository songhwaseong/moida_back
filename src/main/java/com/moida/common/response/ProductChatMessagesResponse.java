package com.moida.common.response;

import com.moida.domain.chat.ProductChatRoomStatus;

import java.util.List;

public record ProductChatMessagesResponse(
        ProductChatRoomStatus roomStatus,
        List<ProductChatMessageResponse> messages
) {
}
