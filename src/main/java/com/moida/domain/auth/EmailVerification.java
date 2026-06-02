package com.moida.domain.auth;

import com.moida.common.entity.BaseTimeEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.*;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "email_verifications",
        indexes = @Index(name = "uk_email_verification_email", columnList = "email", unique = true))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailVerification extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "email_verification_id")
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 6)
    private String code;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean verified;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Builder
    private EmailVerification(String email, String code, LocalDateTime expiresAt) {
        this.email = email;
        this.code = code;
        this.expiresAt = expiresAt;
        this.verified = false;
        this.attemptCount = 0;
    }

    public static EmailVerification issue(String email, String code, LocalDateTime expiresAt) {
        return EmailVerification.builder()
                .email(email)
                .code(code)
                .expiresAt(expiresAt)
                .build();
    }

    public void renew(String code, LocalDateTime expiresAt) {
        this.code = code;
        this.expiresAt = expiresAt;
        this.verified = false;
        this.verifiedAt = null;
        this.attemptCount = 0;
    }

    public void increaseAttempt() {
        this.attemptCount++;
    }

    public void markVerified(LocalDateTime now) {
        this.verified = true;
        this.verifiedAt = now;
    }

    public boolean isExpired(LocalDateTime now) {
        return now.isAfter(this.expiresAt);
    }

    public boolean isVerifiedWithin(LocalDateTime now, Duration window) {
        return this.verified
                && this.verifiedAt != null
                && Duration.between(this.verifiedAt, now).compareTo(window) <= 0;
    }
}
