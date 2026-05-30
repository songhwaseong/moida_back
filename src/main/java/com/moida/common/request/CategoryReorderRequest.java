package com.moida.common.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 카테고리 순서 일괄 변경 요청 DTO.
 * orders 배열에 담긴 순서대로 (id → displayOrder) 를 갱신한다.
 */
@Getter
@NoArgsConstructor
public class CategoryReorderRequest {
    private List<Item> orders;

    @Getter
    @NoArgsConstructor
    public static class Item {
        private Long id;
        private Integer displayOrder;
    }
}
