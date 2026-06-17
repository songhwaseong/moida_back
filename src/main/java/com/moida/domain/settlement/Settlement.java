package com.moida.domain.settlement;

import com.moida.common.entity.BaseTimeEntity;
import com.moida.domain.auction.Auction;
import com.moida.domain.member.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "settlements",
        // 참고: auction_id 의 UNIQUE 제약은 @OneToOne 매핑상 Hibernate 가 자동 생성한다
        //       (운영 DB에 UK...auction_id 인덱스로 존재 확인됨). 이중 정산은 이 제약이 최종 방어한다.
        indexes = {
                @Index(name = "idx_settlement_status", columnList = "status"),
                @Index(name = "idx_settlement_seller", columnList = "seller_id")
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Settlement extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "settlement_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "auction_id", nullable = false)
    private Auction auction;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id", nullable = false)
    private Member seller;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "buyer_id", nullable = false)
    private Member buyer;

    @Column(name = "sales_amount", nullable = false)
    private Long salesAmount;            // 거래 금액

    @Column(name = "fee_amount", nullable = false)
    private Long feeAmount;              // 수수료

    @Column(name = "settled_amount", nullable = false)
    private Long settledAmount;          // 정산 금액 (sales - fee)

    @Column(name = "fee_rate", nullable = false)
    private Double feeRate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SettlementStatus status;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Builder
    private Settlement(Auction auction, Member seller, Member buyer,
                       Long salesAmount, Long feeAmount, Double feeRate) {
        this.auction = auction;
        this.seller = seller;
        this.buyer = buyer;
        this.salesAmount = salesAmount;
        this.feeAmount = feeAmount;
        this.settledAmount = salesAmount - feeAmount;
        this.feeRate = feeRate;
        this.status = SettlementStatus.PENDING;
    }

    public void markAsPaid() {
        this.status = SettlementStatus.PAID;
        this.paidAt = LocalDateTime.now();
    }

    public void payToSeller() {
        if (this.status == SettlementStatus.PAID) {
            throw new IllegalStateException("이미 정산 완료된 거래입니다.");
        }
        if (this.status == SettlementStatus.CANCELED) {
            throw new IllegalStateException("보류된 정산은 지급할 수 없습니다.");
        }
        if (this.settledAmount > 0) {
            this.seller.chargeBalance(this.settledAmount);
        }
        this.auction.markSettled();
        markAsPaid();
    }

    public void cancel() { this.status = SettlementStatus.CANCELED; }

    public enum SettlementStatus {
        PENDING,    // 정산 대기
        PAID,       // 정산 완료
        CANCELED    // 정산 취소
    }
}
