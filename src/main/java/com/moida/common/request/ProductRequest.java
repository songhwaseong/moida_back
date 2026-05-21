package com.moida.common.request;

import com.moida.domain.product.ProductCondition;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class ProductRequest {

    private String name;
    private String description;
    private String category;
    private String condition;
    private Long price;
    private String location;
    private String image;
    private List<String> images;
    private Integer mainImageIndex;
    private Long buyNowPrice;
    private Long minBidUnit;

    public ProductCondition toProductCondition() {
        if (this.condition == null || this.condition.isBlank()) {
            throw new IllegalArgumentException("Product condition is required.");
        }

        // Frontend labels can arrive as "S급"/"A급"; the enum stores only S/A/B/C.
        String code = this.condition.trim().toUpperCase().substring(0, 1);
        return ProductCondition.valueOf(code);
    }
}
