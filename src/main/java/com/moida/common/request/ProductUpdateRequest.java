package com.moida.common.request;

import com.moida.domain.product.ProductCondition;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class ProductUpdateRequest {

    private String name;
    private String description;
    private String category;
    private String condition;       // "S급"/"A급" 또는 "S"/"A"
    private Long price;             // 경매 시작가
    private Long minBidUnit;        // 최소 호가단위
    private Long immediatePrice;    // 즉시낙찰가 (선택, null 이면 미사용)
    private String location;
    private List<String> images;
    private Integer mainImageIndex;
    private String status;          // "SCHEDULED" | "HIDDEN" (선택, 미지정 시 유지)

    // 값이 비어 있으면 null 을 반환해 수정 단계에서 변경하지 않도록 한다.
    public ProductCondition toProductCondition() {
        if (this.condition == null || this.condition.isBlank()) {
            return null;
        }
        String code = this.condition.trim().toUpperCase().substring(0, 1);
        return ProductCondition.valueOf(code);
    }
}
