package com.moida.common.response;

import com.moida.domain.review.Review;

import java.time.LocalDateTime;
import java.util.function.Function;

public record ReceivedReviewResponse(
        Long id,
        String reviewerNickname,
        String reviewerAvatar,
        Long productId,
        String productName,
        String productImage,
        Integer rating,
        String content,
        Double mannerTempChange,
        LocalDateTime createdAt
) {
    public static ReceivedReviewResponse from(Review review) {
        return from(review, Function.identity());
    }

    public static ReceivedReviewResponse from(Review review, Function<String, String> imageUrlResolver) {
        String mainImageUrl = review.getProduct().getMainImageUrl();
        return new ReceivedReviewResponse(
                review.getId(),
                review.getReviewer().getNickname(),
                review.getReviewer().getAvatar(),
                review.getProduct().getId(),
                review.getProduct().getName(),
                imageUrlResolver.apply(mainImageUrl),
                review.getRating(),
                review.getContent(),
                review.getMannerTempChange(),
                review.getCreatedAt()
        );
    }
}
