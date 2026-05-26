package com.moida.common.response;

import com.moida.domain.wallet.WalletTransaction;

import java.time.LocalDateTime;

/**
 * 지갑 거래 내역 단건 정보 응답 DTO
 */
public record WalletTransactionResponse(
        Long id,
        WalletTransaction.TransactionType type,
        WalletTransaction.TransactionStatus status,
        Long amount,
        String description,
        LocalDateTime createdAt
) {
    /**
     * WalletTransaction 엔티티로부터 WalletTransactionResponse DTO를 생성합니다.
     */
    public static WalletTransactionResponse from(WalletTransaction transaction) {
        return new WalletTransactionResponse(
                transaction.getId(),
                transaction.getType(),
                transaction.getStatus(),
                transaction.getAmount(),
                transaction.getDescription(),
                transaction.getCreatedAt()
        );
    }
}
