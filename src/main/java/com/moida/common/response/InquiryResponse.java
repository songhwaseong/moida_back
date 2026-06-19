package com.moida.common.response;

import com.moida.domain.inquiry.Inquiry;
import com.moida.domain.product.ProductStatus;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.UnaryOperator;

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
    private static final String SECRET_USER = "Private";
    private static final String SECRET_QUESTION = "This is a private inquiry.";

    public static InquiryResponse from(Inquiry inquiry) {
        return from(inquiry, UnaryOperator.identity());
    }

    public static InquiryResponse from(Inquiry inquiry, UnaryOperator<String> imageUrlResolver) {
        return from(inquiry, imageUrlResolver, true);
    }

    public static InquiryResponse from(Inquiry inquiry, boolean canViewSecret) {
        return from(inquiry, UnaryOperator.identity(), canViewSecret);
    }

    public static InquiryResponse from(Inquiry inquiry, UnaryOperator<String> imageUrlResolver,
                                       boolean canViewSecret) {
        boolean maskSecret = Boolean.TRUE.equals(inquiry.getIsSecret()) && !canViewSecret;
        return new InquiryResponse(
                inquiry.getId(),
                inquiry.getProduct().getId(),
                inquiry.getProduct().getName(),
                imageUrlResolver.apply(inquiry.getProduct().getMainImageUrl()),
                inquiry.getSeller().getName(),
                maskSecret ? SECRET_USER : inquiry.getUser().getName(),
                kind(inquiry.getProduct().getStatus()),
                format(inquiry.getCreatedAt()),
                maskSecret ? SECRET_QUESTION : inquiry.getQuestion(),
                maskSecret ? null : inquiry.getAnswer(),
                maskSecret ? null : format(inquiry.getAnsweredAt()),
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
