package com.moida.domain.auction;

import com.moida.common.entity.BaseTimeEntity;
import com.moida.domain.member.Member;
import com.moida.domain.product.Product;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "auctions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_auction_no", columnNames = "auction_no"),
                @UniqueConstraint(name = "uk_auction_product", columnNames = "product_id")
        },
        indexes = {
                @Index(name = "idx_auction_status", columnList = "status"),
                @Index(name = "idx_auction_end", columnList = "end_at")
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Auction extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "auction_id")
    private Long id;

    @Column(name = "auction_no", nullable = false, length = 30)
    private String auctionNo;            // 경매번호 (e.g. AUC-20260501-0001)

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "start_price", nullable = false)
    private Long startPrice;             // 시작가

    @Column(name = "current_price", nullable = false)
    private Long currentPrice;           // 현재 최고 입찰가

    @Column(name = "immediate_price")
    private Long immediatePrice;         // 즉시낙찰가 (선택)

    @Column(name = "min_bid_unit", nullable = false)
    private Long minBidUnit;             // 최소 호가 단위

    @Column(name = "bid_count", nullable = false)
    private Integer bidCount;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuctionStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_id")
    private Member winner;               // 낙찰자

    @Column(name = "winning_price")
    private Long winningPrice;

    @Column(name = "settled_at")
    private LocalDateTime settledAt;     // 정산 완료 시각

    @Column(name = "payment_deadline")
    private LocalDateTime paymentDeadline;  // 낙찰 후 결제 마감 시각 (AWAITING_PAYMENT 상태에서만 유효)

    @Builder
    private Auction(String auctionNo, Product product, Long startPrice, Long immediatePrice,
                    Long minBidUnit, LocalDateTime startAt, LocalDateTime endAt) {
        this.auctionNo = auctionNo;
        this.product = product;
        this.startPrice = startPrice;
        this.currentPrice = startPrice;
        this.immediatePrice = immediatePrice;
        this.minBidUnit = minBidUnit;
        this.startAt = startAt;
        this.endAt = endAt;
        this.bidCount = 0;
        this.status = AuctionStatus.READY;
    }

    // ===== Domain Methods =====
    public void start() { this.status = AuctionStatus.LIVE; }

    public void placeBid(long amount) {
        if (this.status != AuctionStatus.LIVE) {
            throw new IllegalStateException("진행 중인 경매가 아닙니다.");
        }
        if (amount <= this.currentPrice) {
            throw new IllegalArgumentException("현재가보다 높게 입찰해주세요.");
        }
        this.currentPrice = amount;
        this.bidCount++;
    }

    public void close(Member winner, Long winningPrice) {
        if (winner != null) {
            this.status = AuctionStatus.SUCCESS;
            this.winner = winner;
            this.winningPrice = winningPrice;
        } else {
            this.status = AuctionStatus.FAILED;
        }
    }

    /**
     * 낙찰자 잔액 부족으로 결제 대기 상태 전환.
     * winner/winningPrice 는 확정되어 들어가고, paymentDeadline 까지 결제하지 않으면 FAILED 로 전환된다.
     */
    public void markAwaitingPayment(Member winner, Long winningPrice, LocalDateTime paymentDeadline) {
        this.status = AuctionStatus.AWAITING_PAYMENT;
        this.winner = winner;
        this.winningPrice = winningPrice;
        this.paymentDeadline = paymentDeadline;
    }

    /** AWAITING_PAYMENT → SUCCESS (결제 완료). 멱등성 호출 방지를 위해 상태 가드. */
    public void completePayment() {
        if (this.status != AuctionStatus.AWAITING_PAYMENT) {
            throw new IllegalStateException("결제 대기 상태가 아닙니다.");
        }
        this.status = AuctionStatus.SUCCESS;
        this.paymentDeadline = null;
    }

    /** AWAITING_PAYMENT → FAILED (결제 기한 만료). 스케줄러가 호출. */
    public void expirePayment() {
        if (this.status != AuctionStatus.AWAITING_PAYMENT) {
            throw new IllegalStateException("결제 대기 상태가 아닙니다.");
        }
        this.status = AuctionStatus.FAILED;
        // 유찰 처리. winner/winningPrice 는 이력 보존을 위해 그대로 둔다.
    }

    public void cancel() {
        this.status = AuctionStatus.CANCELED;
    }

    public void markSettled() {
        this.settledAt = LocalDateTime.now();
    }

    public boolean isLive() {
        return this.status == AuctionStatus.LIVE
                && LocalDateTime.now().isBefore(this.endAt);
    }

    public boolean isEnded() {
        return LocalDateTime.now().isAfter(this.endAt)
                || this.status == AuctionStatus.AWAITING_PAYMENT
                || this.status == AuctionStatus.SUCCESS
                || this.status == AuctionStatus.FAILED
                || this.status == AuctionStatus.CANCELED;
    }
}
