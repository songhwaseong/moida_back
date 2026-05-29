package com.moida.common.response;

import com.moida.domain.inquiry.Inquiry;
import com.moida.domain.product.ProductStatus;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record InquiryResponse(
        Long id,
        Long productId,
        String itemName,
        String itemImage,
        String seller,
        String user,
        String kind,
        String date,
        String question,
        String answer,
        String answerDate,
        Boolean isSecret
) {
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    public static InquiryResponse from(Inquiry inquiry) {
        return new InquiryResponse(
                inquiry.getId(),
                inquiry.getProduct().getId(),
                inquiry.getProduct().getName(),
                inquiry.getProduct().getMainImageUrl(),
                inquiry.getSeller().getName(),
                inquiry.getUser().getName(),
                kind(inquiry.getProduct().getStatus()),
                format(inquiry.getCreatedAt()),
                inquiry.getQuestion(),
                inquiry.getAnswer(),
                format(inquiry.getAnsweredAt()),
                inquiry.getIsSecret()
        );
    }

    private static String format(LocalDateTime value) {
        return value == null ? null : value.format(DATE_TIME_FORMAT);
    }

    private static String kind(ProductStatus status) {
        return status == ProductStatus.LIVE ? "auction" : "product";
    }
}
