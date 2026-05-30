package com.moida.common.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 수수료 정책 수정 요청 DTO. null 인 필드는 변경하지 않는다.
 */
@Getter
@NoArgsConstructor
public class FeeRuleUpdateRequest {
    private Long minAmount;
    private Double feeRate;
}
