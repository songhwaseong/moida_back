package com.moida.domain.notification;

import com.moida.common.entity.BaseTimeEntity;
import com.moida.domain.member.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "notifications",
        indexes = {
                @Index(name = "idx_notif_member", columnList = "member_id"),
                @Index(name = "idx_notif_read", columnList = "is_read")
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 500)
    private String content;

    @Column(name = "link_url", length = 500)
    private String linkUrl;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead;

    @Builder
    private Notification(Member member, NotificationType type, String title, String content, String linkUrl) {
        this.member = member;
        this.type = type;
        this.title = title;
        this.content = content;
        this.linkUrl = linkUrl;
        this.isRead = false;
    }

    public void markAsRead() { this.isRead = true; }

    public enum NotificationType {
        BID_PLACED,       // 입찰 발생
        BID_OUTBID,       // 다른 사람이 더 높게 입찰
        AUCTION_WON,      // 낙찰 성공
        AUCTION_LOST,     // 낙찰 실패
        AUCTION_ENDED,    // 경매 종료
        INQUIRY_NEW,      // 새 문의
        INQUIRY_ANSWERED, // 문의 답변 등록
        CHAT_MESSAGE,     // 채팅 메시지
        SANCTION,         // 제재 발생
        NOTICE            // 공지사항
    }
}
