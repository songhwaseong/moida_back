package com.moida.common.response;

import com.moida.domain.settlement.FeeRule;

/**
 * 수수료 정책 응답 DTO.
 *   - minFee : minAmount × feeRate / 100 — 화면 "기준금액 × 수수료율" 표시용 자동 계산값
 */
public record FeeRuleResponse(
        Long id,
        long minAmount,
        double feeRate,
        long minFee
) {
    public static FeeRuleResponse from(FeeRule rule) {
        long minFee = Math.round(rule.getMinAmount() * rule.getFeeRate() / 100.0);
        return new FeeRuleResponse(rule.getId(), rule.getMinAmount(), rule.getFeeRate(), minFee);
    }
}
