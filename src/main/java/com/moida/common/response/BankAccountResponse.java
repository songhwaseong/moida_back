package com.moida.common.response;

import com.moida.domain.wallet.MemberBankAccount;

/**
 * 회원 출금 계좌 정보 응답 DTO
 */
public record BankAccountResponse(
        String bank,
        String accountNumber,
        String holder,
        boolean verified
) {
    /**
     * MemberBankAccount 엔티티로부터 BankAccountResponse DTO를 생성합니다.
     */
    public static BankAccountResponse from(MemberBankAccount account) {
        if (account == null) return null;
        return new BankAccountResponse(
                account.getBank(),
                account.getAccountNumber(),
                account.getHolder(),
                account.isVerified()
        );
    }
}
