package com.moida.common.response;

/**
 * 관리자 상품 관리 화면 상단 통계 카드용 DTO.
 * (DELETED 상태는 집계에서 제외한다.)
 */
public record AdminProductStatsResponse(
        long total,      // 전체 상품
        long selling,    // 경매예정(SCHEDULED)
        long approving,  // 승인요청중(PENDING)
        long inBid,      // 경매중(LIVE)
        long hidden      // 숨김(HIDDEN)
) {
}
