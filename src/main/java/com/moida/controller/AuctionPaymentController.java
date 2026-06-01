package com.moida.controller;

import com.moida.common.response.ApiResponse;
import com.moida.domain.auction.Auction;
import com.moida.domain.auction.AuctionCompletionService;
import com.moida.domain.auction.AuctionRepository;
import com.moida.common.exception.BusinessException;
import com.moida.common.exception.ErrorCode;
import com.moida.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 낙찰 후 결제 대기 상태의 경매에 대해 사용자가 직접 결제를 트리거하기 위한 엔드포인트.
 * - 충전 후 결제 버튼을 누르면 호출.
 * - 잔액 검증/차감/Settlement 생성/알림은 AuctionCompletionService 가 모두 처리한다.
 */
@RestController
@RequestMapping("/api/products/{productId}/payment")
@RequiredArgsConstructor
@Slf4j
public class AuctionPaymentController {

    private final AuctionCompletionService completionService;
    private final AuctionRepository auctionRepository;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> payForAuction(
            @PathVariable Long productId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("[AuctionPaymentController] POST /api/products/{}/payment memberId={}",
                productId, userDetails.getMemberId());

        Auction auction = auctionRepository.findByProductId(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUCTION_NOT_FOUND));

        completionService.payForWinningAuction(auction.getId(), userDetails.getMemberId());
        return ResponseEntity.ok(ApiResponse.success(null, "결제가 완료되었습니다."));
    }
}
