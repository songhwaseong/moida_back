package com.moida.common.response;

import com.moida.domain.member.Member;
import com.moida.domain.sanction.Sanction;

import java.time.format.DateTimeFormatter;

/**
 * 관리자 제재 응답 DTO.
 *   - type     : SanctionType enum 이름 그대로 (프론트에서 라벨 매핑)
 *   - createdAt: yyyy.MM.dd HH:mm
 */
public record AdminSanctionResponse(
        Long id,
        String memberNo,
        String memberName,
        String type,
        String reason,
        String adminNote,
        String expiresAt,
        String createdAt
) {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    public static AdminSanctionResponse from(Sanction s) {
        Member m = s.getMember();
        return new AdminSanctionResponse(
                s.getId(),
                m == null || m.getMemberNo() == null ? "" : m.getMemberNo(),
                m == null ? "-" : m.getName(),
                s.getType().name(),
                s.getReason(),
                s.getAdminNote(),
                s.getExpiresAt() == null ? null : s.getExpiresAt().format(DATE),
                s.getCreatedAt() == null ? "" : s.getCreatedAt().format(DATE_TIME)
        );
    }
}
