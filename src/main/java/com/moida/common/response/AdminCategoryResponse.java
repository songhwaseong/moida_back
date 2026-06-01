package com.moida.common.response;

import com.moida.domain.category.Category;

/**
 * 관리자 카테고리 관리 화면용 응답 DTO.
 * 공개 GET /api/categories 와 달리 활성/비활성 여부(visible)와 displayOrder 를 모두 노출한다.
 */
public record AdminCategoryResponse(
        Long id,
        String name,
        String emoji,
        int displayOrder,
        boolean visible
) {
    public static AdminCategoryResponse from(Category category) {
        return new AdminCategoryResponse(
                category.getId(),
                category.getName(),
                category.getEmoji(),
                category.getDisplayOrder() == null ? 0 : category.getDisplayOrder(),
                Boolean.TRUE.equals(category.getIsActive())
        );
    }
}
