package com.moida.domain.audit;

import com.moida.common.entity.BaseTimeEntity;
import com.moida.domain.member.MemberRole;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 관리자(ADMIN/MANAGER) 로그인 시도 기록.
 * 성공/실패 모두 남기며, 접속 시각은 BaseTimeEntity 의 createdAt 을 사용한다.
 * 조회는 ADMIN 만 가능하다(SecurityConfig).
 */
@Entity
@Getter
@Table(name = "admin_login_log",
        indexes = @Index(name = "idx_admin_login_log_created_at", columnList = "created_at"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminLoginLog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "admin_login_log_id")
    private Long id;

    /** 대상 회원 ID. (실패 시에도 대상 계정이 존재하므로 채워진다) */
    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private MemberRole role;

    @Column(name = "ip", length = 64)
    private String ip;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false, length = 10)
    private LoginResult result;

    @Builder
    private AdminLoginLog(Long memberId, String email, MemberRole role,
                          String ip, String userAgent, LoginResult result) {
        this.memberId = memberId;
        this.email = email;
        this.role = role;
        this.ip = ip;
        this.userAgent = userAgent;
        this.result = result;
    }
}
