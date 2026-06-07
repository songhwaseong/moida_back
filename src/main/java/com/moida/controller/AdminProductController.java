package com.moida.controller;

import com.moida.common.exception.BusinessException;
import com.moida.common.exception.ErrorCode;
import com.moida.common.request.AdminReasonRequest;
import com.moida.common.request.AdminStatusUpdateRequest;
import com.moida.common.request.AdminProductUpdateRequest;
import com.moida.common.response.AdminProductDetailResponse;
import com.moida.common.response.AdminProductResponse;
import com.moida.common.response.AdminProductStatsResponse;
import com.moida.common.response.ApiResponse;
import com.moida.domain.audit.AdminActionLogService;
import com.moida.domain.product.AdminProductService;
import com.moida.domain.product.ProductStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
/**
 * 관리자 상품 관리 API.
 * SecurityConfig 에서 /api/admin/** 는 ADMIN / MANAGER 만 접근 가능하다.
 */
@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final AdminProductService adminProductService;
    private final AdminActionLogService adminActionLogService;

    /** 상품 목록 조회 */
    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminProductResponse>>> getProducts() {
        adminActionLogService.recordView(
                "ADMIN_PRODUCT_VIEW",
                "PRODUCT",
                adminActionLogService.fields("view", "list")
        );
        return ResponseEntity.ok(ApiResponse.success(adminProductService.getProducts()));
    }

    /** 상품 통계 조회 */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<AdminProductStatsResponse>> getStats() {
        adminActionLogService.recordView(
                "ADMIN_PRODUCT_STATS_VIEW",
                "PRODUCT",
                adminActionLogService.fields("view", "stats")
        );
        return ResponseEntity.ok(ApiResponse.success(adminProductService.getStats()));
    }

    /** 상품 상세 조회 (이미지 전체 포함) */
    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<AdminProductDetailResponse>> getProduct(@PathVariable Long productId) {
        adminActionLogService.recordView(
                "ADMIN_PRODUCT_DETAIL_VIEW",
                "PRODUCT",
                adminActionLogService.fields("productId", productId)
        );
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
            @RequestBody AdminStatusUpdateRequest request) {
        String reason = requireReason(request == null ? null : request.reason());
        try {
            ProductStatus status = ProductStatus.valueOf(request.status());
            adminProductService.updateStatus(productId, status, reason);
            return ResponseEntity.ok(ApiResponse.success("상태가 변경되었습니다."));
        } catch (RuntimeException e) {
            adminActionLogService.recordFailure(
                    "PRODUCT_STATUS_CHANGE_FAILED",
                    "PRODUCT",
                    productId,
                    String.valueOf(productId),
                    adminActionLogService.fields("requestedStatus", request == null ? null : request.status()),
                    e.getMessage()
            );
            throw e;
        }
    }

    /** 상품 삭제 (soft delete) */
    @DeleteMapping("/{productId}")
    public ResponseEntity<ApiResponse<String>> delete(
            @PathVariable Long productId,
            @RequestBody(required = false) AdminReasonRequest request) {
        String reason = requireReason(request == null ? null : request.reason());
        try {
            adminProductService.delete(productId, reason);
            return ResponseEntity.ok(ApiResponse.success("상품이 삭제되었습니다."));
        } catch (RuntimeException e) {
            adminActionLogService.recordFailure(
                    "PRODUCT_DELETE_FAILED",
                    "PRODUCT",
                    productId,
                    String.valueOf(productId),
                    adminActionLogService.fields("reason", reason),
                    e.getMessage()
            );
            throw e;
        }
    }

    private String requireReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "관리자 처리 사유를 입력해야 합니다.");
        }
        return reason.trim();
    }
}
