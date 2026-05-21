package com.moida.common.response;

import com.moida.domain.category.Category;

// 홈 화면 카테고리 칩에 표시할 정보. displayOrder는 정렬 순서를 프론트에서도 확인 가능하도록 함께 노출.
public record CategoryResponse(
        Long id,
        String name,
        String emoji,
        Integer displayOrder
) {
    // Category 엔티티를 프론트 카드 컴포넌트가 쓰는 평탄화된 응답으로 변환한다.
    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getEmoji(),
                category.getDisplayOrder()
        );
    }
}
