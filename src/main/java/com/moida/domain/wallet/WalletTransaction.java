package com.moida.domain.wallet;

import com.moida.common.entity.BaseTimeEntity;
import com.moida.common.exception.BusinessException;
import com.moida.common.exception.ErrorCode;
import com.moida.domain.member.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 지갑 충전 및 출금 거래 내역 정보를 저장하는 엔티티
 */
@Entity
@Getter
@Table(name = "wallet_transactions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WalletTransaction extends BaseTimeEntity {

    /** 거래 종류 (입금/출금) */
    public enum TransactionType {
        DEPOSIT,
        WITHDRAW
    }

    /** 거래 상태 (완료/대기/취소) */
    public enum TransactionStatus {
        COMPLETED,
        PENDING,
        CANCELED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long id;

    /** 거래가 일어난 지갑의 소유 회원 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    /** 거래 타입 (입금/출금) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type;

    /** 거래 처리 상태 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;

    /** 거래 금액 */
    @Column(nullable = false)
    private Long amount;

    /** 거래 상세 설명 */
    @Column(nullable = false, length = 200)
    private String description;

    @Builder
    private WalletTransaction(Member member, TransactionType type, TransactionStatus status,
                               Long amount, String description) {
        this.member = member;
        this.type = type;
        this.status = status;
        this.amount = amount;
        this.description = description;
    }

    /**
     * 가상계좌 입금 확인이 끝난 PENDING 입금 거래를 완료 처리합니다.
     */
    public void completeDeposit() {
        if (type != TransactionType.DEPOSIT || status != TransactionStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_WALLET_TRANSACTION);
        }
        this.status = TransactionStatus.COMPLETED;
        this.description = "가상계좌 입금 완료";
    }

    /**
     * 출금 처리가 끝난 PENDING 출금 거래를 완료 처리합니다.
     */
    public void completeWithdrawal() {
        if (type != TransactionType.WITHDRAW || status != TransactionStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_WALLET_TRANSACTION);
        }
        this.status = TransactionStatus.COMPLETED;
        this.description = "출금 처리 완료";
    }

    /**
     * 아직 처리되지 않은 입금 거래를 취소합니다.
     */
    public void cancelDeposit() {
        if (type != TransactionType.DEPOSIT || status != TransactionStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_WALLET_TRANSACTION);
        }
        this.status = TransactionStatus.CANCELED;
        this.description = "가상계좌 입금 요청 취소";
    }

    /**
     * 아직 처리되지 않은 출금 거래를 취소합니다.
     */
    public void cancelWithdrawal() {
        if (type != TransactionType.WITHDRAW || status != TransactionStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_WALLET_TRANSACTION);
        }
        this.status = TransactionStatus.CANCELED;
        this.description = "출금 신청 취소";
    }
}
