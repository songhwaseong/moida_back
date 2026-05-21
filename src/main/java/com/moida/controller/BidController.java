package com.moida.controller;

import com.moida.common.request.BidRequest;
import com.moida.common.response.ApiResponse;
import com.moida.common.response.BidResultResponse;
import com.moida.domain.auction.AuctionBidService;
import com.moida.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products/{productId}/bids")
@RequiredArgsConstructor
@Slf4j
public class BidController {

    private final AuctionBidService auctionBidService;

    @PostMapping
    public ResponseEntity<ApiResponse<BidResultResponse>> placeBid(
            @PathVariable Long productId,
            @Valid @RequestBody BidRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("[BidController] POST /api/products/{}/bids memberId={}, amount={}",
                productId, userDetails.getMemberId(), request.getAmount());
        BidResultResponse response = auctionBidService.placeProductBid(
                productId,
                userDetails.getMemberId(),
                request
        );
        return ResponseEntity.ok(ApiResponse.success(response, "입찰이 등록되었습니다."));
    }

    @PostMapping("/immediate")
    public ResponseEntity<ApiResponse<BidResultResponse>> buyNow(
            @PathVariable Long productId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("[BidController] POST /api/products/{}/bids/immediate memberId={}",
                productId, userDetails.getMemberId());
        BidResultResponse response = auctionBidService.buyNowProduct(
                productId,
                userDetails.getMemberId()
        );
        return ResponseEntity.ok(ApiResponse.success(response, "즉시낙찰이 완료되었습니다."));
    }
}
