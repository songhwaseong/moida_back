package com.moida.common.response;

import com.moida.domain.member.Member;
import com.moida.domain.product.Product;
import com.moida.domain.product.ProductImage;
import com.moida.domain.product.ProductType;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

/**
 * 관리자 상품 상세 모달용 응답 DTO.
 * 목록 응답(AdminProductResponse)과 동일한 정보에 더해, 등록된 모든 이미지(images)를 포함한다.
 * 이미지는 base64 라 무거우므로 목록이 아닌 상세 단건 조회에서만 내려준다.
 */
public record AdminProductDetailResponse(
        Long id,
        String productNo,
        String image,
        List<String> images,
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

    public static AdminProductDetailResponse from(Product product) {
        Member seller = product.getSeller();
        String sellerName = seller == null ? "-"
                : (seller.getNickname() != null && !seller.getNickname().isBlank()
                    ? seller.getNickname() : seller.getName());

        String type = product.getType() == ProductType.AUCTION ? "경매" : "중고거래";

        // displayOrder 오름차순으로 정렬해 등록 순서대로 보여준다.
        List<String> images = product.getImages().stream()
                .sorted(Comparator.comparing(img ->
                        img.getDisplayOrder() == null ? Integer.MAX_VALUE : img.getDisplayOrder()))
                .map(ProductImage::getUrl)
                .toList();

        return new AdminProductDetailResponse(
                product.getId(),
                product.getProductNo(),
                product.getMainImageUrl(),
                images,
                product.getName(),
                type,
                sellerName,
                product.getCategory().getName(),
                product.getCondition().getDescription(),
                product.getPrice(),
                AdminProductResponse.toKorean(product.getStatus()),
                product.getCreatedAt() == null ? "-" : product.getCreatedAt().format(DATE_FMT),
                product.getDescription()
        );
    }
}
