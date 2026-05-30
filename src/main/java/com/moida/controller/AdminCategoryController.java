package com.moida.controller;

import com.moida.common.request.CategoryReorderRequest;
import com.moida.common.response.AdminCategoryResponse;
import com.moida.common.response.ApiResponse;
import com.moida.domain.category.AdminCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 관리자 카테고리 관리 API.
 * SecurityConfig /api/admin/** 규칙에 의해 ADMIN/MANAGER 만 접근 가능.
 */
@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final AdminCategoryService adminCategoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminCategoryResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(adminCategoryService.getAll()));
    }

    /** 노출 여부 토글 (body: { "visible": true | false }) */
    @PatchMapping("/{id}/visibility")
    public ResponseEntity<ApiResponse<AdminCategoryResponse>> setVisibility(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body) {
        boolean visible = Boolean.TRUE.equals(body.get("visible"));
        return ResponseEntity.ok(ApiResponse.success(adminCategoryService.setVisibility(id, visible)));
    }

    /** 순서 일괄 변경 (body: { "orders": [{ "id": 1, "displayOrder": 1 }, ...] }) */
    @PatchMapping("/reorder")
    public ResponseEntity<ApiResponse<List<AdminCategoryResponse>>> reorder(
            @RequestBody CategoryReorderRequest request) {
        return ResponseEntity.ok(ApiResponse.success(adminCategoryService.reorder(request),
                "카테고리 순서가 변경되었습니다."));
    }
}
