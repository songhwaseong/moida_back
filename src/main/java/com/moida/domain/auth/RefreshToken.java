package com.moida.domain.auth;

import com.moida.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 서버측에 보관하는 refresh 토큰 — stateless refresh 의 보완.
 *
 * 회원당 활성 refresh 토큰을 정확히 1개만 저장하고(member_id UNIQUE), 갱신 때마다 해시를 교체한다.
 * 이로써 (1) 로그아웃/제재 시 즉시 폐기(revoke), (2) 갱신 시 이전 토큰 자동 무효화(rotation),
 * (3) 이미 회전된/위조된 토큰 거부가 가능해진다.
 *
 * 원문 대신 SHA-256 해시를 저장해 DB 유출 시에도 토큰 자체는 노출되지 않게 한다.
 */
@Entity
@Getter
@Table(name = "refresh_tokens")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "refresh_token_id")
    private Long id;

    @Column(name = "member_id", nullable = false, unique = true)
    private Long memberId;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    private RefreshToken(Long memberId, String tokenHash, LocalDateTime expiresAt) {
        this.memberId = memberId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public static RefreshToken create(Long memberId, String tokenHash, LocalDateTime expiresAt) {
        return new RefreshToken(memberId, tokenHash, expiresAt);
    }

    /** 갱신: 새 해시/만료로 교체. 이전 refresh 토큰은 더 이상 매칭되지 않아 자연히 무효화된다. */
    public void rotate(String tokenHash, LocalDateTime expiresAt) {
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public boolean matches(String tokenHash) {
        return this.tokenHash.equals(tokenHash);
    }

    public boolean isExpired(LocalDateTime now) {
        return expiresAt.isBefore(now);
    }
}
