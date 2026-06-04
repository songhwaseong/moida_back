package com.moida.common.response;

import com.moida.domain.audit.AdminLoginLog;

import java.time.format.DateTimeFormatter;

/**
 * 관리자 로그인 기록 응답 DTO.
 */
public record AdminLoginLogResponse(
        Long id,
        Long memberId,
        String email,
        String role,
        String ip,
        String userAgent,
        String result,     // SUCCESS / FAIL
        String loginAt
) {
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss");

    public static AdminLoginLogResponse from(AdminLoginLog log) {
        return new AdminLoginLogResponse(
                log.getId(),
                log.getMemberId(),
                log.getEmail(),
                log.getRole().name(),
                log.getIp(),
                log.getUserAgent(),
                log.getResult().name(),
                log.getCreatedAt() == null ? "-" : log.getCreatedAt().format(DATE_TIME_FMT)
        );
    }
}
