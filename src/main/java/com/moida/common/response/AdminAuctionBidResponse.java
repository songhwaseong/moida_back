package com.moida.common.response;

import com.moida.domain.auction.Bid;
import com.moida.domain.member.Member;

import java.time.format.DateTimeFormatter;

/**
 * 관리자 경매 관리 화면의 입찰 내역 row.
 *   - memberNo : 회원번호(없으면 빈 문자열)
 *   - time     : 입찰 시각 (yyyy.MM.dd HH:mm)
 */
public record AdminAuctionBidResponse(
        Long id,
        String user,
        String memberNo,
        long amount,
        String time,
        boolean isWinning
) {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    public static AdminAuctionBidResponse from(Bid bid) {
        Member bidder = bid.getBidder();
        String name = bidder == null ? "-"
                : (bidder.getNickname() != null && !bidder.getNickname().isBlank()
                    ? bidder.getNickname() : bidder.getName());
        String memberNo = bidder == null || bidder.getMemberNo() == null ? "" : bidder.getMemberNo();

        return new AdminAuctionBidResponse(
                bid.getId(),
                name,
                memberNo,
                bid.getAmount(),
                bid.getCreatedAt() == null ? "" : bid.getCreatedAt().format(FMT),
                Boolean.TRUE.equals(bid.getIsWinning())
        );
    }
}
