package com.moida.domain.product;

import com.moida.common.exception.BusinessException;
import com.moida.common.exception.ErrorCode;
import com.moida.common.response.ProductSummaryResponse;
import com.moida.domain.auction.Auction;
import com.moida.domain.auction.AuctionRepository;
import com.moida.domain.member.Member;
import com.moida.domain.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductLikeService {

    private final ProductLikeRepository productLikeRepository;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;
    private final AuctionRepository auctionRepository;
    private final ProductImageStorageService productImageStorageService;

    // 토글 결과를 프론트가 그대로 받아 하트 상태와 카운트를 갱신한다.
    public record ToggleResult(boolean liked, long likeCount) {}

    // 좋아요 토글. 이미 눌렀으면 해제하고 likeCount를 감소, 아니면 추가하고 증가시킨다.
    // ProductLike 테이블과 Product.likeCount 카운터를 한 트랜잭션에서 묶어 데이터 일관성을 보장한다.
    @Transactional
    public ToggleResult toggle(Long productId, Long memberId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        boolean alreadyLiked = productLikeRepository.existsByProductIdAndMemberId(productId, memberId);
        if (alreadyLiked) {
            productLikeRepository.deleteByProductIdAndMemberId(productId, memberId);
            product.decreaseLikeCount();
            log.info("[ProductLikeService] removed productId={}, memberId={}", productId, memberId);
            return new ToggleResult(false, product.getLikeCount());
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        productLikeRepository.save(ProductLike.builder()
                .product(product)
                .member(member)
                .build());
        product.increaseLikeCount();
        log.info("[ProductLikeService] added productId={}, memberId={}", productId, memberId);
        return new ToggleResult(true, product.getLikeCount());
    }

    // 위시리스트(관심 목록) 페이지가 호출. 사용자가 좋아요한 상품을 최신순으로 가져와
    // 진행 중인 경매 정보(현재가/입찰수/남은시간)를 함께 평탄화해 돌려준다.
    // 프론트는 isLive 플래그로 "경매 예정" / "경매" 탭에 분배한다.
    @Transactional(readOnly = true)
    public List<ProductSummaryResponse> getMyLikes(Long memberId) {
        List<Product> products = productLikeRepository
                .findAllByMemberId(memberId, PageRequest.of(0, 100))
                .stream()
                .map(ProductLike::getProduct)
                .toList();

        if (products.isEmpty()) return Collections.emptyList();

        // N+1 회피: 상품 ID 묶음으로 한 번에 경매 정보를 조회해 매핑한다.
        Map<Long, Auction> auctionsByProductId = auctionRepository
                .findAllByProductIdIn(products.stream().map(Product::getId).toList())
                .stream()
                .collect(Collectors.toMap(a -> a.getProduct().getId(), a -> a));

        return products.stream()
                .map(product -> ProductSummaryResponse.from(
                        product,
                        auctionsByProductId.get(product.getId()),
                        productImageStorageService::toPublicUrl
                ))
                .toList();
    }
}
