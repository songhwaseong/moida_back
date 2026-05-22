package com.moida.common.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 지갑 입출금(충전/출금) 요청 금액 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class WalletAmountRequest {

    /** 입출금 요청 금액 (최소 1,000원 이상) */
    @NotNull(message = "금액을 입력해주세요.")
    @Min(value = 1000, message = "금액은 1,000원 이상이어야 합니다.")
    private Long amount;
}
