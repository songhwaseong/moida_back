package com.moida.domain.wallet;

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

/**
 * 회원의 출금 계좌 정보를 관리하는 엔티티
 */
@Entity
@Getter
@Table(name = "member_bank_accounts",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_member_bank_account_member", columnNames = "member_id")
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberBankAccount extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_id")
    private Long id;

    /** 계좌의 소유 회원 */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    /** 은행명 */
    @Column(nullable = false, length = 50)
    private String bank;

    /** 계좌번호 */
    @Column(name = "account_number", nullable = false, length = 20)
    private String accountNumber;

    /** 예금주명 */
    @Column(nullable = false, length = 50)
    private String holder;

    /** 계좌 소유 인증 여부 */
    @Column(nullable = false)
    private boolean verified;

    @Builder
    private MemberBankAccount(Member member, String bank, String accountNumber, String holder) {
        this.member = member;
        this.bank = bank;
        this.accountNumber = accountNumber;
        this.holder = holder;
        this.verified = true;
    }

    /**
     * 계좌 정보를 갱신합니다.
     */
    public void update(String bank, String accountNumber, String holder) {
        this.bank = bank;
        this.accountNumber = accountNumber;
        this.holder = holder;
        this.verified = true;
    }
}
