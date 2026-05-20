package com.moida.controller;

import com.moida.common.response.ApiResponse;
import com.moida.common.response.CategoryResponse;
import com.moida.domain.category.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Slf4j
public class CategoryController {

    private final CategoryRepository categoryRepository;

    // 홈 화면 카테고리 칩 영역에서 사용한다.
    // 활성(is_active=true) 카테고리만 displayOrder 오름차순으로 반환하며,
    // displayOrder 중복 시 id 오름차순을 2차 키로 적용해 매 호출 동일한 순서를 보장한다.
    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getCategories() {
        List<CategoryResponse> categories = categoryRepository
                .findAllByIsActiveTrueOrderByDisplayOrderAscIdAsc()
                .stream()
                .map(CategoryResponse::from)
                .toList();
        log.info("[CategoryController] GET /api/categories count={}", categories.size());
        return ResponseEntity.ok(ApiResponse.success(categories));
    }
}
