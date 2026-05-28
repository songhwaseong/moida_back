package com.moida.common.response;

import com.moida.domain.member.Member;

import java.time.LocalDateTime;

public record AdminDeactivatedMemberResponse(
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
        String deactivationReasonCode,
        String deactivationReasonDetail,
        String status
) {
    public static AdminDeactivatedMemberResponse from(Member member) {
        return new AdminDeactivatedMemberResponse(
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
                member.getDeactivationReasonCode(),
                member.getDeactivationReasonDetail(),
                member.getStatus().name()
        );
    }
}
