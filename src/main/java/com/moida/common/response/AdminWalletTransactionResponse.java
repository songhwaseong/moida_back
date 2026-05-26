package com.moida.common.response;

import com.moida.domain.member.Member;
import com.moida.domain.wallet.MemberBankAccount;
import com.moida.domain.wallet.WalletTransaction;

import java.time.LocalDateTime;

/**
 * 관리자 지갑 요청 목록에서 사용하는 거래 내역 응답 DTO입니다.
 */
public record AdminWalletTransactionResponse(
        Long id,
        Long memberId,
        String memberNo,
        String memberName,
        String memberEmail,
        WalletTransaction.TransactionType type,
        WalletTransaction.TransactionStatus status,
        Long amount,
        String description,
        BankAccountResponse account,
        LocalDateTime createdAt
) {
    public static AdminWalletTransactionResponse of(WalletTransaction transaction, MemberBankAccount account) {
        Member member = transaction.getMember();
        return new AdminWalletTransactionResponse(
                transaction.getId(),
                member.getId(),
                member.getMemberNo(),
                member.getName(),
                member.getEmail(),
                transaction.getType(),
                transaction.getStatus(),
                transaction.getAmount(),
                transaction.getDescription(),
                BankAccountResponse.from(account),
                transaction.getCreatedAt()
        );
    }
}
