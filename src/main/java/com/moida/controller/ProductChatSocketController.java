package com.moida.controller;

import com.moida.common.exception.BusinessException;
import com.moida.common.exception.ErrorCode;
import com.moida.common.request.ProductChatMessageRequest;
import com.moida.common.response.ProductChatMessageResponse;
import com.moida.config.realtime.RealtimeMessagePublisher;
import com.moida.domain.chat.ProductChatService;
import com.moida.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

import java.security.Principal;

@Controller
@Validated
@RequiredArgsConstructor
public class ProductChatSocketController {

    private final ProductChatService productChatService;
    private final RealtimeMessagePublisher realtimePublisher;

    // 상품 상세 채팅 입력창에서 사용하는 STOMP 메시지 전송 엔드포인트다.
    @MessageMapping("/products/{productId}/chat")
    public void createProductChatMessage(
            @DestinationVariable Long productId,
            @Valid @Payload ProductChatMessageRequest request,
            Principal principal
    ) {
        CustomUserDetails userDetails = resolveUser(principal);
        ProductChatMessageResponse message = productChatService.createMessage(
                productId,
                userDetails.getMemberId(),
                request
        );
        realtimePublisher.broadcast("/topic/products/" + productId + "/chat", message);
    }

    // 인바운드 채널 인터셉터가 STOMP JWT 헤더를 검증해 Principal로 넣어준다.
    private CustomUserDetails resolveUser(Principal principal) {
        if (!(principal instanceof Authentication authentication)
                || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return userDetails;
    }
}
