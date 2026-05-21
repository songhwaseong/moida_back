package com.moida.common.response;

import com.moida.domain.auction.Bid;

import java.time.Duration;
import java.time.LocalDateTime;

public record BidHistoryResponse(
        Long id,
        String user,
        String memberNo,
        Long amount,
        String time
) {
    // 경매 상세 화면의 입찰 이력 리스트에 필요한 최소 정보만 내려준다.
    // bidder는 지연 로딩 대상이라 Repository에서 fetch join으로 함께 조회한다.
    public static BidHistoryResponse from(Bid bid) {
        return new BidHistoryResponse(
                bid.getId(),
                bid.getBidder().getName(),
                bid.getBidder().getMemberNo(),
                bid.getAmount(),
                timeAgo(bid.getCreatedAt())
        );
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
