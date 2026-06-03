package com.moida.domain.notification;

import com.moida.common.entity.BaseTimeEntity;
import com.moida.domain.member.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "notification_settings",
        uniqueConstraints = @UniqueConstraint(name = "uk_notification_setting_member", columnNames = "member_id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationSetting extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_setting_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "bid_enabled", nullable = false)
    private boolean bidEnabled;

    @Column(name = "price_enabled", nullable = false)
    private boolean priceEnabled;

    @Column(name = "chat_enabled", nullable = false)
    private boolean chatEnabled;

    @Column(name = "trade_enabled", nullable = false)
    private boolean tradeEnabled;

    @Column(name = "marketing_enabled", nullable = false)
    private boolean marketingEnabled;

    @Column(name = "product_status_enabled")
    private Boolean productStatusEnabled;

    @Column(name = "inquiry_enabled")
    private Boolean inquiryEnabled;

    @Column(name = "auction_result_enabled")
    private Boolean auctionResultEnabled;

    @Builder
    private NotificationSetting(Member member, boolean bidEnabled, boolean priceEnabled,
                                boolean chatEnabled, boolean tradeEnabled, boolean marketingEnabled,
                                Boolean productStatusEnabled, Boolean inquiryEnabled, Boolean auctionResultEnabled) {
        this.member = member;
        this.bidEnabled = bidEnabled;
        this.priceEnabled = priceEnabled;
        this.chatEnabled = chatEnabled;
        this.tradeEnabled = tradeEnabled;
        this.marketingEnabled = marketingEnabled;
        this.productStatusEnabled = productStatusEnabled == null || productStatusEnabled;
        this.inquiryEnabled = inquiryEnabled == null || inquiryEnabled;
        this.auctionResultEnabled = auctionResultEnabled == null || auctionResultEnabled;
    }

    // 신규 회원 또는 기존 회원의 최초 조회 시 사용할 앱 기본 알림 설정입니다.
    public static NotificationSetting defaultFor(Member member) {
        return NotificationSetting.builder()
                .member(member)
                .bidEnabled(true)
                .priceEnabled(true)
                .chatEnabled(true)
                .tradeEnabled(true)
                .marketingEnabled(false)
                .productStatusEnabled(true)
                .inquiryEnabled(true)
                .auctionResultEnabled(true)
                .build();
    }

    // 설정 화면에서 전달된 전체 토글 상태를 한 번에 반영합니다.
    public void update(boolean bidEnabled, boolean priceEnabled, boolean chatEnabled,
                       boolean tradeEnabled, boolean marketingEnabled,
                       boolean productStatusEnabled, boolean inquiryEnabled, boolean auctionResultEnabled) {
        this.bidEnabled = bidEnabled;
        this.priceEnabled = priceEnabled;
        this.chatEnabled = chatEnabled;
        this.tradeEnabled = tradeEnabled;
        this.marketingEnabled = marketingEnabled;
        this.productStatusEnabled = productStatusEnabled;
        this.inquiryEnabled = inquiryEnabled;
        this.auctionResultEnabled = auctionResultEnabled;
    }

    public boolean isProductStatusEnabled() {
        return productStatusEnabled == null || productStatusEnabled;
    }

    public boolean isInquiryEnabled() {
        return inquiryEnabled == null || inquiryEnabled;
    }

    public boolean isAuctionResultEnabled() {
        return auctionResultEnabled == null || auctionResultEnabled;
    }
}
