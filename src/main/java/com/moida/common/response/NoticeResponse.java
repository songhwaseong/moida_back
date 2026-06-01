package com.moida.common.response;

import com.moida.domain.notice.Notice;

import java.time.format.DateTimeFormatter;

public record NoticeResponse(
        Long id,
        String title,
        String category,
        String status,
        Boolean isPinned,
        String author,
        String createdAt,
        String content,
        Long viewCount
) {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    public static NoticeResponse from(Notice notice) {
        return new NoticeResponse(
                notice.getId(),
                notice.getTitle(),
                categoryLabel(notice.getCategory()),
                statusLabel(notice.getStatus()),
                notice.getIsPinned(),
                notice.getAuthor().getName(),
                notice.getCreatedAt() == null ? "" : notice.getCreatedAt().format(DATE_FORMAT),
                notice.getContent(),
                notice.getViewCount()
        );
    }

    private static String categoryLabel(Notice.NoticeCategory category) {
        if (category == Notice.NoticeCategory.EVENT) return "이벤트";
        if (category == Notice.NoticeCategory.MAINTENANCE) return "점검";
        if (category == Notice.NoticeCategory.POLICY) return "정책";
        return "서비스";
    }

    private static String statusLabel(Notice.NoticeStatus status) {
        if (status == Notice.NoticeStatus.SCHEDULED) return "예약";
        if (status == Notice.NoticeStatus.HIDDEN) return "숨김";
        return "게시중";
    }
}
