package com.moida.domain.notice;

import com.moida.common.entity.BaseTimeEntity;
import com.moida.domain.member.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "notices",
        indexes = @Index(name = "idx_notice_pinned", columnList = "is_pinned"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notice extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notice_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private Member author;

    @Column(nullable = false, length = 200)
    private String title;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NoticeCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NoticeStatus status;

    @Column(name = "is_pinned", nullable = false)
    private Boolean isPinned;

    @Column(name = "view_count", nullable = false)
    private Long viewCount;

    @Builder
    private Notice(Member author, String title, String content, NoticeCategory category, NoticeStatus status, Boolean isPinned) {
        this.author = author;
        this.title = title;
        this.content = content;
        this.category = category == null ? NoticeCategory.GENERAL : category;
        this.status = status == null ? NoticeStatus.PUBLISHED : status;
        this.isPinned = isPinned != null && isPinned;
        this.viewCount = 0L;
    }

    public void update(String title, String content, NoticeCategory category, NoticeStatus status, Boolean isPinned) {
        if (title != null) this.title = title;
        if (content != null) this.content = content;
        if (category != null) this.category = category;
        if (status != null) this.status = status;
        if (isPinned != null) this.isPinned = isPinned;
    }

    public void increaseViewCount() { this.viewCount++; }

    public enum NoticeCategory {
        GENERAL,    // 일반
        EVENT,      // 이벤트
        MAINTENANCE,// 점검
        POLICY,     // 정책
        UPDATE,     // 업데이트
        URGENT      // 긴급
    }

    public enum NoticeStatus {
        PUBLISHED,
        SCHEDULED,
        HIDDEN
    }
}
