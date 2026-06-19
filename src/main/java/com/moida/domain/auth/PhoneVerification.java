package com.moida.domain.auth;

import com.moida.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 휴대폰 인증번호 발급/검증 상태를 보관하는 엔티티.
 * 번호(phone)당 1행을 유지하며, 재전송 시 같은 행을 갱신(renew)한다.
 */
@Entity
@Getter
@Table(name = "phone_verifications",
        indexes = @Index(name = "uk_phone_verification_phone", columnList = "phone", unique = true))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PhoneVerification extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "phone_verification_id")
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String phone;          // 숫자만 (예: 01012345678)

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

    @Column(name = "daily_send_count", nullable = false)
    private int dailySendCount;      // 오늘 발송 횟수 (일일 총량 제한용)

    @Column(name = "daily_send_date")
    private LocalDate dailySendDate; // dailySendCount 가 가리키는 날짜

    @Builder
    private PhoneVerification(String phone, String code, LocalDateTime expiresAt) {
        this.phone = phone;
        this.code = code;
        this.expiresAt = expiresAt;
        this.verified = false;
        this.attemptCount = 0;
    }

    public static PhoneVerification issue(String phone, String code, LocalDateTime expiresAt) {
        return PhoneVerification.builder()
                .phone(phone)
                .code(code)
                .expiresAt(expiresAt)
                .build();
    }

    /** 재전송 시 새 코드/만료시각으로 갱신하고 검증/시도 상태를 초기화한다. */
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

    /** 인증 완료 후 유효시간(window) 이내인지. 가입 게이트에서 사용. */
    public boolean isVerifiedWithin(LocalDateTime now, Duration window) {
        return this.verified
                && this.verifiedAt != null
                && Duration.between(this.verifiedAt, now).compareTo(window) <= 0;
    }

    /** 오늘 발송 횟수가 limit 이상이면 true(더 보낼 수 없음). 날짜가 바뀌면 오늘 첫 발송이라 false. */
    public boolean hasReachedDailyLimit(LocalDate today, int limit) {
        return today.equals(this.dailySendDate) && this.dailySendCount >= limit;
    }

    /** 발송 1건 기록. 날짜가 바뀌면 1로 리셋하고, 같은 날이면 누적한다. */
    public void recordSend(LocalDate today) {
        if (today.equals(this.dailySendDate)) {
            this.dailySendCount++;
        } else {
            this.dailySendDate = today;
            this.dailySendCount = 1;
        }
    }
}
