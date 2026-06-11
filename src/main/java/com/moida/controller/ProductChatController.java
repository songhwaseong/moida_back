package com.moida.controller;

import com.moida.common.request.ProductChatMessageRequest;
import com.moida.common.request.ProductChatRoomStatusRequest;
import com.moida.common.response.ApiResponse;
import com.moida.common.response.ProductChatMessageResponse;
import com.moida.common.response.ProductChatMessagesResponse;
import com.moida.common.response.ProductChatRoomResponse;
import com.moida.config.realtime.RealtimeMessagePublisher;
import com.moida.domain.audit.AdminActionLogService;
import com.moida.domain.chat.ProductChatService;
import com.moida.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ProductChatController {

    private final ProductChatService productChatService;
    private final RealtimeMessagePublisher realtimePublisher;
    private final AdminActionLogService adminActionLogService;

    // 상품/경매 상세에서 사용할 최근 채팅 이력을 조회한다.
    @GetMapping("/api/products/{productId}/chat/messages")
    public ResponseEntity<ApiResponse<ProductChatMessagesResponse>> getProductChatMessages(
            @PathVariable Long productId,
            @RequestParam(required = false) Integer size,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long memberId = userDetails == null ? null : userDetails.getMemberId();
        return ResponseEntity.ok(ApiResponse.success(productChatService.getMessages(productId, memberId, size)));
    }

    // REST 전송 fallback 경로다. 저장 후 STOMP와 같은 토픽으로 브로드캐스트한다.
    @PostMapping("/api/products/{productId}/chat/messages")
    public ResponseEntity<ApiResponse<ProductChatMessageResponse>> createProductChatMessage(
            @PathVariable Long productId,
            @Valid @RequestBody ProductChatMessageRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        ProductChatMessageResponse message = productChatService.createMessage(
                productId,
                userDetails.getMemberId(),
                request
        );
        realtimePublisher.broadcast("/topic/products/" + productId + "/chat", message);
        return ResponseEntity.ok(ApiResponse.success(message, "Message has been created."));
    }

    // 관리자 채팅방 목록과 모더레이션 기능이다.
    @GetMapping("/api/admin/chat/rooms")
    public ResponseEntity<ApiResponse<List<ProductChatRoomResponse>>> getAdminChatRooms() {
        adminActionLogService.recordView(
                "ADMIN_CHAT_ROOM_VIEW",
                "PRODUCT_CHAT_ROOM",
                adminActionLogService.fields("view", "rooms")
        );
        return ResponseEntity.ok(ApiResponse.success(productChatService.getAdminRooms()));
    }

    @PatchMapping("/api/admin/chat/rooms/{roomId}/status")
    public ResponseEntity<ApiResponse<ProductChatRoomResponse>> changeAdminChatRoomStatus(
            @PathVariable Long roomId,
            @Valid @RequestBody ProductChatRoomStatusRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(productChatService.changeRoomStatus(roomId, request.getStatus())));
    }

    @DeleteMapping("/api/admin/chat/messages/{messageId}")
    public ResponseEntity<ApiResponse<ProductChatMessageResponse>> hideAdminChatMessage(
            @PathVariable Long messageId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.success(productChatService.hideMessage(messageId, userDetails.getMemberId())));
    }
}
