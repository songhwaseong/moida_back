package com.moida.domain.address;

import com.moida.common.entity.BaseTimeEntity;
import com.moida.domain.member.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원의 배송지 주소를 관리하는 엔티티
 */
@Entity
@Getter
@Table(name = "member_addresses",
        indexes = {
                @Index(name = "idx_member_address_member", columnList = "member_id"),
                @Index(name = "idx_member_address_default", columnList = "member_id, default_address")
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberAddress extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "address_id")
    private Long id;

    /** 주소의 소유 회원 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    /** 배송지 이름 */
    @Column(nullable = false, length = 50)
    private String name;

    /** 우편번호 */
    @Column(nullable = false, length = 5)
    private String zonecode;

    /** 기본 주소 */
    @Column(nullable = false, length = 255)
    private String address;

    /** 상세 주소 */
    @Column(length = 100)
    private String detail;

    /** 연락처 */
    @Column(nullable = false, length = 20)
    private String phone;

    /** 기본 배송지 여부 */
    @Column(name = "default_address", nullable = false)
    private boolean defaultAddress;

    @Builder
    private MemberAddress(
            Member member,
            String name,
            String zonecode,
            String address,
            String detail,
            String phone,
            boolean defaultAddress
    ) {
        this.member = member;
        this.name = name;
        this.zonecode = zonecode;
        this.address = address;
        this.detail = detail;
        this.phone = phone;
        this.defaultAddress = defaultAddress;
    }

    /**
     * 주소 정보를 갱신합니다.
     */
    public void update(String name, String zonecode, String address, String detail, String phone, boolean defaultAddress) {
        this.name = name;
        this.zonecode = zonecode;
        this.address = address;
        this.detail = detail;
        this.phone = phone;
        this.defaultAddress = defaultAddress;
    }

    /**
     * 기본 배송지 여부를 변경합니다.
     */
    public void setDefaultAddress(boolean defaultAddress) {
        this.defaultAddress = defaultAddress;
    }
}
