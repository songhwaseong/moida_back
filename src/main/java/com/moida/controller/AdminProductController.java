package com.moida.controller;

import com.moida.common.request.AdminProductUpdateRequest;
import com.moida.common.response.AdminProductDetailResponse;
import com.moida.common.response.AdminProductResponse;
import com.moida.common.response.AdminProductStatsResponse;
import com.moida.common.response.ApiResponse;
import com.moida.domain.product.AdminProductService;
import com.moida.domain.product.ProductStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 관리자 상품 관리 API.
 * SecurityConfig 에서 /api/admin/** 는 ADMIN / MANAGER 만 접근 가능하다.
 */
@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final AdminProductService adminProductService;

    /** 상품 목록 조회 */
    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminProductResponse>>> getProducts() {
        return ResponseEntity.ok(ApiResponse.success(adminProductService.getProducts()));
    }

    /** 상품 통계 조회 */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<AdminProductStatsResponse>> getStats() {
        return ResponseEntity.ok(ApiResponse.success(adminProductService.getStats()));
    }

    /** 상품 상세 조회 (이미지 전체 포함) */
    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<AdminProductDetailResponse>> getProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(ApiResponse.success(adminProductService.getProduct(productId)));
    }

    /** 상품 정보 수정 (상품명/설명/카테고리/제품상태/가격) */
    @PatchMapping("/{productId}")
    public ResponseEntity<ApiResponse<String>> updateProduct(
            @PathVariable Long productId,
            @RequestBody AdminProductUpdateRequest request) {
        adminProductService.updateProduct(productId, request);
        return ResponseEntity.ok(ApiResponse.success("상품 정보가 수정되었습니다."));
    }

    /** 상품 상태 변경 (body: { "status": "HIDDEN" } 형태의 enum 이름) */
    @PatchMapping("/{productId}/status")
    public ResponseEntity<ApiResponse<String>> updateStatus(
            @PathVariable Long productId,
            @RequestBody Map<String, String> body) {
        ProductStatus status = ProductStatus.valueOf(body.get("status"));
        adminProductService.updateStatus(productId, status);
        return ResponseEntity.ok(ApiResponse.success("상태가 변경되었습니다."));
    }

    /** 상품 삭제 (soft delete) */
    @DeleteMapping("/{productId}")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Long productId) {
        adminProductService.delete(productId);
        return ResponseEntity.ok(ApiResponse.success("상품이 삭제되었습니다."));
    }
}
