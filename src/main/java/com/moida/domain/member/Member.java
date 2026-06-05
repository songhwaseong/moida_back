package com.moida.domain.member;

import com.moida.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "members",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_member_email", columnNames = "email"),
                @UniqueConstraint(name = "uk_member_no", columnNames = "member_no")
        },
        indexes = {
                @Index(name = "idx_member_status", columnList = "status"),
                @Index(name = "idx_member_phone", columnList = "phone")
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    @Column(name = "member_no", nullable = false, length = 20)
    private String memberNo;        // 회원번호 (e.g. 2024031500001)

    @Column(nullable = false, length = 100)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 20)
    private String phone;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Column(length = 100)
    private String location;        // 거래 지역

    @Column(name = "manner_temp", nullable = false)
    private Double mannerTemp;      // 매너 온도

    @Column(name = "balance", nullable = false)
    private Long balance;           // 예치금 잔액

    @Column(name = "sales_count", nullable = false)
    private Integer salesCount;

    @Column(name = "purchase_count", nullable = false)
    private Integer purchaseCount;

    @Column(name = "bid_count", nullable = false)
    private Integer bidCount;

    @Column(name = "report_count", nullable = false)
    private Integer reportCount;

    @Column(name = "sanction_count", nullable = false)
    private Integer sanctionCount;

    /**
     * 누적 미결제(낙찰 후 결제 기한 만료) 횟수.
     * 3 회 누적 시 자동으로 7일 입찰 정지(Sanction SUSPEND_7) 가 발급되고 본 카운트는 0 으로 리셋된다.
     * 정상 결제 완료 시점에는 변경하지 않는다(전체 기록 기반 정책 변경 시 활용을 위해).
     */
    @Column(name = "non_payment_count", nullable = false)
    private Integer nonPaymentCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberStatus status;

    @Column(name = "suspended_until")
    private LocalDateTime suspendedUntil;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "withdrawn_at")
    private LocalDateTime withdrawnAt;

    @Column(name = "deactivation_reason_code", length = 50)
    private String deactivationReasonCode;

    @Column(name = "deactivation_reason_detail", length = 500)
    private String deactivationReasonDetail;

    @Column(name = "social_login", length = 20)
    private String socialLogin; // 소셜 로그인 구분값 ("KAKAO" / "NAVER" / "GOOGLE" / null이면 일반 가입)

    @Column(length = 50)
    private String nickname; // 나중에 nullable=false 추가

    @Column(length = 10)
    private String avatar;

    @Builder
    private Member(String memberNo, String email, String password, String name, String nickname, String phone,
                   String profileImageUrl, String location, MemberRole role, String socialLogin) {// 소셜 로그인 구분값 추가
        this.memberNo = memberNo;
        this.email = email;
        this.password = password;
        this.name = name;
        this.nickname = (nickname != null && !nickname.isBlank()) ? nickname : name;
        this.phone = phone;
        this.profileImageUrl = profileImageUrl;
        this.location = location;
        this.mannerTemp = 36.5;
        this.balance = 0L;
        this.salesCount = 0;
        this.purchaseCount = 0;
        this.bidCount = 0;
        this.reportCount = 0;
        this.sanctionCount = 0;
        this.nonPaymentCount = 0;
        this.role = role != null ? role : MemberRole.USER;
        this.status = MemberStatus.ACTIVE;
        this.socialLogin = socialLogin; // 소셜 로그인 구분값 초기화
    }

    // ===== Domain Methods =====
    public void updateProfile(String name, String phone, String location, String profileImageUrl) {
        if (name != null) this.name = name;
        if (phone != null) this.phone = phone;
        if (location != null) this.location = location;
        if (profileImageUrl != null) this.profileImageUrl = profileImageUrl;
    }

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void registerLocalCredentials(String email, String encodedPassword, String name,
                                         String nickname, String phone, String location) {
        if (email != null && !email.isBlank()) this.email = email;
        if (encodedPassword != null && !encodedPassword.isBlank()) this.password = encodedPassword;
        if (name != null && !name.isBlank()) this.name = name;
        updateNickname(nickname);
        updateProfile(null, phone, location, null);
        this.socialLogin = null;
    }

    public void updateLastLogin() {
        this.lastLoginAt = LocalDateTime.now();
    }

    public void chargeBalance(long amount) {
        if (amount <= 0) throw new IllegalArgumentException("충전 금액은 0보다 커야 합니다.");
        this.balance += amount;
    }

    public void deductBalance(long amount) {
        if (amount > this.balance) throw new IllegalStateException("잔액이 부족합니다.");
        this.balance -= amount;
    }

    public void increaseSalesCount() { this.salesCount++; }
    public void increasePurchaseCount() { this.purchaseCount++; }
    public void increaseBidCount() { this.bidCount++; }
    public void increaseReportCount() { this.reportCount++; }

    /** 미결제 1 회 추가. 누적값이 임계치(예: 3)에 도달하면 호출자가 Sanction 발급 후 resetNonPaymentCount() 를 부른다. */
    public void increaseNonPaymentCount() { this.nonPaymentCount = (this.nonPaymentCount == null ? 0 : this.nonPaymentCount) + 1; }

    public void resetNonPaymentCount() { this.nonPaymentCount = 0; }

    public void suspend(LocalDateTime until) {
        this.status = MemberStatus.SUSPENDED;
        this.suspendedUntil = until;
        this.sanctionCount++;
    }

    public void permanentBan() {
        this.status = MemberStatus.PERMANENT;
        this.sanctionCount++;
    }

    /** 경고(WARNING) — 상태는 변경하지 않고 누적 카운트만 증가시킨다. */
    public void warn() {
        this.sanctionCount++;
    }

    public void deactivateAccount(String reasonCode, String reasonDetail) {
        this.status = MemberStatus.WITHDRAWN;
        this.withdrawnAt = LocalDateTime.now();
        this.deactivationReasonCode = reasonCode;
        this.deactivationReasonDetail = reasonDetail;
    }

    public void activate() {
        this.status = MemberStatus.ACTIVE;
        this.suspendedUntil = null;
    }

    public boolean isActive() {
        return this.status == MemberStatus.ACTIVE;
    }

    public void updateNickname(String nickname) {
        if (nickname != null && !nickname.isBlank()) {
            this.nickname = nickname;
        }
    }

    public void updateAvatar(String avatar) {
        if (avatar != null && !avatar.isBlank()) {
            this.avatar = avatar;
        }
    }

    // 관리자가 특정 회원의 역할을 변경할 때 사용
    public void updateRole(MemberRole role) {
        this.role = role;
    }
}
