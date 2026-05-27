package com.moida.common.request;

import com.moida.domain.chat.ProductChatRoomStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class ProductChatRoomStatusRequest {

    @NotNull
    private ProductChatRoomStatus status;
}
