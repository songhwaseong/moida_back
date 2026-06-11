package com.moida.common.response;

import com.moida.domain.auction.Auction;
import com.moida.domain.auction.AuctionStatus;
import com.moida.domain.auction.Bid;
import com.moida.domain.product.ProductCondition;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.function.UnaryOperator;

public record MyBidResponse(
        Long id,
        Long productId,
        String productNo,
        String auctionNo,
        String name,
        String image,
        String category,
        String condition,
        Long myBidAmount,
        Long currentPrice,
        Integer bidCount,
        Long timeLeft,
        Boolean isLive,
        String status,
        String bidTime
) {
    public static MyBidResponse from(Bid bid) {
        return from(bid, UnaryOperator.identity());
    }

    public static MyBidResponse from(Bid bid, UnaryOperator<String> imageUrlResolver) {
        Auction auction = bid.getAuction();
        boolean live = auction.getStatus() == AuctionStatus.LIVE
                && LocalDateTime.now().isBefore(auction.getEndAt());
        boolean won = auction.getWinner() != null
                && auction.getWinner().getId().equals(bid.getBidder().getId());

        String status = live ? "BIDDING" : (won ? "WON" : "FAILED");

        return new MyBidResponse(
                bid.getId(),
                auction.getProduct().getId(),
                auction.getProduct().getProductNo(),
                auction.getAuctionNo(),
                auction.getProduct().getName(),
                imageUrlResolver.apply(auction.getProduct().getMainImageUrl()),
                auction.getProduct().getCategory().getName(),
                conditionLabel(auction.getProduct().getCondition()),
                bid.getAmount(),
                auction.getCurrentPrice(),
                auction.getBidCount(),
                Math.max(0, Duration.between(LocalDateTime.now(), auction.getEndAt()).getSeconds()),
                live,
                status,
                timeAgo(bid.getCreatedAt())
        );
    }

    private static String conditionLabel(ProductCondition condition) {
        return condition.name() + "급";
    }

    private static String timeAgo(LocalDateTime createdAt) {
        if (createdAt == null) return "방금 전";

        Duration duration = Duration.between(createdAt, LocalDateTime.now());
        long minutes = Math.max(0, duration.toMinutes());
        if (minutes < 1) return "방금 전";
        if (minutes < 60) return minutes + "분 전";

        long hours = duration.toHours();
        if (hours < 24) return hours + "시간 전";

        return duration.toDays() + "일 전";
    }
}
