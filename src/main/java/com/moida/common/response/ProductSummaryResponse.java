package com.moida.common.response;

import com.moida.domain.auction.Auction;
import com.moida.domain.auction.AuctionStatus;
import com.moida.domain.auction.DeliveryStatus;
import com.moida.domain.product.Product;
import com.moida.domain.product.ProductCondition;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

public record ProductSummaryResponse(
        Long id,
        String productNo,
        String name,
        String image,
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
        String type,
        String status,
        String auctionNo,
        Long currentPrice,
        Integer bidCount,
        Long timeLeft,
        Boolean isLive,
        String seller,
        String auctionStatus,
        String paymentDeadline,
        String deliveryStatus,
        String deliveryStatusLabel,
        String reviewRevisionReason,
        String reviewRevisionRequestedAt
) {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    // 프론트 카드 컴포넌트(AuctionCard/ProductCard) 가 기대하는 평탄한 구조로 변환한다.
    // 프로젝트가 경매-only 로 피벗되어 모든 product 를 경매로 취급한다.
    // auction 이 null 인 경우(예: 경매 예정 단계) 경매 부가 정보는 안전한 기본값으로 채워
    // 카드 렌더링이 깨지지 않도록 한다.
    public static ProductSummaryResponse from(Product product, Auction auction) {
        return from(product, auction, UnaryOperator.identity());
    }

    public static ProductSummaryResponse from(
            Product product,
            Auction auction,
            UnaryOperator<String> imageUrlResolver
    ) {
        // 화면 LIVE 뱃지 노출 조건: 경매 row 가 있고, 상태가 LIVE 이며, endAt 이 아직 지나지 않았다.
        boolean liveAuction = auction != null
                && auction.getStatus() == AuctionStatus.LIVE
                && LocalDateTime.now().isBefore(auction.getEndAt());

        return new ProductSummaryResponse(
                product.getId(),
                product.getProductNo(),
                product.getName(),
                imageUrlResolver.apply(product.getMainImageUrl()),
                product.getLocation(),
                timeAgo(product.getCreatedAt()),
                auction != null ? auction.getStartPrice() : product.getPrice(),
                conditionLabel(product.getCondition()),
                tags(product),
                product.getLikeCount(),
                product.getViewCount(),
                false,
                true,
                auction != null ? auction.getStartAt().format(DATE_FORMAT) : null,
                product.getCategory().getName(),
                product.getType().name(),
                product.getStatus().name(),
                auction != null ? auction.getAuctionNo() : null,
                auction != null ? auction.getCurrentPrice() : null,
                auction != null ? auction.getBidCount() : 0,
                auction != null ? Math.max(0, Duration.between(LocalDateTime.now(), auction.getEndAt()).getSeconds()) : 0,
                liveAuction,
                product.getSeller().getName(),
                auction != null ? auction.getStatus().name() : null,
                auction != null && auction.getPaymentDeadline() != null
                        ? auction.getPaymentDeadline().format(DATE_TIME_FORMAT) : null,
                auction != null && auction.getDeliveryStatus() != null ? auction.getDeliveryStatus().name() : null,
                auction != null ? deliveryStatusLabel(auction.getDeliveryStatus()) : null,
                product.getReviewRevisionReason(),
                product.getReviewRevisionRequestedAt() == null ? null : product.getReviewRevisionRequestedAt().format(DATE_TIME_FORMAT)
        );
    }

    private static String deliveryStatusLabel(DeliveryStatus status) {
        if (status == null) return null;
        return switch (status) {
            case PAYMENT_COMPLETED -> "결제완료";
            case SHIPMENT_NOTICE -> "발송알림";
            case SHIPPING -> "배송중";
            case DELIVERED -> "수령확인 대기";
            case RECEIVED -> "정산완료";
        };
    }

    private static String conditionLabel(ProductCondition condition) {
        return condition.name() + "급";
    }

    // 카드에 표시할 태그 뱃지. S급은 "거의새것"(new), 그 외는 "상태양호"(good) 으로 매핑하고,
    // 경매-only 서비스이므로 auction 태그는 일괄 부여한다(필요 시 프론트에서 필터링).
    private static List<String> tags(Product product) {
        List<String> tags = new ArrayList<>();
        if (product.getCondition() == ProductCondition.S) {
            tags.add("new");
        } else {
            tags.add("good");
        }
        tags.add("auction");
        return tags;
    }

    private static String timeAgo(LocalDateTime createdAt) {
        if (createdAt == null) return "방금 전";

        Duration duration = Duration.between(createdAt, LocalDateTime.now());
        long minutes = Math.max(0, duration.toMinutes());
        if (minutes < 1) return "방금 전";
        if (minutes < 60) return minutes + "분 전";

        long hours = duration.toHours();
        if (hours < 24) return hours + "시간 전";

        long days = duration.toDays();
        if (days < 7) return days + "일 전";

        return createdAt.format(DATE_FORMAT);
    }
}
