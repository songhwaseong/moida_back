package com.moida.common.response;

import com.moida.domain.member.Member;
import java.time.format.DateTimeFormatter;

public record AdminMemberResponse (
        Long id,
        String memberNo,
        String email,
        String name,
        String phone,
        String joinedAt,
        String lastLoginAt,
        double mannerTemp,
        int salesCount,
        int purchaseCount,
        int bidCount,
        int reportCount,
        int sanctionCount,
        String status,       // "active" | "suspended" | "permanent" | "withdrawn"
        String suspendUntil,
        String role          // "USER" | "ADMIN"
){
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private static final DateTimeFormatter DATETIME = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    public static AdminMemberResponse from(Member m) {
        return new AdminMemberResponse(
                m.getId(),
                m.getMemberNo(),
                m.getEmail(),
                m.getName(),
                m.getPhone(),
                m.getCreatedAt() != null ? m.getCreatedAt().format(DATE) : "",
                m.getLastLoginAt() != null ? m.getLastLoginAt().format(DATETIME) : "—",
                m.getMannerTemp(),
                m.getSalesCount(),
                m.getPurchaseCount(),
                m.getBidCount(),
                m.getReportCount(),
                m.getSanctionCount(),
                m.getStatus().name().toLowerCase(), // ACTIVE → "active"
                m.getSuspendedUntil() != null ? m.getSuspendedUntil().format(DATE) : null,
                m.getRole().name()                  // USER → "USER"
        );
    }
}
