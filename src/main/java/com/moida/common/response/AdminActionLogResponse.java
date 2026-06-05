package com.moida.common.response;

import com.moida.domain.audit.AdminActionLog;

import java.time.format.DateTimeFormatter;

public record AdminActionLogResponse(
        Long id,
        Long adminId,
        String adminEmail,
        String actionType,
        String targetType,
        Long targetId,
        String targetName,
        String beforeValue,
        String afterValue,
        String reason,
        String ip,
        String userAgent,
        String createdAt
) {
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss");

    public static AdminActionLogResponse from(AdminActionLog log) {
        return new AdminActionLogResponse(
                log.getId(),
                log.getAdminId(),
                log.getAdminEmail(),
                log.getActionType(),
                log.getTargetType(),
                log.getTargetId(),
                log.getTargetName(),
                log.getBeforeValue(),
                log.getAfterValue(),
                log.getReason(),
                log.getIp(),
                log.getUserAgent(),
                log.getCreatedAt() == null ? "-" : log.getCreatedAt().format(DATE_TIME_FMT)
        );
    }
}
