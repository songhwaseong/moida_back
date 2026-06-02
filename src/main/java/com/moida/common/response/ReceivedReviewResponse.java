package com.moida.common.response;

import com.moida.domain.review.Review;

import java.time.LocalDateTime;

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
        return new ReceivedReviewResponse(
                review.getId(),
                review.getReviewer().getNickname(),
                review.getReviewer().getAvatar(),
                review.getProduct().getId(),
                review.getProduct().getName(),
                review.getProduct().getMainImageUrl(),
                review.getRating(),
                review.getContent(),
                review.getMannerTempChange(),
                review.getCreatedAt()
        );
    }
}
