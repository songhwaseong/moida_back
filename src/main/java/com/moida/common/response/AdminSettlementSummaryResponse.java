package com.moida.common.response;

/**
 * 관리자 정산 관리 화면 상단 요약 카드용 DTO.
 *   - totalSale : 총 거래금액 합계 (CANCELED 제외)
 *   - totalFee  : 총 수수료 수익 합계 (CANCELED 제외)
 *   - totalNet  : 총 정산 금액 합계 (CANCELED 제외)
 *   - pending   : 정산 대기 건수
 */
public record AdminSettlementSummaryResponse(
        long totalSale,
        long totalFee,
        long totalNet,
        long pending
) {
}
