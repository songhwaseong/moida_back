package com.moida.domain.review;

import com.moida.common.exception.BusinessException;
import com.moida.common.exception.ErrorCode;
import com.moida.common.request.CreateReviewRequest;
import com.moida.common.response.ReceivedReviewResponse;
import com.moida.domain.auction.Auction;
import com.moida.domain.auction.AuctionRepository;
import com.moida.domain.auction.AuctionStatus;
import com.moida.domain.auction.DeliveryStatus;
import com.moida.domain.member.Member;
import com.moida.domain.product.Product;
import com.moida.domain.product.ProductImageStorageService;
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
    private final AuctionRepository auctionRepository;
    private final ProductImageStorageService productImageStorageService;

    @Transactional(readOnly = true)
    public List<ReceivedReviewResponse> getReceivedReviews(Long memberId, Integer size) {
        int pageSize = normalizeSize(size);
        return reviewRepository.findAllByTargetMemberIdOrderByCreatedAtDesc(
                        memberId,
                        PageRequest.of(0, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"))
                )
                .stream()
                .map(review -> ReceivedReviewResponse.from(review, productImageStorageService::toPublicUrl))
                .toList();
    }

    /**
     * 구매자(낙찰자)가 수령확인을 마친 거래에 대해 판매자 후기를 작성한다.
     * - 본인이 낙찰받은 SUCCESS 경매여야 하고, 수령확인(RECEIVED)이 완료돼 있어야 한다.
     * - 한 상품(=거래)당 1회만 작성 가능하다. (DB uk_review + 사전 검증)
     * - 평점에 따라 판매자 매너온도를 가감한다.
     */
    @Transactional
    public Long createReview(Long memberId, CreateReviewRequest request) {
        Auction auction = auctionRepository.findByProductId(request.getProductId())
                .orElseThrow(() -> new BusinessException(ErrorCode.AUCTION_NOT_FOUND));

        Member buyer = auction.getWinner();
        if (auction.getStatus() != AuctionStatus.SUCCESS || buyer == null
                || !buyer.getId().equals(memberId)) {
            throw new BusinessException(ErrorCode.REVIEW_NOT_ALLOWED, "낙찰받은 상품에만 후기를 작성할 수 있습니다.");
        }
        if (auction.getDeliveryStatus() != DeliveryStatus.RECEIVED) {
            throw new BusinessException(ErrorCode.REVIEW_RECEIPT_NOT_CONFIRMED);
        }

        Product product = auction.getProduct();
        if (reviewRepository.existsByProductIdAndReviewerId(product.getId(), memberId)) {
            throw new BusinessException(ErrorCode.REVIEW_ALREADY_WRITTEN);
        }

        Member seller = product.getSeller();
        double mannerTempChange = mannerTempDelta(request.getRating());
        seller.applyMannerTempChange(mannerTempChange);

        Review review = Review.builder()
                .product(product)
                .reviewer(buyer)
                .targetMember(seller)
                .rating(request.getRating())
                .content(normalizeContent(request.getContent()))
                .mannerTempChange(mannerTempChange)
                .build();
        return reviewRepository.save(review).getId();
    }

    /** 평점(1~5)을 판매자 매너온도 가감치로 환산한다. */
    private double mannerTempDelta(int rating) {
        return switch (rating) {
            case 5 -> 0.5;
            case 4 -> 0.2;
            case 2 -> -0.2;
            case 1 -> -0.5;
            default -> 0.0;
        };
    }

    private String normalizeContent(String content) {
        if (content == null) return null;
        String trimmed = content.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private int normalizeSize(Integer size) {
        if (size == null) return DEFAULT_RECEIVED_REVIEW_SIZE;
        return Math.max(1, Math.min(size, MAX_RECEIVED_REVIEW_SIZE));
    }
}
