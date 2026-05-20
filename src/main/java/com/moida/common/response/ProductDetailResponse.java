package com.moida.common.response;

import com.moida.domain.auction.Auction;
import com.moida.domain.auction.Bid;
import com.moida.domain.product.Product;
import com.moida.domain.product.ProductImage;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

public record ProductDetailResponse(
        Long id,
        String productNo,
        String name,
        String image,
        List<String> images,
        String location,
        String timeAgo,
        Long price,
        String condition,
        List<String> tags,
        Long likeCount,
        Long viewCount,
        Boolean liked,
        Boolean canAuction,
        String auctionDate,
        String category,
        String description,
        String seller,
        Double sellerTemp,
        Integer sellerSales,
        Boolean ownedByMe,
        Long immediatePrice,
        String type,
        String auctionNo,
        Long startPrice,
        Long minBidUnit,
        Long currentPrice,
        Integer bidCount,
        Long timeLeft,
        Boolean isLive,
        String endDate,
        List<BidHistoryResponse> bidHistory
) {
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    // 상품 상세와 경매 상세가 같은 API를 쓰도록 공통 상품 정보에 경매 필드를 함께 담는다.
    // 일반 상품이면 경매 관련 필드는 null/빈 목록으로 내려간다.
    // liked: 요청 사용자가 이 상품에 좋아요를 눌렀는지 여부. 비로그인이면 false.
    public static ProductDetailResponse from(Product product, Auction auction, List<Bid> bids, boolean liked, Long memberId) {
        ProductSummaryResponse summary = ProductSummaryResponse.from(product, auction);
        // 상세 화면의 썸네일 순서를 DB display_order 기준으로 유지한다.
        List<String> images = product.getImages().stream()
                .sorted(Comparator.comparing(ProductImage::getDisplayOrder))
                .map(ProductImage::getUrl)
                .toList();

        if (images.isEmpty() && product.getMainImageUrl() != null) {
            images = List.of(product.getMainImageUrl());
        }

        return new ProductDetailResponse(
                summary.id(),
                summary.productNo(),
                summary.name(),
                summary.image(),
                images,
                summary.location(),
                summary.timeAgo(),
                summary.price(),
                summary.condition(),
                summary.tags(),
                summary.likeCount(),
                summary.viewCount(),
                liked,
                summary.canAuction(),
                summary.auctionDate(),
                summary.category(),
                product.getDescription(),
                product.getSeller().getName(),
                product.getSeller().getMannerTemp(),
                product.getSeller().getSalesCount(),
                memberId != null && product.isOwnedBy(memberId),
                auction != null ? auction.getImmediatePrice() : null,
                summary.type(),
                summary.auctionNo(),
                auction != null ? auction.getStartPrice() : null,
                auction != null ? auction.getMinBidUnit() : null,
                summary.currentPrice(),
                summary.bidCount(),
                summary.timeLeft(),
                summary.isLive(),
                auction != null ? auction.getEndAt().format(DATE_TIME_FORMAT) : null,
                bids.stream().map(BidHistoryResponse::from).toList()
        );
    }
}
