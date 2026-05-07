package com.moida.domain.auction;

import com.moida.common.entity.BaseTimeEntity;
import com.moida.domain.member.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "bids",
        indexes = {
                @Index(name = "idx_bid_auction", columnList = "auction_id"),
                @Index(name = "idx_bid_bidder", columnList = "bidder_id"),
                @Index(name = "idx_bid_amount", columnList = "amount")
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Bid extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bid_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "auction_id", nullable = false)
    private Auction auction;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bidder_id", nullable = false)
    private Member bidder;

    @Column(nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BidType bidType;            // NORMAL / IMMEDIATE

    @Column(name = "is_winning", nullable = false)
    private Boolean isWinning;          // 현재 최고가 여부

    @Column(name = "is_suspicious", nullable = false)
    private Boolean isSuspicious;       // 허위입찰 의심 여부

    @Builder
    private Bid(Auction auction, Member bidder, Long amount, BidType bidType) {
        this.auction = auction;
        this.bidder = bidder;
        this.amount = amount;
        this.bidType = bidType == null ? BidType.NORMAL : bidType;
        this.isWinning = true;
        this.isSuspicious = false;
    }

    public void unmarkWinning() { this.isWinning = false; }

    public void flagAsSuspicious() { this.isSuspicious = true; }

    public enum BidType {
        NORMAL,      // 일반 입찰
        IMMEDIATE    // 즉시낙찰
    }
}
