package com.moida.common.response;

import com.moida.domain.member.Member;
import com.moida.domain.wallet.MemberBankAccount;
import com.moida.domain.wallet.WalletTransaction;

import java.util.List;

/**
 * 회원의 지갑 및 계좌 정보, 최근 거래 내역을 포함하는 응답 DTO
 */
public record WalletResponse(
        Long balance,
        BankAccountResponse account,
        List<WalletTransactionResponse> transactions
) {
    /**
     * 회원 정보, 계좌 정보, 거래 내역 목록으로부터 WalletResponse DTO를 생성합니다.
     */
    public static WalletResponse of(Member member, MemberBankAccount account, List<WalletTransaction> transactions) {
        return new WalletResponse(
                member.getBalance(),
                BankAccountResponse.from(account),
                transactions.stream()
                        .map(WalletTransactionResponse::from)
                        .toList()
        );
    }
}
