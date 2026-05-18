package com.moida.common.request;

import com.moida.domain.product.ProductType;
import com.moida.domain.product.ProductCondition;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ProductRequest {

    // ── 공통 필드 ──
    private String name;           // 상품명
    private String description;    // 상품 설명
    private String category;       // 카테고리 이름 (예: "패션/의류")
    private String condition;      // 상품 상태: S / A / B / C
    private String type;           // 거래 타입: AUCTION / TRADE
    private Long price;            // 경매: 시작가 / 중고거래: 판매가
    private String location;       // 거래 희망 지역
    private String image;          // 대표 이미지 Base64 문자열

    // ── 경매(AUCTION) 전용 필드 ──
    private Long buyNowPrice;      // 즉시낙찰가 (선택)
    private Long minBidUnit;       // 최소 호가 단위

    // ── 중고거래(TRADE) 전용 필드 ──
    private String tradeMethod;         // 직거래 / 택배 / 둘다
    private Boolean isPriceNegotiable;  // 가격 협의 여부

    // ── 편의 메서드 ──

    // "AUCTION" 문자열 → ProductType enum 변환
    public ProductType toProductType() {
        return ProductType.valueOf(this.type);
    }

    // "S" / "A" / "B" / "C" → ProductCondition enum 변환
    // 프론트에서 "S급", "A급" 형태로 오므로 "급" 제거 후 변환
    public ProductCondition toProductCondition() {
        String cleaned = this.condition.replace("급", "").trim(); // "S급" → "S"
        return ProductCondition.valueOf(cleaned);
    }


    public boolean isAuction() {
        return "AUCTION".equals(this.type);
    }

    public boolean isTrade() {
        return "TRADE".equals(this.type);
    }
}
