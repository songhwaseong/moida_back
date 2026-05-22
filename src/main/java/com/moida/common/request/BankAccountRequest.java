package com.moida.common.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 출금 계좌 등록 및 변경 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BankAccountRequest {

    /** 은행명 */
    @NotBlank(message = "은행을 입력해주세요.")
    private String bank;

    /** 계좌번호 (숫자 10~14자리) */
    @NotBlank(message = "계좌번호를 입력해주세요.")
    @Pattern(regexp = "\\d{10,14}", message = "계좌번호는 숫자 10~14자리여야 합니다.")
    private String accountNumber;

    /** 예금주명 */
    @NotBlank(message = "예금주를 입력해주세요.")
    private String holder;
}
