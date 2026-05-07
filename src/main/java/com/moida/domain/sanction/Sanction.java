package com.moida.domain.sanction;

import com.moida.common.entity.BaseTimeEntity;
import com.moida.domain.member.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "sanctions",
        indexes = @Index(name = "idx_sanction_member", columnList = "member_id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Sanction extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sanction_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "admin_id", nullable = false)
    private Member admin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SanctionType type;

    @Column(nullable = false, length = 200)
    private String reason;

    @Lob
    @Column(name = "admin_note", columnDefinition = "TEXT")
    private String adminNote;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Builder
    private Sanction(Member member, Member admin, SanctionType type, String reason,
                     String adminNote, LocalDateTime expiresAt) {
        this.member = member;
        this.admin = admin;
        this.type = type;
        this.reason = reason;
        this.adminNote = adminNote;
        this.expiresAt = expiresAt;
    }

    public enum SanctionType {
        WARNING,        // 경고
        SUSPEND_7,      // 7일 정지
        SUSPEND_30,     // 30일 정지
        PERMANENT       // 영구 정지
    }
}
