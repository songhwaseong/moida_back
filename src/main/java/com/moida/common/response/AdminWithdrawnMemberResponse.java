package com.moida.common.response;

import com.moida.domain.member.Member;
import com.moida.domain.member.MemberStatus;

import java.time.LocalDateTime;

/**
 * 관리자 탈퇴 회원 관리 화면에서 사용하는 회원 요약 응답 DTO입니다.
 */
public record AdminWithdrawnMemberResponse(
        Long id,
        String memberNo,
        String name,
        String email,
        String phone,
        LocalDateTime joinedAt,
        LocalDateTime lastLoginAt,
        LocalDateTime withdrawnAt,
        Double mannerTemp,
        Integer salesCount,
        Integer purchaseCount,
        Integer bidCount,
        Integer reportCount,
        Integer sanctionCount,
        MemberStatus status
) {
    public static AdminWithdrawnMemberResponse from(Member member) {
        return new AdminWithdrawnMemberResponse(
                member.getId(),
                member.getMemberNo(),
                member.getName(),
                member.getEmail(),
                member.getPhone(),
                member.getCreatedAt(),
                member.getLastLoginAt(),
                member.getWithdrawnAt(),
                member.getMannerTemp(),
                member.getSalesCount(),
                member.getPurchaseCount(),
                member.getBidCount(),
                member.getReportCount(),
                member.getSanctionCount(),
                member.getStatus()
        );
    }
}
