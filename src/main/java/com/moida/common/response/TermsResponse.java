package com.moida.common.response;

import com.moida.domain.terms.TermsDocument;

import java.time.format.DateTimeFormatter;

public record TermsResponse(
        Long id,
        String type,
        String title,
        String content,
        String effectiveDate,
        String updatedAt
) {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    public static TermsResponse from(TermsDocument document) {
        return new TermsResponse(
                document.getId(),
                document.getType().name(),
                document.getTitle(),
                document.getContent(),
                document.getEffectiveDate() == null ? null : document.getEffectiveDate().format(DATE_FORMATTER),
                document.getUpdatedAt() == null ? null : document.getUpdatedAt().format(DATE_TIME_FORMATTER)
        );
    }
}
