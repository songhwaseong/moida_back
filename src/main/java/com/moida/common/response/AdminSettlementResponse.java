package com.moida.common.response;

import com.moida.domain.member.Member;
import com.moida.domain.settlement.Settlement;

import java.time.format.DateTimeFormatter;

/**
 * 관리자 정산 관리 테이블 행(row)용 DTO.
 *   - status : PAID/PENDING/CANCELED enum 이름 그대로 (프론트가 한글 라벨로 매핑)
 *   - 현재 프로젝트는 경매-only 이므로 type 은 항상 "경매" 로 내려준다.
 */
public record AdminSettlementResponse(
        Long id,
        String sellerNo,
        String buyerNo,
        String productName,
        String type,
        long saleAmount,
        double feeRate,
        long feeAmount,
        long netAmount,
        String status,
        String transactionDate,
        String settlementDate
) {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    public static AdminSettlementResponse from(Settlement s) {
        Member seller = s.getSeller();
        Member buyer = s.getBuyer();
        return new AdminSettlementResponse(
                s.getId(),
                seller == null || seller.getMemberNo() == null ? "" : seller.getMemberNo(),
                buyer == null || buyer.getMemberNo() == null ? "" : buyer.getMemberNo(),
                s.getAuction().getProduct().getName(),
                "경매",
                s.getSalesAmount(),
                s.getFeeRate(),
                s.getFeeAmount(),
                s.getSettledAmount(),
                s.getStatus().name(),
                s.getCreatedAt() == null ? "" : s.getCreatedAt().format(DATE_FMT),
                s.getPaidAt() == null ? null : s.getPaidAt().format(DATE_FMT)
        );
    }
}
