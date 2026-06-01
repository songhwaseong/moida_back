package com.moida.common.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 관리자 상품 수정 요청 DTO.
 * 모달의 텍스트 필드(상품명/설명/카테고리/제품상태/가격) 수정에 사용한다.
 * null 인 필드는 변경하지 않는다(Product.update 의 null-skip 동작과 동일).
 *   - category  : 카테고리 이름
 *   - condition : ProductCondition enum 이름(S/A/B/C)
 */
@Getter
@NoArgsConstructor
public class AdminProductUpdateRequest {
    private String name;
    private String description;
    private String category;
    private String condition;
    private Long price;
}
