package com.moida.common.response;

import com.moida.domain.member.Member;

import java.time.format.DateTimeFormatter;

public record FindIdResponse(
        String maskedEmail,
        String joinedAt
) {
    private static final DateTimeFormatter JOINED_AT_FORMATTER = DateTimeFormatter.ofPattern("yyyy년 M월 d일");

    public static FindIdResponse from(Member member) {
        String joinedAt = member.getCreatedAt() == null
                ? null
                : member.getCreatedAt().format(JOINED_AT_FORMATTER);
        return new FindIdResponse(maskEmail(member.getEmail()), joinedAt);
    }

    private static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "";
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return maskLocalPart(email);
        }
        return maskLocalPart(email.substring(0, atIndex)) + email.substring(atIndex);
    }

    private static String maskLocalPart(String localPart) {
        if (localPart.length() <= 1) {
            return "*";
        }
        if (localPart.length() <= 3) {
            return localPart.charAt(0) + "*".repeat(localPart.length() - 1);
        }
        int visibleSuffixLength = localPart.length() >= 6 ? 2 : 1;
        int maskedLength = localPart.length() - 2 - visibleSuffixLength;
        return localPart.substring(0, 2)
                + "*".repeat(maskedLength)
                + localPart.substring(localPart.length() - visibleSuffixLength);
    }
}
