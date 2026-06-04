package com.moida.common.response;

import com.moida.domain.auction.Auction;
import com.moida.domain.auction.AuctionStatus;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 관리자 경매 관리 화면 행(row)용 응답 DTO.
 * 카드/테이블이 즉시 그릴 수 있는 평탄한 구조로 정리한다.
 *   - timeLeft : 종료까지 남은 초 (음수면 0). LIVE 가 아니어도 endAt 기준으로 계산.
 *   - status   : ProductStatus 와 별개로 Auction 자체 상태 enum 이름 (LIVE/SUCCESS/FAILED/CANCELED/READY)
 */
public record AdminAuctionResponse(
        Long id,
        String auctionNo,
        Long productId,
        String productName,
        String category,
        long currentPrice,
        long startPrice,
        int bidCount,
        long timeLeft,
        String status,
        // 낙찰(SUCCESS) 이후 결제·배송 진행 단계. 경매관리 화면의 '진행상태' 컬럼/필터에 사용한다.
        // 배송 단계 진입 전이면 null.
        String deliveryStatus,
        String startAt,
        String endAt
) {
    public static AdminAuctionResponse from(Auction auction) {
        long timeLeftSec = Math.max(0,
                Duration.between(LocalDateTime.now(), auction.getEndAt()).getSeconds());
        return new AdminAuctionResponse(
                auction.getId(),
                auction.getAuctionNo(),
                auction.getProduct().getId(),
                auction.getProduct().getName(),
                auction.getProduct().getCategory().getName(),
                auction.getCurrentPrice(),
                auction.getStartPrice(),
                auction.getBidCount(),
                timeLeftSec,
                statusName(auction.getStatus()),
                auction.getDeliveryStatus() == null ? null : auction.getDeliveryStatus().name(),
                auction.getStartAt().toString(),
                auction.getEndAt().toString()
        );
    }

    private static String statusName(AuctionStatus status) {
        return status == null ? "READY" : status.name();
    }
}
