package com.moida.common.response;

import com.moida.domain.auction.AuctionPolicy;

/**
 * 경매 정책 응답 DTO.
 * 저장은 총 분(durationMinutes) 단위지만, 화면 표시를 위해 일/시간/분으로도 분해해 함께 내려준다.
 */
public record AuctionPolicyResponse(
        int durationMinutes,
        int days,
        int hours,
        int minutes
) {
    public static AuctionPolicyResponse of(int durationMinutes) {
        int days = durationMinutes / (24 * 60);
        int hours = (durationMinutes % (24 * 60)) / 60;
        int minutes = durationMinutes % 60;
        return new AuctionPolicyResponse(durationMinutes, days, hours, minutes);
    }

    public static AuctionPolicyResponse from(AuctionPolicy policy) {
        return of(policy.getDurationMinutes());
    }
}
