package com.moida.domain.report;

import com.moida.common.entity.BaseTimeEntity;
import com.moida.domain.member.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "reports",
        indexes = {
                @Index(name = "idx_report_status", columnList = "status"),
                @Index(name = "idx_report_type", columnList = "type")
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Report extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportType type;

    @Column(name = "target_id", nullable = false)
    private Long targetId;               // 신고된 대상 ID (product_id, chat_room_id, review_id 등)

    @Column(name = "target_name", length = 200)
    private String targetName;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_id", nullable = false)
    private Member reporter;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_user_id", nullable = false)
    private Member targetUser;

    @Column(nullable = false, length = 100)
    private String reason;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String detail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportStatus status;

    @Column(name = "admin_note", length = 500)
    private String adminNote;

    @Builder
    private Report(ReportType type, Long targetId, String targetName,
                   Member reporter, Member targetUser, String reason, String detail) {
        this.type = type;
        this.targetId = targetId;
        this.targetName = targetName;
        this.reporter = reporter;
        this.targetUser = targetUser;
        this.reason = reason;
        this.detail = detail;
        this.status = ReportStatus.PENDING;
    }

    public void process(ReportStatus status, String adminNote) {
        this.status = status;
        this.adminNote = adminNote;
    }

    public enum ReportType { PRODUCT, CHAT, REVIEW, USER }

    public enum ReportStatus { PENDING, APPROVED, REJECTED, DELETED }
}
