package com.moida.domain.review;

import com.moida.common.response.ReceivedReviewResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private static final int DEFAULT_RECEIVED_REVIEW_SIZE = 50;
    private static final int MAX_RECEIVED_REVIEW_SIZE = 100;

    private final ReviewRepository reviewRepository;

    @Transactional(readOnly = true)
    public List<ReceivedReviewResponse> getReceivedReviews(Long memberId, Integer size) {
        int pageSize = normalizeSize(size);
        return reviewRepository.findAllByTargetMemberIdOrderByCreatedAtDesc(
                        memberId,
                        PageRequest.of(0, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"))
                )
                .stream()
                .map(ReceivedReviewResponse::from)
                .toList();
    }

    private int normalizeSize(Integer size) {
        if (size == null) return DEFAULT_RECEIVED_REVIEW_SIZE;
        return Math.max(1, Math.min(size, MAX_RECEIVED_REVIEW_SIZE));
    }
}
