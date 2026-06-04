package com.moida.common.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 경매 정책 수정 요청 DTO. 일/시간/분으로 받아 서버에서 총 분으로 환산한다.
 * null 필드는 0으로 간주한다.
 */
@Getter
@NoArgsConstructor
public class AuctionPolicyUpdateRequest {
    private Integer days;
    private Integer hours;
    private Integer minutes;

    /** 입력값(일/시간/분)을 총 분으로 환산. null 은 0 처리. */
    public int toTotalMinutes() {
        int d = days == null ? 0 : days;
        int h = hours == null ? 0 : hours;
        int m = minutes == null ? 0 : minutes;
        return d * 24 * 60 + h * 60 + m;
    }
}
