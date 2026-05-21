package com.moida.common.response;

import com.moida.domain.auction.Bid;

import java.util.List;

public record BidResultResponse(
        Long currentPrice,
        Integer bidCount,
        Boolean isLive,
        List<BidHistoryResponse> bidHistory
) {
    public static BidResultResponse from(Long currentPrice, Integer bidCount, Boolean isLive, List<Bid> bids) {
        return new BidResultResponse(
                currentPrice,
                bidCount,
                isLive,
                bids.stream().map(BidHistoryResponse::from).toList()
        );
    }
}
