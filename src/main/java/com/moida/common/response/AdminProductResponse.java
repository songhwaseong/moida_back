package com.moida.common.response;

import com.moida.domain.member.Member;
import com.moida.domain.product.Product;
import com.moida.domain.product.ProductStatus;
import com.moida.domain.product.ProductType;

import java.time.format.DateTimeFormatter;

/**
 * 관리자 상품 관리 화면용 상품 응답 DTO.
 * Product 엔티티를 관리자 테이블이 바로 쓸 수 있는 형태(한글 상태/카테고리/타입)로 변환한다.
 */
public record AdminProductResponse(
        Long id,
        String productNo,
        String image,
        String name,
        String type,
        String seller,
        String category,
        String condition,
        long price,
        String status,
        String registeredAt,
        String description
) {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    /** ProductStatus(enum) → 화면 표시용 한글 */
    public static String toKorean(ProductStatus status) {
        return switch (status) {
            case SCHEDULED -> "경매예정";
            case PENDING -> "승인요청중";
            case LIVE -> "경매중";
            case SOLD -> "낙찰";
            case FAILED -> "유찰";
            case HIDDEN -> "숨김";
            case DELETED -> "삭제";
        };
    }

    public static AdminProductResponse from(Product product) {
        Member seller = product.getSeller();
        String sellerName = seller == null ? "-"
                : (seller.getNickname() != null && !seller.getNickname().isBlank()
                    ? seller.getNickname() : seller.getName());

        String type = product.getType() == ProductType.AUCTION ? "경매" : "중고거래";

        return new AdminProductResponse(
                product.getId(),
                product.getProductNo(),
                product.getMainImageUrl(),
                product.getName(),
                type,
                sellerName,
                product.getCategory().getName(),
                product.getCondition().getDescription(),
                product.getPrice(),
                toKorean(product.getStatus()),
                product.getCreatedAt() == null ? "-" : product.getCreatedAt().format(DATE_FMT),
                product.getDescription()
        );
    }
}
