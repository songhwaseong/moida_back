package com.moida.controller;

import com.moida.common.request.CreateReviewRequest;
import com.moida.common.response.ApiResponse;
import com.moida.common.response.ReceivedReviewResponse;
import com.moida.domain.review.ReviewService;
import com.moida.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/members/me/reviews/received")
    public ResponseEntity<ApiResponse<List<ReceivedReviewResponse>>> getReceivedReviews(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) Integer size
    ) {
        return ResponseEntity.ok(ApiResponse.success(reviewService.getReceivedReviews(
                userDetails.getMemberId(),
                size
        )));
    }

    @GetMapping("/public/members/{memberId}/reviews/received")
    public ResponseEntity<ApiResponse<List<ReceivedReviewResponse>>> getMemberReceivedReviews(
            @PathVariable Long memberId,
            @RequestParam(required = false) Integer size
    ) {
        return ResponseEntity.ok(ApiResponse.success(reviewService.getReceivedReviews(
                memberId,
                size
        )));
    }

    @PostMapping("/members/me/reviews")
    public ResponseEntity<ApiResponse<Long>> createReview(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateReviewRequest request
    ) {
        Long reviewId = reviewService.createReview(userDetails.getMemberId(), request);
        return ResponseEntity.ok(ApiResponse.success(reviewId, "후기가 등록되었습니다."));
    }
}
